import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

import static java.lang.System.out;

public class ReliableBroadcast {


    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("helloworld");
        MsgReceiver msgr = new MsgReceiver(InetAddress.getLocalHost().toString(), 5000, InetAddress.getByName("255.255.255.255"));
        Thread threadserver = new Thread(msgr);
        threadserver.start();
        Thread.sleep(5000);
        MsgSender send = new MsgSender(InetAddress.getLocalHost().toString(), 5000, InetAddress.getByName("255.255.255.255"));
        send.sendJoin();
        System.out.println("sended");
    }
}