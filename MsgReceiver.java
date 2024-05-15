import java.io.*;
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
    private String state = "new";
    private int bufferSize = 4096; //lenght of message
    private HashMap<String, Boolean> endMap = null;
    private int retry = 0;
    private MsgLogger msgLogger = new MsgLogger();
    private MessageBuffer messageBuffer;
    private LogicalClock clock;

    public MsgReceiver(MsgSender msgSender, String ip, int port, MessageBuffer messageBuffer, LogicalClock clock) {
        this.msgSender = msgSender;
        this.ip = ip;
        this.port = port;
        this.messageBuffer = messageBuffer;
        this.clock = clock;
        System.out.println("done");
    }

    public void run() {
        System.out.println("start");
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
                    // TODO: FOR LOGGER DEBUG
                    // handleJoin(new ReliableMsg(Constants.MSG_ALIVE, "TEST ", System.currentTimeMillis(), "TEST", "TEST", 1, null));
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
                            clock.updateScalarclock(msg.getScalarclock());
                            switch(msg.getType()){
                                case(Constants.MSG_JOIN):
                                    handleJoin(msg);
                                    break;
                                case(Constants.MSG_END):
                                    handleEnd(msg);
                                    break;
                                case(Constants.MSG_ACK):
                                    handleACK(msg);
                                    break;
                                case(Constants.MSG):
                                    handleMsg(msg);
                                    break;
                                case(Constants.MSG_ALIVE):
                                    handleAlive(msg);
                                    break;
                                case(Constants.MSG_DROP):
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

    public void handleJoin(ReliableMsg msg) throws IOException {
        switch(this.state) {
            case("new"):
                //ignored
                msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_NEW);
                break;
            case("joining"):
                //ignored
                msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINING);
                break;
            case("joined"):
                this.setChange();
                this.msgSender.setLastJoin(msg.getFrom(), msg.getTimestamp());
                this.endMap = new HashMap<>();
                for(String a : msgSender.getMember()) {
                    this.endMap.put(a, false);
                }
                this.endMap.put(msg.getFrom(), false);
                if(messageBuffer.isMessageQueueEmpty()) {
                    this.msgSender.sendEnd();//need add acheker for arraylist
                }
                msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINED);
                break;
            case("change"):
                //ignored
                //gia ho fatto una decisione se ho fatto join, non ti posso piu accettare, se ho fatto remove, remova ha la precedenza su join
                msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_CHANGE);
                break;
        }
    }

    public void handleEnd(ReliableMsg msg) throws IOException {
        String s = msg.getBody();
        String[] t = s.split(";");
        HashSet<String> tmp = new HashSet<>(Arrays.asList(t));
        boolean done = true;
        switch(this.state) {
            case("new"):
                //ignored
                msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_NEW);
                break;
            case("joining"):
                msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINING);
                if(tmp.contains(this.ip)) {
                    // e un end per me
                    if(this.endMap == null) {// il primo end che ricevo
                        this.msgSender.setMemberMap(tmp);
                        this.msgSender.sendEnd();
                        this.endMap.replace(msg.getFrom(), false, true);
                    }
                    else {
                        if(tmp.equals(this.endMap.keySet())) {
                            this.endMap.replace(msg.getFrom(), false, true);
                            for(Map.Entry<String, Boolean> entry : this.endMap.entrySet()) {
                                if (!entry.getValue()) {
                                    done = false;
                                    break;
                                }
                            }
                            if(done) {
                                this.setJoined(msg.getView());
                            }
                        }
                        if(tmp.size() < this.endMap.size()) {
                            //succede un remove e non posso entrare
                            this.setNew();
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }// i am not allowed to join wait 1s and retry
                        }
                    }
                }
                break;
            case ("joined"):
                msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINED);
                this.setChange();
                if(tmp.size() > this.msgSender.getMember().size()) {
                    //this a join
                    tmp.removeAll(this.msgSender.getMember());
                    ArrayList<String> arrayTmp = new ArrayList<>(tmp);
                    this.msgSender.setLastJoin(arrayTmp.get(0), msg.getTimestamp());
                    this.endMap = new HashMap<>();
                    for (String a : tmp) {
                        this.endMap.put(a, false);
                    }
                    this.endMap.put(msgSender.getLastJoinIp(), false);
                    this.endMap.replace(msg.getFrom(), false, true);
                    if(messageBuffer.isMessageQueueEmpty()) {
                        this.msgSender.sendEnd();
                    }
                    break;
                }
                else {
                    //this a remove
                    String b = "";
                    for(String a : this.msgSender.getMember()) {
                        if(!tmp.contains(a)) {
                            b = a;
                            this.msgSender.setLastRemoveIp(b);
                            break;
                        }
                    }
                    this.endMap = new HashMap<>();
                    for (String a : tmp) {
                        this.endMap.put(a, false);
                    }
                    this.endMap.replace(msg.getFrom(), false, true);
                }
                if(messageBuffer.isMessageQueueEmpty()) {
                    this.msgSender.sendEnd();//need add acheker for arraylist
                }
                break;
            case ("change"):
                msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_CHANGE);
                if (tmp.size() == this.endMap.size()) {
                    if (tmp.equals(this.endMap.keySet())) {
                        this.endMap.replace(msg.getFrom(), false, true);
                    } else {
                        if (msg.getTimestamp() < msgSender.getLastJoinTimestamp()) {
                            //ho ricevuto un join piu presto percio devo cambiare la mia scelta
                            this.msgSender.setLastJoin(msg.getFrom(), msg.getTimestamp());
                            this.endMap = new HashMap<>();
                            for (String a : tmp) {
                                this.endMap.put(a, false);
                            }
                            this.endMap.replace(msg.getFrom(), false, true);
                            if(messageBuffer.isMessageQueueEmpty()) {
                                this.msgSender.sendEnd();//need add acheker for arraylist
                            }
                        }
                    }
                }
                if(tmp.size() > this.endMap.size()) {
                    //ho fatto un remove e ho ricevuto un join
                    //ignorato semplicemente
                }
                else {
                    //ho fatto un join e ho ricevuto un remove
                    //remove ha precedenza su join
                    this.endMap.keySet().removeAll(tmp); //back-end and vice versa so we just modify on hashset to change hashmap
                    this.endMap.keySet().remove(this.msgSender.getLastJoinIp());
                    ArrayList<String> arrayTmp = new ArrayList<>(this.endMap.keySet());
                    this.msgSender.setLastRemoveIp(arrayTmp.get(0));
                    this.endMap = new HashMap<>();
                    for (String a : tmp) {
                        this.endMap.put(a, false);
                    }
                    this.endMap.put(msgSender.getLastJoinIp(), false);
                    this.endMap.replace(msg.getFrom(), false, true);
                    if(messageBuffer.isMessageQueueEmpty()) {
                        this.msgSender.sendEnd();//need add acheker for arraylist
                    }
                }
                for(Map.Entry<String, Boolean> entry : this.endMap.entrySet()) {
                    if (!entry.getValue()) {
                        done = false;
                        break;
                    }
                }
                if(done) {
                    this.setJoined(msg.getView());
                }
                break;
        }
    }

    public void handleACK(ReliableMsg msg) throws IOException {
        if (msgSender.isMember(msg.getFrom())) {
            switch (this.state) {
                case ("new"):
                    //not gonna happen
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_NEW);
                    break;
                case ("joining"):
                    //not gonna happen
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINING);
                    break;
                case ("joined"):
                    //
                    messageBuffer.receiveACK(msg);
                    messageBuffer.delivery();
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINED);
                    break;
                case ("change"):
                    messageBuffer.receiveACK(msg);
                    messageBuffer.delivery();
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_CHANGE);
                    break;
            }
        }
    }// wait rentao

    public void handleMsg(ReliableMsg msg) throws IOException {
        if (msgSender.isMember(msg.getFrom()) && msg.getView() == this.msgSender.getView()) {
            switch (this.state) {
                case ("new"):
                    //ignored
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_NEW);
                    break;
                case ("joining"):
                    //ignored
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINING);
                    break;
                case ("joined"):
                    //add here
                    msgSender.sendACK(messageBuffer.getMessageId(msg));
                    messageBuffer.newMessage(msg);
                    messageBuffer.newMessageACK(msg);
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINED);
                    break;
                case ("change"):
                    //add here
                    msgSender.sendACK(messageBuffer.getMessageId(msg));
                    messageBuffer.newMessage(msg);
                    messageBuffer.newMessageACK(msg);
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_CHANGE);
                    break;
            }
        }//wait rentao
    }

    public void handleAlive(ReliableMsg msg) throws IOException {
        if (msgSender.isMember(msg.getFrom())) {
            switch (this.state) {
                case ("new"):
                    //ignored
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_NEW);
                    break;
                case ("joining"):
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINING);
                    if(this.endMap == null) {
                        retry++;
                    }
                    if(retry > 2) {
                        this.setJoined(msg.getView());; // i am alone so i am the member now
                    }
                    this.msgSender.update(msg.getFrom(), msg.getTimestamp());
                    break;
                case ("joined"):
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINED);
                    this.msgSender.update(msg.getFrom(), msg.getTimestamp());
                    break;
                case ("change"):
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_CHANGE);
                    this.msgSender.update(msg.getFrom(), msg.getTimestamp());
                    break;
            }
        }
    }

    public void handleDrop(ReliableMsg msg) throws IOException {
        if (msgSender.isMember(msg.getFrom())) {
            switch (this.state) {
                case ("new"):
                    //ignored
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_NEW);
                    break;
                case ("joining"):
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINING);
                    if(this.endMap != null) {
                        this.msgSender.setLastRemoveIpShadow(msg.getBody());
                        this.endMap.keySet().remove(msg.getBody());
                        boolean done = true;
                        for (Map.Entry<String, Boolean> entry : this.endMap.entrySet()) {
                            if (!entry.getValue()) {
                                done = false;
                                break;
                            }
                        }
                        if(done) {
                            this.setJoined(msg.getView());
                        }
                    }
                    break;
                case ("joined"):
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINED);
                    this.setChange();
                    this.msgSender.setLastRemoveIp(msg.getBody());
                    this.endMap = new HashMap<>();
                    for(String a : msgSender.getMember()) {
                        this.endMap.put(a, false);
                    }
                    this.endMap.remove(msg.getBody(), false);
                    this.msgSender.sendEnd();
                    break;
                case ("change"):
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_CHANGE);
                    this.msgSender.setLastRemoveIpShadow(msg.getBody());
                    this.endMap.keySet().remove(msg.getBody());
                    boolean done = true;
                    for (Map.Entry<String, Boolean> entry : this.endMap.entrySet()) {
                        if (!entry.getValue()) {
                            done = false;
                            break;
                        }
                    }
                    if(done) {
                        this.setJoined(msg.getView());
                    }
                    break;
            }
        }
    }

    public void setNew() {
        this.state = "new";
        this.endMap = null;
        this.msgSender.setFalse();
        this.retry = 0;
    }

    public void setJoining() {
        this.state = "joining";
        this.msgSender.setFalse();
        this.retry = 0;
        this.endMap = null;
    }

    public void setChange() {
        this.state = "change";
        this.msgSender.setFalse();
    }

    public void setJoined(int view) {
        this.state = "joined";
        this.endMap = null;
        this.msgSender.setTrue();
        this.msgSender.updateView(view);
    }

}