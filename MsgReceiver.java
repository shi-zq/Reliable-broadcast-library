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
    private String state = "new";
    private int bufferSize = 4096; //lenght of message
    private HashMap<String, Boolean> endMap = null;
    private int retry = 0;

    public MsgReceiver(MsgSender msgSender, String ip, int port, InetAddress broadcast) {
        this.msgSender = msgSender;
        this.ip = ip;
        this.port = port;
        this.broadcast = broadcast;
    }

    public void run() {
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
                } catch(IOException e) {
                    System.out.println("Selector error");
                    break;
                }
                Set<SelectionKey> readyKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = readyKeys.iterator();
                while(iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if(key.isReadable()) {
                        try {
                            DatagramChannel receiver = (DatagramChannel) key.channel();
                            ByteBuffer readData = ByteBuffer.allocate(bufferSize);
                            receiver.receive(readData);
                            ByteArrayInputStream bais = new ByteArrayInputStream(readData.array());
                            ObjectInputStream ois = new ObjectInputStream(bais);
                            ReliableMsg msg = (ReliableMsg) ois.readObject();
                            switch(msg.getType()){
                                case("JOIN"):
                                    handleJoin(msg);
                                    break;
                                case("END"):
                                    handleEnd(msg);
                                    break;
                                case("ACK"):
                                    //handleACK(msg);
                                    break;
                                case("MSG"):
                                    handleMsg(msg);
                                    break;
                                case("ALIVE"):
                                    handleAlive(msg);
                                    break;
                                case("DROP"):
                                    handleDrop(msg);
                                default:
                                    break;
                            }
                        } catch(IOException | ClassNotFoundException ignored) {
                            System.out.println("switch error");
                        }
                    }
                }
            }
        } catch(IOException ignored) {
            System.out.println("selector error");
        }
    }

    public void handleJoin(ReliableMsg msg) {
        switch(this.state) {
            case("new"):
                //ignored
                break;
            case("joining"):
                //ignored
                break;
            case("joined"):
                this.setChange();
                this.msgSender.setLastJoin(msg.getFrom(), msg.getTimestamp());
                this.endMap = new HashMap<>();
                for (String a : msgSender.getMember()) {
                    this.endMap.put(a, false);
                }
                this.msgSender.sendEnd();//need add acheker for arraylist
                break;
            case("change"):
                //ignored
                break;
        }
    }

    public void handleEnd(ReliableMsg msg) {
        String s = msg.getBody();
        String[] t = s.split(";");
        HashSet<String> tmp = new HashSet<>(Arrays.asList(t));
        boolean done = true;
        switch(this.state) {
            case("new"):
                //ignored
                break;
            case("joining"):
                if(tmp.contains(this.ip)) {
                    if(this.endMap == null) {
                        this.msgSender.setMemberMap(tmp);
                        this.msgSender.sendEnd();
                        this.endMap.replace(msg.getFrom(), false, true);
                    }
                    else {
                        if(tmp.equals(this.endMap.keySet())) {
                            this.endMap.replace(msg.getFrom(), false, true);
                        }
                        if(tmp.size() < this.endMap.size()) {
                            this.endMap.keySet().removeAll(tmp); //back-end and vice versa so we just modify on hashset to change hashmap
                            this.endMap.keySet().remove(this.msgSender.getLastJoinIp());
                            ArrayList<String> arrayTmp = new ArrayList<>(this.endMap.keySet());
                            this.msgSender.setLastRemoveIp(arrayTmp.getFirst());
                            this.endMap = new HashMap<>();
                            for (String a : tmp) {
                                this.endMap.put(a, false);
                            }
                            this.endMap.put(msgSender.getLastJoinIp(), false);
                            this.endMap.replace(msg.getFrom(), false, true);
                            msgSender.sendEnd();
                        }
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
                if(msg.getTimestamp() < msgSender.getLastJoinTimestamp()) {
                    this.setNew();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }// i am not allowed to join wait 1s and retry
                }
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
            case ("joined"):
                this.setChange();//need modity check is a remove or a join
                this.msgSender.setLastJoin(msg.getFrom(), msg.getTimestamp());
                this.endMap = new HashMap<>();
                for (String a : msgSender.getMember()) {
                    this.endMap.put(a, false);
                }
                this.endMap.replace(msg.getFrom(), false, true);
                msgSender.sendEnd();
                break;
            case ("change"):
                if (tmp.size() == this.endMap.size()) {
                    if (tmp.equals(this.endMap.keySet())) {
                        this.endMap.replace(msg.getFrom(), false, true);
                    } else {
                        if (msg.getTimestamp() < msgSender.getLastJoinTimestamp()) {
                            this.msgSender.setLastJoin(msg.getFrom(), msg.getTimestamp());
                            this.endMap = new HashMap<>();
                            for (String a : tmp) {
                                this.endMap.put(a, false);
                            }
                        }
                        this.endMap.replace(msg.getFrom(), false, true);
                        msgSender.sendEnd();
                    }
                }
                if(tmp.size() > this.endMap.size()) {
                    tmp.remove(this.msgSender.getLastRemoveIp());
                    tmp.removeAll(this.msgSender.getMember());
                    ArrayList<String> arrayTmp = new ArrayList<>(tmp);
                    this.msgSender.setLastJoin(arrayTmp.getFirst(), msg.getTimestamp());
                    this.endMap = new HashMap<>();
                    for(String a : tmp) {
                        this.endMap.put(a, false);
                    }
                    this.endMap.replace(msg.getFrom(), false, true);
                    this.msgSender.setLastJoin(msg.getFrom(), msg.getTimestamp());
                    msgSender.sendEnd();
                }
                else {
                    this.endMap.keySet().removeAll(tmp); //back-end and vice versa so we just modify on hashset to change hashmap
                    this.endMap.keySet().remove(this.msgSender.getLastJoinIp());
                    ArrayList<String> arrayTmp = new ArrayList<>(this.endMap.keySet());
                    this.msgSender.setLastRemoveIp(arrayTmp.getFirst());
                    this.endMap = new HashMap<>();
                    for (String a : tmp) {
                        this.endMap.put(a, false);
                    }
                    this.endMap.put(msgSender.getLastJoinIp(), false);
                    this.endMap.replace(msg.getFrom(), false, true);

                    msgSender.sendEnd();
                }
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

    public void handleACK(ReliableMsg msg) {
        if (msgSender.isMember(msg.getFrom())) {
            switch (this.state) {
                case ("new"):
                    //not gonna happen
                    break;
                case ("joining"):
                    //not gonna happen
                    break;
                case ("joined"):
                    //
                    break;
                case ("change"):
                    break;
            }
        }
    }// wait rentao

    public void handleMsg(ReliableMsg msg) {
        if (msgSender.isMember(msg.getFrom()) && msg.getView() == this.msgSender.getView()) {
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
        }//wait rentao
    }

    public void handleAlive(ReliableMsg msg) {
        if (msgSender.isMember(msg.getFrom())) {
            switch (this.state) {
                case ("new"):
                    //ignored
                    break;
                case ("joining"):
                    if(this.endMap == null) {
                        retry++;
                    }
                    if(retry > 2) {
                        this.setJoined();
                    }
                    this.msgSender.update(msg.getFrom(), msg.getTimestamp());
                    break;
                case ("joined"):
                    this.msgSender.update(msg.getFrom(), msg.getTimestamp());
                    break;
                case ("change"):
                    this.msgSender.update(msg.getFrom(), msg.getTimestamp());
                    break;
            }
        }
    }

    public void handleDrop(ReliableMsg msg) {
        if (msgSender.isMember(msg.getFrom())) {
            switch (this.state) {
                case ("new"):
                    //ignored
                    break;
                case ("joining"):
                    if(this.endMap != null) {

                    }
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
    public void setNew() {
        this.state = "new";
        this.endMap = null;
        this.retry = 0;
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
        this.endMap = null;
        this.msgSender.setTrue();
    }

}