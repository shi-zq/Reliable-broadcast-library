import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class MsgReceiver implements Runnable{
    private MsgSender msgSender;
    private String ip;
    private int port;
    private InetAddress broadcast;
    private String state;
    final int bufferSize = 4096; //lenght of message

    private Long lastTimestamp;
    private String lastIp;

    private HashMap<String, Boolean> memberAck;

    private SharedArraylist sharedlist;

    public MsgReceiver(MsgSender msgSender, String ip, int port, InetAddress broadcast, SharedArraylist sharedList) throws IOException {
        this.msgSender = msgSender;
        this.ip = ip;
        this.port = port;
        this.broadcast = broadcast;
        this.state = "new";
        lastTimestamp = 0L;
        lastIp = null;
        this.sharedlist = sharedList;
    }

    public void run(){
        try {
            Selector selector = Selector.open();
            DatagramChannel serverSocket = DatagramChannel.open();
            serverSocket.bind(new InetSocketAddress(port));
            serverSocket.configureBlocking(false);
            serverSocket.register(selector, SelectionKey.OP_READ);
            System.out.println("server ready");
            while(true) {
                if(this.state.equals("new")) {
                    lastTimestamp = msgSender.sendJoin();
                }
                try {
                    selector.select();
                }
                catch (IOException e) {
                    System.out.println("Selector error");
                    break;
                }
                Set<SelectionKey> readyKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = readyKeys.iterator();
                while(iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                        if (key.isReadable()) {
                            try {
                                DatagramChannel receiver = (DatagramChannel) key.channel();
                                ByteBuffer readData = ByteBuffer.allocate(bufferSize);
                                receiver.receive(readData);
                                ByteArrayInputStream bais = new ByteArrayInputStream(readData.array());
                                ObjectInputStream ois = new ObjectInputStream(bais);
                                ReliableMsg msg = (ReliableMsg) ois.readObject();
                                msg.print();
                                msgSender.update(msg.getFrom(), msg.getTimestamp());
                                switch (msg.getType()) {
                                    //加case+加handle
                                    case ("JOIN"):
                                        handleJoin(msg);
                                        break;
                                    case("WELCOME"):
                                        handleWelcome(msg);
                                        break;
                                    case ("END"):
                                        handleEnd(msg);
                                        break;
                                    case ("ALIVE"):
                                        handleAlive(msg);
                                        break;
                                    case ("CHANGE"):
                                        handleChange(msg);
                                    default:
                                        break;
                                }

                            } catch (IOException | ClassNotFoundException ignored) {
                                System.out.println("swtich error");

                            }
                        }
                }

            }

        }
        catch(IOException ignored) {
            System.out.println("selectr error");
        }
    }

    public void handleJoin(ReliableMsg msg) {
        if(!msg.getFrom().equals(ip)) {
            //only join from ip different, meaningless join msg from myself
            switch ("") {
                case("new"):
                    //ignored
                    break;
                case("joined"):
                    msgSender.sendWelcome(msg.getCreator(), msg.getTimestamp());
                    this.setAddMember();
                    this.ip = msg.getCreator();
                    this.lastTimestamp = msg.getTimestamp();
                    break;
                case("addmember"):
                    if(msg.getTimestamp() < this.lastTimestamp) { //there a earler join so who come first who join
                        msgSender.sendWelcome(msg.getCreator(), msg.getTimestamp());
                        this.ip = msg.getCreator();
                        this.lastTimestamp = msg.getTimestamp();
                    }
                    break;
                case("removemember"):
                    //ignored
                    break;
            }
        }
    }

    public void handleWelcome(ReliableMsg msg) {
        if(!msg.getFrom().equals(ip)) {
            switch ("") {
                case("new"):
                    //
                    break;
                case("joined"):
                    msgSender.sendWelcome(msg.getFrom(), msg.getTimestamp());
                    this.setAddMember();
                    this.lastIp = msg.getCreator();
                    this.lastTimestamp = msg.getTimestamp();
                    break;
                case("addmember"):
                    if(msg.getTimestamp() < this.lastTimestamp) {
                        msgSender.sendWelcome(msg.getFrom(), msg.getTimestamp());
                        this.ip = msg.getCreator();
                        this.lastTimestamp = msg.getTimestamp();
                    }
                    break;
                case("removemember"):
                    //ignored
                    break;
            }
        }
    public void handleEnd(ReliableMsg msg) {

    }

    public void handleAlive(ReliableMsg msg) {
    }

    public void handleChange(ReliableMsg msg) {

    }

    }
    public boolean checkIp(String ip) {
        return this.ip.equals(ip);
    }
    public void setJoined(){
        this.state = "joined";
        this.msgSender.setTrue();
    }

    public void setAddMember() {
        this.state = "addmember";
        this.msgSender.setFalse();
    }
}
