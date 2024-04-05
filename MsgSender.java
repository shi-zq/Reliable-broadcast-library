import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.HashMap;

public class MsgSender {
    private int view;
    private String ip;
    private int port;
    private boolean sending;
    private InetAddress broadcast;
    private DatagramSocket sendSocket;
    private HashMap<String, Long> memberMap;
    private LogicalClock clock;
    public MsgSender(String ip, int port, InetAddress broadcast, LogicalClock clock) throws IOException{
        this.view = 0;
        this.ip = ip;
        this.port = port;
        this.broadcast = broadcast;
        this.sendSocket = new DatagramSocket();
        sendSocket.setBroadcast(true);
        this.sending = false;
        this.memberMap = new HashMap<String, Long>();
        this.clock = clock;
    }
    public synchronized void sendJoin() {
        try {
            ReliableMsg join = new ReliableMsg("JOIN", this.ip, System.currentTimeMillis(), "");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(join);
            byte[] joinByte = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(joinByte, joinByte.length, broadcast, port);
            sendSocket.send(packet);
        }
        catch (IOException ignored) {
        }
    }

    public synchronized void sendAlive() {
        if(sending) {
            try {
                ReliableMsg alive = new ReliableMsg("ALIVE", this.ip, System.currentTimeMillis(), "");
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
    }

    public synchronized  void sendWelcome(String ip, Long time) {
        try {
            ReliableMsg join = new ReliableMsg("WELCOME", this.ip, System.currentTimeMillis(), ip + System.lineSeparator() + time);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(join);
            byte[] joinByte = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(joinByte, joinByte.length, broadcast, port);
            sendSocket.send(packet);
        }
        catch (IOException ignored) {
        }
    }
    public synchronized void sendMsg(String content) {
        if(sending) {
            try {
                clock.incrementScalarclock();
                ReliableMsg message = new ReliableMsg("MSG", this.ip, System.currentTimeMillis(), content, clock.getScalarclock());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(message);
                byte[] messageByte = baos.toByteArray();
                DatagramPacket packet = new DatagramPacket(messageByte, messageByte.length, broadcast, port);
                sendSocket.send(packet);
                sendACK(content);
            }
            catch (IOException ignored) {
                System.out.println("sendMsg");
            }
        }
    }

    public synchronized void sendACK(String acknowledgecontent) {
        if(sending) {
            try {
                clock.incrementScalarclock();
                ReliableMsg ack = new ReliableMsg("ACK", this.ip, System.currentTimeMillis(), acknowledgecontent, clock.getScalarclock());
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
    }

    public synchronized void sendLeave() {
        if(sending) {
            try {
                clock.incrementScalarclock();
                ReliableMsg leave = new ReliableMsg("LEAVE", this.ip, System.currentTimeMillis(), "I leaved.", clock.getScalarclock());
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
    }
    public synchronized boolean getState() {
        return sending;
    }

    public synchronized void setTrue() {
        this.sending = true;
    }

    public synchronized void setFalse() {
        this.sending = false;
    }

    public synchronized void update(String ip, Long time) {
        this.memberMap.replace(ip,time);
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

}
