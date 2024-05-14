import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

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
    private Long lastJoinIpAlive;
    private String lastRemoveIp;
    private boolean awareness;
    private String type;

    public MsgSender(String ip, int port, InetAddress broadcast, String type) throws IOException{
        this.view = 0;
        this.ip = ip;
        this.port = port;
        this.broadcast = broadcast;
        this.sendSocket = new DatagramSocket();
        this.sendSocket.setBroadcast(true);
        this.sending = false;
        this.memberMap = new HashMap<String, Long>();
        this.lastRemoveIp = null;
        this.lastJoinIp = null;
        this.lastJoinTimestamp = 0L;
        this.lastJoinIpAlive = 0L;
        this.type = type;
    }

    public synchronized void sendJoin() {
        this.lastJoinTimestamp = System.currentTimeMillis();
        this.lastJoinIp = ip;
        ReliableMsg join = new ReliableMsg(Constants.MSG_JOIN, ip, this.lastJoinTimestamp, "", type, view, "");
        try {
            sendMsgToSocket(join);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void sendAlive() {
        ReliableMsg alive = new ReliableMsg(Constants.MSG_ALIVE, ip, System.currentTimeMillis(), "", type, view, "");
        try {
            sendMsgToSocket(alive);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void sendEnd() {
        ReliableMsg end = new ReliableMsg(Constants.MSG_END, ip, System.currentTimeMillis(), "", type, view, createMemberList());
        try {
            sendMsgToSocket(end);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean sendMsg(String content) {
        if(this.sending) {
            ReliableMsg message = new ReliableMsg(Constants.MSG, ip, System.currentTimeMillis(), "index", type, view, content);
            try {
                sendMsgToSocket(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public synchronized void sendACK(String creator, String index, Long timestamp) {
        ReliableMsg ack = new ReliableMsg(Constants.MSG_ACK, this.ip, timestamp, "index", type, view, "");
        try {
            sendMsgToSocket(ack);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void sendDrop(String ip) {
        ReliableMsg drop = new ReliableMsg(Constants.MSG_DROP, this.ip, System.currentTimeMillis(), "index", type, view, ip);
        try {
            sendMsgToSocket(drop);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Extract the socket sending part for logging
     * */
    public synchronized void sendMsgToSocket(ReliableMsg msg) throws IOException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(msg);
            byte[] leaveByte = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(leaveByte, leaveByte.length, broadcast, port);
            sendSocket.send(packet);
        }
        catch (IOException ignored) {
            System.out.println("error " + ignored.toString());
        }
    }

    public synchronized void checkTimestamp() {
        Long now = System.currentTimeMillis();
        for(String key : memberMap.keySet()) {
            if(now - memberMap.get(key) < 5000*2) {
                if(!key.equals(this.lastRemoveIp)){
                    this.sendDrop(key);
                }
            }
        }
        if(now - lastJoinIpAlive < 5000*2) {
            if(this.lastJoinIp.equals(this.lastRemoveIp)){
                this.sendDrop(lastJoinIp);
            }
        }
    }

    public synchronized void update(String ip, Long time) {
        if(memberMap.containsKey(ip)) {
            this.memberMap.replace(ip, time);
        }
        if(ip.equals(lastJoinIp)) {
            this.lastJoinTimestamp = time;
        }
    }

    public synchronized boolean isMember(String ip) {
        return this.memberMap.containsKey(ip);
    }

    public synchronized String createMemberList() {
        StringBuilder tmp = new StringBuilder();
        for (Map.Entry<String, Long> entry : memberMap.entrySet()) {
            tmp.append(";").append(entry.getKey());
        }
        tmp.append(";").append(lastJoinIp);
        return tmp.toString();
    }

    public synchronized HashSet<String> getMember() {
        return (HashSet<String>) this.memberMap.keySet();
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
            memberMap.put(this.lastJoinIp, this.lastJoinIpAlive);
        }
        if(awareness) {
            memberMap.keySet().remove(this.lastRemoveIp);
        }
        this.lastRemoveIp = null;
        this.lastJoinIp = null;
        this.lastJoinTimestamp = 0L;
        this.lastJoinIpAlive = 0L;
    }

    public synchronized void setFalse() {
        this.sending = false;
        this.lastRemoveIp = null;
        this.lastJoinIp = null;
        this.lastJoinTimestamp = 0L;
        this.lastJoinIpAlive = 0L;
    }

    public synchronized void setLastJoin(String ip, Long time) {
        this.lastJoinIp = ip;
        this.lastJoinTimestamp = time;
        this.lastJoinIpAlive = time;
    }

    public synchronized void setLastRemoveIp(String lastRemoveIp) {
        this.lastRemoveIp = lastRemoveIp;
        awareness = true;
    }

    public synchronized void setLastRemoveIpShadow(String lastRemoveIp) {
        this.lastRemoveIp = lastRemoveIp;
        awareness = false;
    }

    public synchronized void setMemberMap(HashSet<String> member) {
        for(String tmp : member) {
            this.memberMap.put(tmp, System.currentTimeMillis());
        }
    }

    public synchronized void setType(String type) {
        this.type = type;
    }

    public synchronized void updateView(int view) {
        this.view = view+1;
    }
}
