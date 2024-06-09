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
    private Boolean debug = true;

    public ReliableBroadcast() throws IOException {
        this.messageBuffer = new MessageBuffer(debug);
        this.clock = new LogicalClock(InetAddress.getLocalHost().getHostAddress());
        this.msgSender = new MsgSender(InetAddress.getLocalHost().getHostAddress(), 6000, InetAddress.getByName("255.255.255.255"), clock, debug);
        AliveSender aliveSender = new AliveSender(msgSender, debug);
        MsgReceiver msgReceiver = new MsgReceiver(msgSender, 6000, messageBuffer, clock, debug);
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
        Scanner scanIn =new Scanner(System.in);
        String input = scanIn.nextLine();
        ReliableBroadcast r = new ReliableBroadcast();
        switch(input){
            case("1"):
                r.run();
                //1. join sequenziale uno dopo uno
                //2. join contemporanea di piu client
                //3. provare a terminare un client e controllare la correta cambiamento di view
                //4. provare a fare il join per vedere il cambiamento del view
                break;
            case ("2"):
                r.run();
                ReliableMsg msg1 = new ReliableMsg(Constants.MSG, InetAddress.getLocalHost().getHostAddress(), System.currentTimeMillis(), 2,"Sencond Message", 2,2 );
                Thread.sleep((5000));
                r.send(msg1);
                Thread.sleep((1000));
                ReliableMsg msg2 = new ReliableMsg(Constants.MSG, InetAddress.getLocalHost().getHostAddress(), System.currentTimeMillis(), 2,"First Message", 1,0 );
                r.send(msg2);
                break;
                //For testing Sender SequenceNumber reset to 3. And initialized to 3. And SendACK method SequenceNumber - 2.
                //Receiver does not change.



        }




    }
}