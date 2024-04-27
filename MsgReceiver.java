import java.io.*;
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

    private HashMap<String, Boolean> memberAck;

    private SharedArraylist sharedlist;

    public MsgReceiver(MsgSender msgSender, String ip, int port, InetAddress broadcast, SharedArraylist sharedList) throws IOException {
        this.msgSender = msgSender;
        this.ip = ip;
        this.port = port;
        this.broadcast = broadcast;
        this.state = "new";
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
                    msgSender.sendJoin();
                    this.setJoining();
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
                            msgSender.update(msg.getFrom(), msg.getTimestamp()); //ALIVE viene gestito daqui
                            switch (msg.getType()) {
                                //加case+加handle
                                case ("JOIN"):
                                    handleJoin(msg);
                                    break;
                                case ("END"):
                                    handleEnd(msg);
                                    break;
                                case ("ACK"):
                                    handleACK(msg);
                                    break;
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
        switch (this.state) {
            case("new"):
                //ignored, from my self or from other
                break;
            case("joining"):
                if(msgSender.checkLast(msg.getCreator(), msg.getTimestamp())) {
                    this.setNew();
                }
                break;
            case("joined"):
                this.setAddMember();
                msgSender.setLast(msg.getCreator(), msg.getTimestamp());
                break;
            case("addmember"):
                msgSender.checkLast(msg.getCreator(), msg.getTimestamp());
                break;
            case("removemember"):
                //ignored
                break;
        }
    }

    public void handleEnd(ReliableMsg msg) {
        if(msgSender.isMember(msg.getFrom())) {
            switch (this.state) {
                case("new"):
                    //not gonna happen
                    break;
                case("joining"):

                    break;
                case("joined"):
                    sharedlist.updateAck(msg);
                    break;
                case("addmember"):
                    sharedlist.updateAck(msg);
                    if(sharedlist.isEmpty()) {
                        msgSender.sendEnd();
                    }
                    break;
                case("removemember"):
                    sharedlist.updateAck(msg);
                    break;
            }
        }
    }

    public void handleACK(ReliableMsg msg) {
        if(msgSender.isMember(msg.getFrom())) {
            switch (this.state) {
                case("new"):
                    //not gonna happen
                    break;
                case("joining"):
                    //not gonna happen
                    break;
                case("joined"):
                    sharedlist.updateAck(msg);
                    break;
                case("addmember"):
                    sharedlist.updateAck(msg);
                    if(sharedlist.isEmpty()) {
                        msgSender.sendEnd();
                    }
                    break;
                case("removemember"):
                    sharedlist.updateAck(msg);
                    break;
            }
        }
    }


    public void setNew() {
        this.state = "new";
        this.msgSender.setFalse();
    }

    public void setJoining() {
        this.state = "joining";
        this.msgSender.setFalse();
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
//    public void handleWelcome(ReliableMsg msg) {
//        if(!msg.getFrom().equals(ip)) {
//            switch ("") {
//                case("new"):
//                    //
//                    break;
//                case("joined"):
//                    msgSender.sendWelcome(msg.getFrom(), msg.getTimestamp());
//                    this.setAddMember();
//                    this.lastIp = msg.getCreator();
//                    this.lastTimestamp = msg.getTimestamp();
//                    break;
//                case("addmember"):
//                    if(msg.getTimestamp() < this.lastTimestamp) {
//                        msgSender.sendWelcome(msg.getFrom(), msg.getTimestamp());
//                        this.ip = msg.getCreator();
//                        this.lastTimestamp = msg.getTimestamp();
//                    }
//                    break;
//                case("removemember"):
//                    //ignored
//                    break;
//            }
//        }
