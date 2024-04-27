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
    private IndexGenerator indexGenerator;

    public MsgSender(String ip, int port, InetAddress broadcast, LogicalClock clock, IndexGenerator indexGenerator) throws IOException{
        this.view = 0;
        this.ip = ip;
        this.port = port;
        this.broadcast = broadcast;
        this.sendSocket = new DatagramSocket();
        this.sendSocket.setBroadcast(true);
        this.sending = false;
        this.memberMap = new HashMap<String, Long>();
        this.indexGenerator = indexGenerator;
    }

    public synchronized Long sendJoin() {
        Long tmp = System.currentTimeMillis();
        try {
            ReliableMsg join = new ReliableMsg("JOIN", ip, ip, tmp, "", "");
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
        return tmp;
    }

    public synchronized void sendAlive() {
        try {
            ReliableMsg alive = new ReliableMsg("ALIVE", ip, ip, System.currentTimeMillis(), "", "");
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

    public synchronized void sendEnd(String newIp) {
        try {
            ReliableMsg end = new ReliableMsg("END", ip, ip, System.currentTimeMillis(), createMemberList(newIp), "");
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
                ReliableMsg message = new ReliableMsg("MSG", ip, ip, System.currentTimeMillis(), content, indexGenerator.getIndex());
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
            ReliableMsg ack = new ReliableMsg("ACK", creator, this.ip, timestamp, "", "");
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

    public synchronized void sendLeave() {
        try {
            ReliableMsg leave = new ReliableMsg("LEAVE", this.ip, this.ip, System.currentTimeMillis(), "", "");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(leave);
            byte[] leaveByte = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(leaveByte, leaveByte.length, broadcast, port);
            sendSocket.send(packet);
        }
        catch (IOException ignored) {
            System.out.println("sendLeave");
        }
    }

    public synchronized HashSet<String> getMember() {
        return (HashSet<String>) this.memberMap.keySet();
    }

    public synchronized void setTrue() {
        this.sending = true;
    }

    public synchronized void setFalse() {
        this.sending = false;
    }

    public synchronized void update(String ip, Long time) {
        if(memberMap.containsKey(ip)) {
            this.memberMap.replace(ip, time);
        }
    }

    public synchronized boolean isMember(String ip) {
        return this.memberMap.containsKey(ip);
    }
    public synchronized String checkTimestamp() {
        Long now = System.currentTimeMillis();
        Long range = now - 5000;
        for(String key : memberMap.keySet()) {
            if(memberMap.get(key) < range) {
                return key;
            }
        }
        return null;
    }

    public String createMemberList(String newIp) {
        StringBuilder tmp = new StringBuilder(indexGenerator.getType());
        for (Map.Entry<String, Long> entry : memberMap.entrySet()) {
            tmp.append(";").append(entry.getKey());
        }
        return tmp.toString();

    }
}
//    public synchronized  void sendWelcome(String creator, Long time) {
//        try {
//            ReliableMsg welcome = new ReliableMsg("WELCOME", creator, ip, time, "", "");
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            ObjectOutputStream oos = new ObjectOutputStream(baos);
//            oos.writeObject(welcome);
//            byte[] joinByte = baos.toByteArray();
//            DatagramPacket packet = new DatagramPacket(joinByte, joinByte.length, broadcast, port);
//            sendSocket.send(packet);
//        }
//        catch (IOException ignored) {
//            System.out.println("sendWelcome");
//        }
//    }
