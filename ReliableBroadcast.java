import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ReliableBroadcast {
    private AliveSender aliveSender;
    private MsgReceiver msgReceiver;
    private MsgSender msgSender;

    public ReliableBroadcast() throws IOException {
        this.msgSender = new MsgSender(InetAddress.getLocalHost().getHostAddress(), 5000, InetAddress.getByName("255.255.255.255"), "FIFO");
        this.aliveSender = new AliveSender(msgSender);
        this.msgReceiver = new MsgReceiver(msgSender, InetAddress.getLocalHost().getHostAddress(), 5000, InetAddress.getByName("255.255.255.255"));
    }

    public boolean sendMsg(String msg) {
        return this.msgSender.sendMsg(msg);
    }

    public ReliableMsg getMsg() {
        return new ReliableMsg(Constants.MSG, "ip", System.currentTimeMillis(), "index", "type", 0, "content");
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        FileWriter fileWriter = new FileWriter("log.txt");
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.print("Some String");
        printWriter.printf("Product name is %s and its price is %d $", "iPhone", 1000);
        printWriter.close();

        // Debug for MsgLogger
        MsgSender sender = new MsgSender("127.0.0.1", 80, InetAddress.getByName("127.0.0.1"), Constants.MSG_ALIVE);
        MsgReceiver msgReceiver = new MsgReceiver(sender, "127.0.0.0",80, InetAddress.getByName("127.0.0.0"));
        msgReceiver.run();

    }
}