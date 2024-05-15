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
        this.messageBuffer = new MessageBuffer();
        this.clock = new LogicalClock(InetAddress.getLocalHost().getHostAddress());
        this.msgSender = new MsgSender(InetAddress.getLocalHost().getHostAddress(), 5000, InetAddress.getByName("255.255.255.255"), clock);
        this.aliveSender = new AliveSender(msgSender);
        this.msgReceiver = new MsgReceiver(msgSender, InetAddress.getLocalHost().getHostAddress(), 5000, messageBuffer, clock);
        System.out.println("dfa");
        msgReceiver.run();
        aliveSender.run();
    }

    public boolean sendMsg(String msg) {
        return this.msgSender.sendMsg(msg);
    }

    public void getMsg() {
        messageBuffer.delivery();
    }

    public static void main(String[] args) {
        try {
            MessageBuffer messageBuffer = new MessageBuffer();
            LogicalClock clock = new LogicalClock(InetAddress.getLocalHost().getHostAddress());
            MsgSender msgSender = new MsgSender(InetAddress.getLocalHost().getHostAddress(), 80, InetAddress.getByName("255.255.255.255"), clock);
            AliveSender aliveSender = new AliveSender(msgSender);
            MsgReceiver msgReceiver = new MsgReceiver(msgSender, InetAddress.getLocalHost().getHostAddress(), 80, messageBuffer, clock);
            aliveSender.run();
            System.out.println("run msg receiver");
            msgReceiver.run();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }


    }
}