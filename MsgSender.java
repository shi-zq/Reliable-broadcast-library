import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class MsgSender {
    private int view;
    private String ip;
    private int port;
    private InetAddress broadcast;
    private DatagramSocket sendSocket;
    private ByteArrayOutputStream baos;
    private ObjectOutputStream oos;
    private String state;
    private HashMap<String, Long> viewMember;
    public MsgSender(String ip, int port, InetAddress broadcast) throws IOException{
        this.view = 0;
        this.ip = ip;
        this.port = port;
        this.broadcast = broadcast;
        this.sendSocket = new DatagramSocket();
        sendSocket.setBroadcast(true);
        this.baos = new ByteArrayOutputStream();
        this.oos = new ObjectOutputStream(baos);
        this.state = "new";
        this.viewMember = new HashMap<String, Long>();
    }

    public synchronized void sendJoin() {
        ReliableMsg join = new ReliableMsg("JOIN", this.ip, "");
        try {
            oos.writeObject(join);
            byte[] joinByte = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(joinByte, joinByte.length, broadcast, port);
            sendSocket.send(packet);
        }
        catch (IOException ignored) {
        }
    }
    public synchronized String getState() {
        return state;
    }

    public synchronized void setJoined() {
        this.state = "joined";
    }

    public synchronized void setChange() {
        this.state = "change";
    }

    public synchronized void update(String ip, Long time) {

    }
}
