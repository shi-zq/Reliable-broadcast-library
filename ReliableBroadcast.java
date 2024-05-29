import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Scanner;

public class ReliableBroadcast {
    private MsgSender msgSender;
    private MessageBuffer messageBuffer;
    private LogicalClock clock;
    private Thread senderThread;
    private Thread receiverThread;

    public ReliableBroadcast() throws IOException {
        this.messageBuffer = new MessageBuffer();
        this.clock = new LogicalClock(InetAddress.getLocalHost().getHostAddress());
        this.msgSender = new MsgSender(InetAddress.getLocalHost().getHostAddress(), 5000, InetAddress.getByName("255.255.255.255"), clock);
        AliveSender aliveSender = new AliveSender(msgSender, true);
        MsgReceiver msgReceiver = new MsgReceiver(msgSender, 5000, messageBuffer, clock);
        this.senderThread = new Thread(aliveSender);
        this.receiverThread = new Thread(msgReceiver);
    }

    public boolean sendMsg(String msg) {
        return this.msgSender.sendMsg(msg);
    }

    public void run() {
        this.senderThread.start();
        this.receiverThread.start();
    }

    public void send(ReliableMsg msg) throws IOException{
        this.msgSender.sendMsgToSocket(msg);
    }


    public static void main(String[] args) throws IOException, InterruptedException{

        ReliableBroadcast t = new ReliableBroadcast();
        System.out.println(t.getClass());
        Scanner scanIn =new Scanner(System.in);
        String input = scanIn.nextLine();
        ReliableBroadcast r = new ReliableBroadcast();
        switch(input){
            case("1"):
                r.run();
                Thread.sleep(10000);
                //r.terminate();
                //vedere se riesco fare il join
                break;
            case("2"):
                r.run();
                Thread.sleep(5000);
                //combinare 1 e 2 per vedere se riesco a fare un joni e leave
                break;
            case("3"):
                Thread.sleep((5000));
                r.run();
                Thread.sleep((5000));
                //combinare 1 e 3 per testare se qualcuno fa il leave
            case ("4"):
                r.run();
                ReliableMsg msg1 = new ReliableMsg(Constants.MSG, InetAddress.getLocalHost().getHostAddress(), System.currentTimeMillis(), 2,"Sencond Message", 2,2 );
                Thread.sleep((10000));
                r.send(msg1);
                Thread.sleep((1000));
                ReliableMsg msg2 = new ReliableMsg(Constants.MSG, InetAddress.getLocalHost().getHostAddress(), System.currentTimeMillis(), 2,"First Message", 1,0 );
                r.send(msg2);



        }




    }
}