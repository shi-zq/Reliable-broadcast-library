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
    private String state;
    private int bufferSize = 4096; //lenght of message
    private HashMap<String, Boolean> endMap;
    private int retry;
    private MsgLogger msgLogger = new MsgLogger();
    private MessageBuffer messageBuffer;
    private LogicalClock clock;
    private boolean debug;
    private Running running;

    public MsgReceiver(MsgSender msgSender, String ip, int port, MessageBuffer messageBuffer, LogicalClock clock, boolean debug, Running running) {
        this.msgSender = msgSender;
        this.ip = ip;
        this.port = port;
        this.messageBuffer = messageBuffer;
        this.clock = clock;
        this.debug = debug;
        this.running = running;
        this.setNew();
    }

    public void run() {
        try {
            Selector selector = Selector.open();
            DatagramChannel serverSocket = DatagramChannel.open();
            serverSocket.bind(new InetSocketAddress(port));
            serverSocket.configureBlocking(false);
            serverSocket.register(selector, SelectionKey.OP_READ);
            System.out.println("Receiver ready");
            while(true) {
                if(this.state.equals(Constants.STATE_NEW)) {
                    msgSender.sendJoin();
                    this.setJoining();
                    // TODO: FOR LOGGER DEBUG
                    // handleJoin(new ReliableMsg(Constants.MSG_ALIVE, "TEST ", System.currentTimeMillis(), "TEST", "TEST", 1, null));
                }
                try {
                    selector.select();
                } catch(IOException e) {
                    System.out.println("Selector error");
                    //non dovrebbe succedere
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
                            System.out.println(msg.getType());
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
                            //non dovrebbe succedere
                            break;
                        }
                    }
                }
            }
        } catch(IOException ignored) {
            System.out.println("selector error");
            //non dovrebbe succedere
        }
    }

    public void handleJoin(ReliableMsg msg) throws IOException {
        switch(this.state) {
            case(Constants.STATE_NEW):
                //ignored
                msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_NEW);
                break;
            case(Constants.STATE_JOINING):
                //ignored
                msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINING);
                break;
            case(Constants.STATE_JOINED):
                msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINED);
                this.setChange();
                this.msgSender.setLastJoin(msg.getFrom(), msg.getTimestamp());
                this.endMap.put(msg.getFrom(), false);// questo e il nuovo membro va aggiunto pure
                this.generateendMap();
                if(messageBuffer.isMessageQueueEmpty()) {
                    this.msgSender.sendEnd();
                }
                break;
            case(Constants.STATE_CHANGE):
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
        //set dei membri ricevuti
        switch(this.state) {
            case(Constants.STATE_NEW):
                //ignored
                msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_NEW);
                break;
            case(Constants.STATE_JOINING):
                msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINING);
                if(tmp.contains(ip)) {
                    // e un end per me
                    if(this.endMap == null) {// il primo end che ricevo
                        tmp.remove(this.ip);//devo rimuovermi dato che il mio ip e su lastjoinip
                        this.msgSender.setMemberMap(tmp);
                        this.generateendMap();
                        if(messageBuffer.isMessageQueueEmpty()) {
                            this.msgSender.sendEnd();//dovrebbe essere vuoto dato che appena creato
                        }
                        this.endMap.replace(msg.getFrom(), false, true);
                    }
                    else {
                        if(tmp.equals(this.endMap.keySet())) {
                            this.endMap.replace(msg.getFrom(), false, true);
                            if(this.checkendMap()) {
                                this.setJoined(msg.getView());
                            }
                        }
                        if(tmp.size() < this.endMap.size()) {
                            //succede un remove e non posso entrare riprovare dopo 1s
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
            case (Constants.STATE_JOINED):
                msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINED);
                this.setChange();
                if(tmp.size() > this.msgSender.getMember().size()) {
                    //this a join
                    tmp.removeAll(this.msgSender.getMember());
                    ArrayList<String> arrayTmp = new ArrayList<>(tmp);
                    this.msgSender.setLastJoin(arrayTmp.get(0), msg.getTimestamp());//cosi trovo quale e il nuovo ip che ha fatto join
                    this.generateendMap();
                    this.endMap.put(msgSender.getLastJoinIp(), false);
                    this.endMap.replace(msg.getFrom(), false, true);
                    if(messageBuffer.isMessageQueueEmpty()) {
                        this.msgSender.sendEnd();
                    }
                }
                else {
                    //this a remove
                    for(String a : this.msgSender.getMember()) {
                        if(!tmp.contains(a)) {
                            this.msgSender.setLastRemoveIp(a);
                            break;//trovo il ip da rimuovere termina la scansione
                        }
                    }
                    this.generateendMap();
                    this.endMap.replace(msg.getFrom(), false, true);
                }
                if(messageBuffer.isMessageQueueEmpty()) {
                    this.msgSender.sendEnd();//need add acheker for arraylist
                }
                break;
            case(Constants.STATE_CHANGE):
                msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_CHANGE);
                if(tmp.size() == this.endMap.size()) {
                    if (tmp.equals(this.endMap.keySet())) {
                        this.endMap.replace(msg.getFrom(), false, true);
                    } else {
                        if (msg.getTimestamp() < msgSender.getLastJoinTimestamp()) {//abbiamo lo stesso size, dato che solo un processo fallisce, sicuramente \`e un join
                            //ho ricevuto un join piu presto percio devo cambiare la mia scelta
                            this.endMap.keySet().removeAll(tmp);
                            ArrayList<String> arrayTmp = new ArrayList<>(this.endMap.keySet());
                            this.msgSender.setLastJoin(arrayTmp.get(0), msg.getTimestamp());//trovato il nuovo join
                            this.endMap.put(msgSender.getLastJoinIp(), false);
                            this.generateendMap();
                            this.endMap.replace(msg.getFrom(), false, true);
                            if (messageBuffer.isMessageQueueEmpty()) {
                                this.msgSender.sendEnd();//need add acheker for arraylist
                            }
                        }
                    }
                }
                else{
                    if (tmp.size() < this.endMap.size()) {
                        //ho fatto un join e ho ricevuto un remove
                        //remove ha precedenza su join
                        this.endMap.keySet().removeAll(tmp); //back-end and vice versa so we just modify on hashset to change hashmap
                        this.endMap.keySet().remove(this.msgSender.getLastJoinIp());
                        this.msgSender.clearLast();
                        ArrayList<String> arrayTmp = new ArrayList<>(this.endMap.keySet());
                        this.msgSender.setLastRemoveIp(arrayTmp.get(0));
                        this.generateendMap();
                        this.endMap.replace(msg.getFrom(), false, true);
                        if (messageBuffer.isMessageQueueEmpty()) {
                            this.msgSender.sendEnd();
                        }
                    }
                    if(this.checkendMap()) {
                        this.setJoined(msg.getView());
                    }
                }
                break;
        }
    }

    public void handleACK(ReliableMsg msg) throws IOException {
        if (msgSender.isMember(msg.getFrom())) {
            switch (this.state) {
                case (Constants.STATE_NEW):
                    //not gonna happen
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_NEW);
                    break;
                case (Constants.STATE_JOINING):
                    //not gonna happen
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINING);
                    break;
                case (Constants.STATE_JOINED):
                    //
                    messageBuffer.receiveACK(msg);
                    messageBuffer.delivery();
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINED);
                    break;
                case (Constants.STATE_CHANGE):
                    messageBuffer.receiveACK(msg);
                    messageBuffer.delivery();
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_CHANGE);
                    break;
            }
        }
    }

    public void handleMsg(ReliableMsg msg) throws IOException {
        if (msgSender.isMember(msg.getFrom()) && msg.getView() == this.msgSender.getView()) {
            switch (this.state) {
                case (Constants.STATE_NEW):
                    //ignored
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_NEW);
                    break;
                case (Constants.STATE_JOINING):
                    //ignored
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINING);
                    break;
                case (Constants.STATE_JOINED):
                    //add here
                    msgSender.sendACK(messageBuffer.getMessageId(msg));
                    messageBuffer.newMessage(msg);
                    messageBuffer.newMessageACK(msg);
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINED);
                    break;
                case (Constants.STATE_CHANGE):
                    //add here
                    msgSender.sendACK(messageBuffer.getMessageId(msg));
                    messageBuffer.newMessage(msg);
                    messageBuffer.newMessageACK(msg);
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_CHANGE);
                    break;
            }
        }
    }

    public void handleAlive(ReliableMsg msg) throws IOException {
        if (msgSender.isMember(msg.getFrom())) {
            switch (this.state) {
                case (Constants.STATE_NEW):
                    //ignored
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_NEW);
                    break;
                case (Constants.STATE_JOINING):
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINING);
                    if(this.endMap == null) {
                        retry++;
                    }
                    if(retry > 2) {
                        this.setJoined(msg.getView()); // i am alone so i am the member now
                    }
                    this.msgSender.update(msg.getFrom(), msg.getTimestamp());
                    break;
                case (Constants.STATE_JOINED):
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINED);
                    this.msgSender.update(msg.getFrom(), msg.getTimestamp());
                    break;
                case (Constants.STATE_CHANGE):
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_CHANGE);
                    this.msgSender.update(msg.getFrom(), msg.getTimestamp());
                    break;
            }
        }
    }

    public void handleDrop(ReliableMsg msg) throws IOException {
        if (msgSender.isMember(msg.getFrom())) {
            switch (this.state) {
                case (Constants.STATE_NEW):
                    //ignored
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_NEW);
                    break;
                case (Constants.STATE_JOINING):
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINING);
                    if(this.endMap != null) {
                        this.msgSender.setLastRemoveIpShadow(msg.getBody()); //purtroppo gia ho fatto un join
                        this.endMap.keySet().remove(msg.getBody());
                        if(this.checkendMap()) {
                            this.setJoined(msg.getView());
                        }
                    }
                    else {
                        this.msgSender.setLastRemoveIpShadow(msg.getBody());
                    }
                    break;
                case (Constants.STATE_JOINED):
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINED);
                    this.setChange();
                    this.msgSender.setLastRemoveIp(msg.getBody());
                    this.generateendMap();
                    this.endMap.remove(msg.getBody(), false);//rimuove il che non serve
                    this.msgSender.sendEnd();
                    break;
                case (Constants.STATE_CHANGE):
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_CHANGE);
                    this.msgSender.setLastRemoveIpShadow(msg.getBody());
                    this.endMap.keySet().remove(msg.getBody());
                    if(this.checkendMap()) {
                        this.setJoined(msg.getView());
                    }
                    break;
            }
        }
    }

    public void setNew() {
        this.state = Constants.STATE_NEW;
        this.endMap = null;
        this.msgSender.setFalse();
        this.retry = 0;
    }

    public void setJoining() {
        this.state = Constants.STATE_JOINING;
        this.msgSender.setFalse();
        this.retry = 0;
        this.endMap = null;
    }

    public void setChange() {
        this.state = Constants.STATE_CHANGE;
        this.msgSender.setFalse();
    }

    public void setJoined(int view) {
        this.state = Constants.STATE_JOINED;
        this.endMap = null;
        this.msgSender.setTrue();
        this.msgSender.updateView(view);
    }

    public void generateendMap() {
        this.endMap = new HashMap<>();
        for(String a : msgSender.getMember()) {
            this.endMap.put(a, false);
        }
        if(debug) {
            for(Map.Entry<String, Boolean> entry : this.endMap.entrySet()) {
                System.out.println(entry.getKey() + "=" + entry.getValue());
            }
        }
    }

    public boolean checkendMap() {
        for (Map.Entry<String, Boolean> entry : this.endMap.entrySet()) {
            System.out.println(entry.getKey() + "=" + entry.getValue());
            if (!entry.getValue()) {
                return false;
            }
        }
        return true;
    }

}