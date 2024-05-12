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
        try {
            ReliableMsg join = new ReliableMsg("JOIN", ip, this.lastJoinTimestamp, "", type, view, "");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(join);
            byte[] joinByte = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(joinByte, joinByte.length, broadcast, port);
            sendSocket.send(packet);
        }
        catch (IOException ignored) {
            System.out.println("sendJoin");
        }
    }

    public synchronized void sendAlive() {
        try {
            ReliableMsg alive = new ReliableMsg("ALIVE", ip, System.currentTimeMillis(), "", type, view, "");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(alive);
            byte[] aliveByte = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(aliveByte, aliveByte.length, broadcast, port);
            sendSocket.send(packet);
        }
        catch (IOException ignored) {
            System.out.println("sendAlive");
        }
    }

    public synchronized void sendEnd() {
        try {
            ReliableMsg end = new ReliableMsg("END", ip, System.currentTimeMillis(), "", type, view, createMemberList());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(end);
            byte[] messageByte = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(messageByte, messageByte.length, broadcast, port);
            sendSocket.send(packet);
        }
        catch (IOException ignored) {
            System.out.println("sendEnd");
        }
    }

    public synchronized boolean sendMsg(String content) {
        if(this.sending) {
            try {
                ReliableMsg message = new ReliableMsg("MSG", ip, System.currentTimeMillis(), "index", type, view, content);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(message);
                byte[] messageByte = baos.toByteArray();
                DatagramPacket packet = new DatagramPacket(messageByte, messageByte.length, broadcast, port);
                sendSocket.send(packet);
                return true;
            }
            catch (IOException ignored) {
                System.out.println("sendMsg");
            }
        }
        return false;
    }

    public synchronized void sendACK(String creator, String index, Long timestamp) {
        try {
            ReliableMsg ack = new ReliableMsg("ACK", this.ip, timestamp, "index", type, view, "");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(ack);
            byte[] ackByte = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(ackByte, ackByte.length, broadcast, port);
            sendSocket.send(packet);
        }
        catch (IOException ignored) {
            System.out.println("sendACK");
        }
    }

    public synchronized void sendDrop(String ip) {
        try {
            ReliableMsg drop = new ReliableMsg("DROP", this.ip, System.currentTimeMillis(), "index", type, view, ip);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(drop);
            byte[] leaveByte = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(leaveByte, leaveByte.length, broadcast, port);
            sendSocket.send(packet);
        }
        catch (IOException ignored) {
            System.out.println("sendLeave");
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

}
