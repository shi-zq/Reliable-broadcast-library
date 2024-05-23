import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ReliableBroadcast {
    private AliveSender aliveSender;
    private MsgReceiver msgReceiver;
    private MsgSender msgSender;
    private MessageBuffer messageBuffer;
    private LogicalClock clock;

    public ReliableBroadcast() throws IOException {
        Running running = new Running();
        this.messageBuffer = new MessageBuffer();
        this.clock = new LogicalClock(InetAddress.getLocalHost().getHostAddress());
        this.msgSender = new MsgSender(InetAddress.getLocalHost().getHostAddress(), 5000, InetAddress.getByName("255.255.255.255"), clock);
        this.aliveSender = new AliveSender(msgSender, true, running);
        this.msgReceiver = new MsgReceiver(msgSender, InetAddress.getLocalHost().getHostAddress(), 5000, messageBuffer, clock);
        Thread senderThread = new Thread(aliveSender);
        Thread receiverThread = new Thread(msgReceiver);
        senderThread.start();
        receiverThread.start();
    }

    public boolean sendMsg(String msg) {
        return this.msgSender.sendMsg(msg);
    }

    public void getMsg() {
        messageBuffer.delivery();
    }

    public static void main(String[] args) throws IOException {
        ReliableBroadcast t = new ReliableBroadcast();

    }
}