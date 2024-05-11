import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;

public class MsgReceiver implements Runnable {
    private MsgSender msgSender;
    private String ip;
    private int port;
    private InetAddress broadcast;
    private String state;
    final int bufferSize = 4096; //lenght of message

    private HashMap<String, Boolean> memberAck;

    private SharedArraylist sharedlist;
    private HashMap<String, Boolean> endMap;

    public MsgReceiver(MsgSender msgSender, String ip, int port, InetAddress broadcast, SharedArraylist sharedList) throws IOException {
        this.msgSender = msgSender;
        this.ip = ip;
        this.port = port;
        this.broadcast = broadcast;
        this.state = "new";
        this.sharedlist = sharedList;
    }

    public void run() {
        try {
            Selector selector = Selector.open();
            DatagramChannel serverSocket = DatagramChannel.open();
            serverSocket.bind(new InetSocketAddress(port));
            serverSocket.configureBlocking(false);
            serverSocket.register(selector, SelectionKey.OP_READ);
            System.out.println("server ready");
            while (true) {
                if (this.state.equals("new")) {
                    msgSender.sendJoin();
                    this.setJoining();
                }
                try {
                    selector.select();
                } catch (IOException e) {
                    System.out.println("Selector error");
                    break;
                }
                Set<SelectionKey> readyKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = readyKeys.iterator();
                while (iterator.hasNext()) {
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
                            switch (msg.getType()) {
                                case ("JOIN"):
                                    handleJoin(msg);
                                    break;
                                case ("END"):
                                    handleEnd(msg);
                                    break;
                                case ("ACK"):
                                    //handleACK(msg);
                                    break;
                                case ("MSG"):
                                    handleMsg(msg);
                                case ("ALIVE"):
                                    handleAlive(msg);
                                default:
                                    break;
                            }

                        } catch (IOException | ClassNotFoundException ignored) {
                            System.out.println("switch error");

                        }
                    }
                }
            }
        } catch (IOException ignored) {
            System.out.println("selector error");
        }
    }

    public void handleJoin(ReliableMsg msg) {
        switch (this.state) {
            case ("new"):
                //ignored
                break;
            case ("joining"):
                //ignored
                break;
            case ("joined"):
                this.setChange();
                this.msgSender.setLast(msg.getFrom(), msg.getTimestamp());
                this.endMap = new HashMap<>();
                for (String a : msgSender.getMember()) {
                    this.endMap.put(a, false);
                }
                //send end after check buffer!!!!
                break;
            case ("change"):
                //ignored
                break;
        }
    }

    public void handleEnd(ReliableMsg msg) {
        if (msgSender.isMember(msg.getFrom()) &&) {
            switch (this.state) {
                case ("new"):
                    //ignored
                    break;
                case ("joining"):
                    //ignored, maybe chekc if need to reset
                    break;
                case ("joined"):
                    this.setChange();
                    this.msgSender.setLast(msg.getFrom(), msg.getTimestamp());
                    this.endMap = new HashMap<>();
                    for (String tmp : msgSender.getMember()) {
                        this.endMap.put(tmp, false);
                    }
                    this.endMap.replace(msg.getFrom(), false, true);
                    break;
                case ("change"):
                    String s = msg.getBody();
                    String[] t = s.split(";");
                    HashSet<String> tmp = new HashSet<>(Arrays.asList(t));
                    if (tmp.size() == this.endMap.size()) {
                        if (tmp.equals(this.endMap.keySet())) {
                            this.endMap.replace(msg.getFrom(), false, true);
                        } else {
                            if (msg.getTimestamp() < msgSender.getLastJoinTimestamp()) {
                                this.msgSender.setLast(msg.getFrom(), msg.getTimestamp());
                                this.endMap = new HashMap<>();
                                for (String a : tmp) {
                                    this.endMap.put(a, false);
                                }
                            }
                            this.endMap.replace(msg.getFrom(), false, true);
                        }
                    }
                    if (tmp.size() > this.endMap.size()) {
                        tmp.remove(this.msgSender.getLastRemoveIp());
                        this.endMap = new HashMap<>();
                        for (String a : tmp) {
                            this.endMap.put(a, false);
                        }
                        this.endMap.replace(msg.getFrom(), false, true);
                    } else {
                        this.endMap = new HashMap<>();
                        for (String a : tmp) {
                            this.endMap.put(a, false);
                        }
                        this.endMap.put(msgSender.getLastJoinIp(), false);
                        this.endMap.replace(msg.getFrom(), false, true);
                    }
                    boolean done = true;
                    for(Map.Entry<String, Boolean> entry : this.endMap.entrySet()) {
                        if (!entry.getValue()) {
                            done = false;
                            break;
                        }
                    }
                    if(done) {
                        this.setJoined();
                    }
                    break;
            }
        }
    }

//    public void handleACK(ReliableMsg msg) {
//        if (msgSender.isMember(msg.getFrom())) {
//            switch (this.state) {
//                case ("new"):
//                    //not gonna happen
//                    break;
//                case ("joining"):
//                    //not gonna happen
//                    break;
//                case ("joined"):
//                    sharedlist.updateAck(msg);
//                    break;
//                case ("change"):
//                    break;
//            }
//        }
//    }// wait rentao

    public void handleMsg(ReliableMsg msg) {
        if (msgSender.isMember(msg.getFrom())) {
            switch (this.state) {
                case ("new"):
                    //ignored
                    break;
                case ("joining"):
                    //ignored
                    break;
                case ("joined"):
                    //add here
                    break;
                case ("change"):
                    //add here
                    break;
            }
        }
    }

    public void handleAlive(ReliableMsg msg) {

    }
    public void setNew() {
        this.state = "new";
        this.msgSender.setFalse();
    }

    public void setJoining() {
        this.state = "joining";
        this.msgSender.setFalse();
    }

    public void setChange() {
        this.state = "change";
        this.msgSender.setFalse();
    }

    public void setJoined() {
        this.state = "joined";
        this.msgSender.setTrue();
    }

}