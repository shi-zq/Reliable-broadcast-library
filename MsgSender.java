import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MsgSender {
    private int view; //current view
    private String ip; //our address
    private int port; //used port
    private boolean sending; //if we are allowed to send msg
    private InetAddress broadcast; // broadcastaddress
    private DatagramSocket sendSocket; //socket to send msg
    private HashMap<String, Long> memberMap; //current member and last live time
    private Long lastJoinTimestamp;
    private String lastJoinIp;
    private String lastRemoveIp;
    private boolean awareness;
    private boolean debug;

    private HashSet<String> tempMember;
    private int messageSequenceNumber;  // Used for FIFO
    private LogicalClock clock;
    //Sender and Receiver share a single logical clock.

    public MsgSender(String ip, int port, InetAddress broadcast, LogicalClock clock, boolean debug) throws SocketException {
        this.view = 0;
        this.ip = ip;
        this.port = port;
        this.broadcast = broadcast;
        this.sendSocket = new DatagramSocket();
        this.sendSocket.setBroadcast(true);
        this.sending = false;
        this.memberMap = new HashMap<>();
        this.lastRemoveIp = null;
        this.lastJoinIp = null;
        this.lastJoinTimestamp = 0L;
        this.debug = debug;

        this.tempMember = new HashSet<>();
        this.messageSequenceNumber = 0;
        //this.messageSequenceNumber = 3;
        this.clock = clock;
    }

    public synchronized void sendJoin() {
        //sono il nuovo che voglio fare il join
        this.lastJoinTimestamp = System.currentTimeMillis();
        this.lastJoinIp = ip;
        ReliableMsg join = new ReliableMsg(Constants.MSG_JOIN, ip, this.lastJoinTimestamp, view, "", -1, -1);
        try {
            sendMsgToSocket(join);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void sendAlive() {
        ReliableMsg alive = new ReliableMsg(Constants.MSG_ALIVE, ip, System.currentTimeMillis(), view, "", -1, -1);
        try {
            sendMsgToSocket(alive);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void sendEnd() {
        ReliableMsg end;
        if(this.lastJoinIp != null) {
            end = new ReliableMsg(Constants.MSG_END, this.ip, this.getLastJoinTimestamp(), view, createMemberList(), -1, -1);

        }
        else {
            end = new ReliableMsg(Constants.MSG_END, ip, System.currentTimeMillis(), view, createMemberList(), -1, -1);
        }
        try {
            sendMsgToSocket(end);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean sendMsg(String content) {
        if(this.sending) {
            ReliableMsg message = new ReliableMsg(Constants.MSG, this.ip, System.currentTimeMillis(), this.view, content, this.clock.getScalarclock(), this.messageSequenceNumber);
            try {
                sendMsgToSocket(message);
                messageSequenceNumber = messageSequenceNumber + 1;
                clock.updateScalarclock(message.getScalarclock());
            }
            catch(IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    public synchronized void sendACK(ReliableMsg message)  {
        ReliableMsg ack = new ReliableMsg(Constants.MSG_ACK, this.ip, System.currentTimeMillis(), this.view, getMessageId(message), clock.getScalarclock(), this.messageSequenceNumber);
        try {
            sendMsgToSocket(ack);
            messageSequenceNumber = messageSequenceNumber + 1;
            //messageSequenceNumber = messageSequenceNumber - 2;
            clock.updateScalarclock(message.getScalarclock());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void sendDrop(String dropIp) {
        ReliableMsg drop = new ReliableMsg(Constants.MSG_DROP, this.ip, System.currentTimeMillis(), this.view, dropIp, -1, -1);
        try {
            sendMsgToSocket(drop);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Extract the socket sending part for logging
     * */
    public synchronized void sendMsgToSocket(ReliableMsg msg) throws IOException{
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(msg);
            byte[] leaveByte = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(leaveByte, leaveByte.length, broadcast, port);
            sendSocket.send(packet);
        }
        catch(IOException ignored) {
            System.out.println("error " + ignored.toString());
        }
    }

    public synchronized void checkTimestamp() {
        Long now = System.currentTimeMillis();
        for(Map.Entry<String, Long> entry : memberMap.entrySet()) {
            if((now - entry.getValue()) > 3000*3) {
                if(this.lastRemoveIp == null) {
                    this.sendDrop(entry.getKey());
                    System.out.println("ipDROP=" + entry.getKey() + " alive=" + entry.getValue());
                }
            }
        }
        if((now - this.lastJoinTimestamp) > 3000*3 && this.lastJoinTimestamp != 0L) {
            if(this.lastRemoveIp == null) {
                this.sendDrop(this.lastJoinIp);

            }
        }
    }

    public synchronized void update(String ip, Long time) {
        this.memberMap.replace(ip, time);
        if(ip.equals(this.lastJoinIp))  {
            this.lastJoinTimestamp = time;
        }
    }


    public synchronized Set<String> getMember() {
        return this.memberMap.keySet();
    }

    public synchronized Long getLastJoinTimestamp() {
        return this.lastJoinTimestamp;
    }

    public synchronized String getLastJoinIp() {
        return this.lastJoinIp;
    }

    public synchronized String getLastRemoveIp() {
        return this.lastRemoveIp;
    }

    public synchronized int getView() {
        return this.view;
    }

    public synchronized void setTrue() {
        this.sending = true;
        if(lastJoinIp != null) {
            memberMap.put(this.lastJoinIp, System.currentTimeMillis());
            tempMember.add(this.lastJoinIp);
        }
        if(awareness && this.lastRemoveIp !=null) {
            memberMap.keySet().remove(this.lastRemoveIp);
            tempMember.remove(this.lastRemoveIp);
        }
        this.lastRemoveIp = null;
        this.lastJoinIp = null;
        this.lastJoinTimestamp = 0L;
        this.clock.reset();
        //this.messageSequenceNumber = 3;
        this.messageSequenceNumber = 0;
        this.view = view+1;
    }

    public synchronized void setFalse() {
        this.sending = false;
    }

    public synchronized void setLastJoin(String ip, Long time) {
        this.lastJoinIp = ip;
        this.lastJoinTimestamp = time;
    }

    public synchronized void setLastRemoveIp(String lastRemoveIp) {
        this.lastRemoveIp = lastRemoveIp;
        awareness = true;
        this.lastJoinIp = null;
        this.lastJoinTimestamp = 0L;

        this.tempMember.remove(lastRemoveIp);
    }

    public synchronized void setLastRemoveIpShadow(String lastRemoveIp) {
        this.lastRemoveIp = lastRemoveIp;
        awareness = false;

        this.tempMember.remove(lastRemoveIp);
    }

    public synchronized void setMemberMap(HashSet<String> member) {
        for(String tmp : member) {
            this.memberMap.put(tmp, System.currentTimeMillis());
        }
    }

    public synchronized void setView(int view) {
        this.view = view;
    }

    public synchronized String getMessageId(ReliableMsg message) {
        return message.getFrom() + ":" + message.getScalarclock();
    }

    public synchronized HashSet<String> getTempMember() {
        return this.tempMember;
    }

    public synchronized String createMemberList() {
        StringBuilder tmp = new StringBuilder();
        for(Map.Entry<String, Long> entry : memberMap.entrySet()) {
            if(this.lastRemoveIp != null && this.lastRemoveIp.equals(entry.getKey()) && awareness) {
                //non fare niente? saltarlo
            }
            else {
                tmp.append(entry.getKey()).append(";");
            }
        }
        if(this.lastJoinIp != null) {
            tmp.append(lastJoinIp).append(";");
        }
        return tmp.toString();
    }
}
