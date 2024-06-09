import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
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
    private HashMap<String, Boolean> endMap;
    private int retry;
    private boolean debug;

    private MsgLogger msgLogger = new MsgLogger();

    private MessageBuffer messageBuffer;
    private LogicalClock clock;

    public MsgReceiver(MsgSender msgSender, int port, MessageBuffer messageBuffer, LogicalClock clock, Boolean debug) throws UnknownHostException {
        this.msgSender = msgSender;
        this.ip = InetAddress.getLocalHost().getHostAddress();
        this.port = port;
        this.messageBuffer = messageBuffer;
        this.clock = clock;
        this.debug = debug;
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
                try {
                    selector.select();
                }
                catch(IOException e) {
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
                            ByteBuffer readData = ByteBuffer.allocate(4096);
                            receiver.receive(readData);
                            ByteArrayInputStream bais = new ByteArrayInputStream(readData.array());
                            ObjectInputStream ois = new ObjectInputStream(bais);
                            ReliableMsg msg = (ReliableMsg) ois.readObject();
                            switch(msg.getType()) {
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
                                    break;
                                default:
                                    break;
                            }
                        }
                        catch(IOException | ClassNotFoundException ignored) {
                            System.out.println("switch error");
                            break;
                        }
                    }
                }
            }
        }
        catch(IOException ignored) {
            System.out.println("selector error");
        }
    }

    public void handleJoin(ReliableMsg msg) throws IOException {
        switch(this.state) {
            case(Constants.STATE_NEW):
                msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_NEW);
                //messaggi ignorato semplicemente
                break;
            case(Constants.STATE_JOINING):
                msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINING);
                //messaggi ignorato semplicemente
                break;
            case(Constants.STATE_JOINED):
                msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINED);
                this.setChange();
                this.msgSender.setLastJoin(msg.getFrom(), msg.getTimestamp());
                this.generateendMap();
                this.endMap.put(this.msgSender.getLastJoinIp(), false);// questo e il nuovo membro va aggiunto pure
                if(debug) {
                    System.out.println("endMap state");
                    for(Map.Entry<String, Boolean> entry : this.endMap.entrySet()) {
                        System.out.println(entry.getKey() + "=" + entry.getValue());
                    }
                }
                if(messageBuffer.isMessageQueueEmpty()) {//controllo se sono pronto per inviare end
                    this.msgSender.sendEnd();
                }
                break;
            case(Constants.STATE_CHANGE):
                //ogni client puo fare solo una decisione(drop ha precedenza su join), percio lo ignoro
                msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_CHANGE);
                break;
        }
    }

    public void handleEnd(ReliableMsg msg) throws IOException {
        String s = msg.getBody();
        String[] t = s.split(";");
        HashSet<String> tmp = new HashSet<>(Arrays.asList(t));
        //crea un hashset dei membri del nuovo view
        switch(this.state) {
            case(Constants.STATE_NEW):
                //messaggi ignorato semplicemente
                msgLogger.printLog(msg, Constants.MSG_SUCC, null, Constants.STATE_NEW);
                break;
            case(Constants.STATE_JOINING):
                msgLogger.printLog(msg, Constants.MSG_SUCC, null, Constants.STATE_JOINING);
                if(tmp.contains(ip)) {
                    if(tmp.size() == 1) {
                        this.generateendMap();
                        this.endMap.put(msgSender.getLastJoinIp(), false);
                        this.endMap.replace(msg.getFrom(), true);
                        //sono l'unico membro del network
                    }
                    else {
                        if(this.endMap == null) {
                            tmp.remove(this.ip);//devo rimuovermi dato che il mio ip e su lastjoinip
                            this.msgSender.setMemberMap(tmp);//aggiorno i membri attuale
                            this.msgSender.setView(msg.getView());//aggiorno il view
                            this.generateendMap();
                            this.endMap.put(this.msgSender.getLastJoinIp(), false);// metto il mio ip
                            this.endMap.replace(msg.getFrom(), true);
                            if(this.msgSender.getLastRemoveIp() != null) {
                                this.endMap.replace(this.msgSender.getLastRemoveIp(), true);//controllo se ho shadowremove
                            }
                            this.msgSender.sendEnd();// sono nello stato joining, sicuramente non ho messaggi
                        }
                        else {
                            this.endMap.replace(msg.getFrom(), true);
                        }
                    }
                    if(this.checkendMap()) {
                        this.setJoined();
                    }
                }
                else {
                    if(tmp.size() == 1 && this.msgSender.getLastJoinTimestamp() > msg.getTimestamp()) {
                        this.setNew(); //qualcuno sta creando il gruppo prima di me, riparto
                    }
                }
                break;
            case(Constants.STATE_JOINED):
                msgLogger.printLog(msg, Constants.MSG_SUCC, null, Constants.STATE_JOINED);
                this.setChange();
                if(tmp.size() > this.msgSender.getMember().size()) {
                    //join
                    tmp.removeAll(this.msgSender.getMember());
                    ArrayList<String> arrayTmp = new ArrayList<>(tmp);
                    this.msgSender.setLastJoin(arrayTmp.get(0), msg.getTimestamp());//cosi trovo quale e il nuovo ip che ha fatto join
                    this.generateendMap();
                    this.endMap.put(msgSender.getLastJoinIp(), false);
                    this.endMap.replace(msg.getFrom(), true);
                    if(debug) {
                        System.out.println("endMap state");
                        for(Map.Entry<String, Boolean> entry : this.endMap.entrySet()) {
                            System.out.println(entry.getKey() + "=" + entry.getValue());
                        }
                    }
                }
                else {
                    //remove
                    for(String a : this.msgSender.getMember()) {
                        if(!tmp.contains(a)) {
                            this.msgSender.setLastRemoveIp(a);//trovo il membro da rimuovere
                        }
                    }
                    this.generateendMap();
                    this.endMap.remove(this.msgSender.getLastRemoveIp());//non ricevero mai il ACK da questo
                    this.endMap.replace(msg.getFrom(), true);
                    if(debug) {
                        System.out.println("endMap state");
                        for(Map.Entry<String, Boolean> entry : this.endMap.entrySet()) {
                            System.out.println(entry.getKey() + "=" + entry.getValue());
                        }
                    }
                }
                if(messageBuffer.isMessageQueueEmpty()) {
                    this.msgSender.sendEnd();
                }
                break;
            case(Constants.STATE_CHANGE):
                msgLogger.printLog(msg, Constants.MSG_SUCC, null, Constants.STATE_CHANGE);
                if(tmp.size() == this.endMap.size()) {
                    if(tmp.equals(this.endMap.keySet())) {
                        this.endMap.replace(msg.getFrom(), true);
                    }
                    else {
                        if(msg.getTimestamp() < msgSender.getLastJoinTimestamp()) {//abbiamo lo stesso size, dato che solo un processo fallisce, sicuramente 'e un join
                            //ho ricevuto un join piu presto percio devo cambiare la mia scelta
                            tmp.removeAll(this.endMap.keySet()); //rimuove i duplicati, rimane solo il nuovo membro
                            ArrayList<String> arrayTmp = new ArrayList<>(tmp);
                            this.msgSender.setLastJoin(arrayTmp.get(0), msg.getTimestamp());//trovato il nuovo join
                            this.generateendMap();
                            this.endMap.put(this.msgSender.getLastJoinIp(), false);
                            this.endMap.replace(msg.getFrom(), true);
                            if(debug) {
                                System.out.println("endMap state");
                                for(Map.Entry<String, Boolean> entry : this.endMap.entrySet()) {
                                    System.out.println(entry.getKey() + "=" + entry.getValue());
                                }
                            }
                            if(messageBuffer.isMessageQueueEmpty()) {
                                this.msgSender.sendEnd();
                            }
                        }
                    }
                }
                else {
                    //ho fatto un join e ho ricevuto un drop
                    //drop ha precedenza su join
                    this.endMap.keySet().removeAll(tmp);
                    this.endMap.keySet().remove(this.msgSender.getLastJoinIp());
                    ArrayList<String> arrayTmp = new ArrayList<>(this.endMap.keySet());//
                    this.msgSender.setLastRemoveIp(arrayTmp.get(0));
                    this.generateendMap();
                    this.endMap.remove(this.msgSender.getLastRemoveIp()); //sicuramente non 'e shadow remove
                    this.endMap.replace(msg.getFrom(), true);
                    if(debug) {
                        System.out.println("endMap state");
                        for(Map.Entry<String, Boolean> entry : this.endMap.entrySet()) {
                            System.out.println(entry.getKey() + "=" + entry.getValue());
                        }
                    }
                    if(messageBuffer.isMessageQueueEmpty()) {
                        this.msgSender.sendEnd();
                    }
                }
                if(this.checkendMap()) {
                    this.setJoined();
                }
                break;
        }
    }

    public void handleACK(ReliableMsg msg) throws IOException {
        if (msg.getView() == this.msgSender.getView()) {
            switch (this.state) {
                case (Constants.STATE_NEW):
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_NEW);
                    break;
                case (Constants.STATE_JOINING):
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINING);
                    break;
                case (Constants.STATE_JOINED):
                    clock.updateScalarclock(msg.getScalarclock());
                    messageBuffer.newMessage(msg, this.msgSender.getTempMember());
                    //messageBuffer.receiveACK(msg);
                    //messageBuffer.delivery(msgSender.getMember());
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINED);
                    break;
                case (Constants.STATE_CHANGE):
                    clock.updateScalarclock(msg.getScalarclock());
                    messageBuffer.newMessage(msg,this.msgSender.getTempMember());
                    //messageBuffer.receiveACK(msg);
                    //messageBuffer.delivery(msgSender.getMember());
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_CHANGE);
                    if(messageBuffer.isMessageQueueEmpty()) {
                        this.msgSender.sendEnd();
                    }
                    break;
            }
        }
    }

    public void handleMsg(ReliableMsg msg) throws IOException {
        if (msg.getView() == this.msgSender.getView()) {
            switch (this.state) {
                case (Constants.STATE_NEW):
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_NEW);
                    break;
                case (Constants.STATE_JOINING):
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINING);
                    break;
                case (Constants.STATE_JOINED):
                    clock.updateScalarclock(msg.getScalarclock());
                    msgSender.sendACK(msg);
                    messageBuffer.newMessage(msg, this.msgSender.getTempMember());
                    messageBuffer.newMessageACK(msg);
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINED);
                    break;
                case (Constants.STATE_CHANGE):
                    clock.updateScalarclock(msg.getScalarclock());
                    msgSender.sendACK(msg);
                    messageBuffer.newMessage(msg, this.msgSender.getTempMember());
                    messageBuffer.newMessageACK(msg);
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_CHANGE);
                    break;
            }
        }
    }

    public void handleAlive(ReliableMsg msg) throws IOException {
            switch(this.state) {
                case(Constants.STATE_NEW):
                    if(msg.getFrom().equals(this.ip)) {
                        msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_NEW);
                        this.msgSender.sendJoin();
                        this.setJoining();
                        this.msgSender.update(msg.getFrom(), msg.getTimestamp());
                    }
                    break;
                case(Constants.STATE_JOINING):
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINING);
                    if(this.endMap == null) {
                        this.retry++;
                        if(this.retry > 2) {
                            this.msgSender.sendEnd();
                            //dopo 2 ALIVE non ricevo nessun END vuole dire che sono solo invio end
                        }
                    }
                    else {
                        this.retry++;
                        if(this.retry > 2) {
                            this.setNew();//non sono riuscito a fare il join entro 6 secondi sicuramento ho fallito percio riprovo
                        }
                    }
                    this.msgSender.update(msg.getFrom(), msg.getTimestamp());
                    break;
                case(Constants.STATE_JOINED):
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINED);
                    this.msgSender.update(msg.getFrom(), msg.getTimestamp());
                    break;
                case(Constants.STATE_CHANGE):
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_CHANGE);
                    this.msgSender.update(msg.getFrom(), msg.getTimestamp());
                    break;
            }
    }

    public void handleDrop(ReliableMsg msg) throws IOException {
            switch(this.state) {
                case(Constants.STATE_NEW):
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_NEW);
                    break;
                case(Constants.STATE_JOINING):
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINING);
                    if(this.endMap != null) {
                        this.msgSender.setLastRemoveIpShadow(msg.getBody()); //purtroppo gia ho fatto un join, cerco di rimediare
                        this.endMap.replace(this.msgSender.getLastRemoveIp(), true);
                        if(debug) {
                            System.out.println("endMap state");
                            for(Map.Entry<String, Boolean> entry : this.endMap.entrySet()) {
                                System.out.println(entry.getKey() + "=" + entry.getValue());
                            }
                        }
                        if(this.checkendMap()) {
                            this.setJoined();
                        }
                    }
                    else {
                        this.msgSender.setLastRemoveIpShadow(msg.getBody());//rimediare dopo
                    }
                    break;
                case(Constants.STATE_JOINED):
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_JOINED);
                    this.setChange();
                    this.msgSender.setLastRemoveIp(msg.getBody());
                    this.generateendMap();
                    this.endMap.remove(msg.getBody());//rimuove il che non serve
                    if(debug) {
                        System.out.println("endMap state");
                        for(Map.Entry<String, Boolean> entry : this.endMap.entrySet()) {
                            System.out.println(entry.getKey() + "=" + entry.getValue());
                        }
                    }
                    if(messageBuffer.isMessageQueueEmpty()) {
                        this.msgSender.sendEnd();
                    }
                    break;
                case(Constants.STATE_CHANGE):
                    msgLogger.printLog(msg,Constants.MSG_SUCC,null,Constants.STATE_CHANGE);
                    this.msgSender.setLastRemoveIpShadow(msg.getBody());
                    this.endMap.replace(this.msgSender.getLastRemoveIp(), true);//shadow remove
                    if(debug) {
                        System.out.println("endMap state");
                        for(Map.Entry<String, Boolean> entry : this.endMap.entrySet()) {
                            System.out.println(entry.getKey() + "=" + entry.getValue());
                        }
                    }
                    if(this.checkendMap()) {
                        this.setJoined();
                    }
                    break;
            }
    }

    public void setNew() {
        this.state = Constants.STATE_NEW;
        this.msgSender.setFalse();
        this.retry = 0;
    }

    public void setJoining() {
        this.state = Constants.STATE_JOINING;
    }

    public void setChange() {
        this.state = Constants.STATE_CHANGE;
        this.msgSender.setFalse();
    }

    public void setJoined() {
        this.state = Constants.STATE_JOINED;
        this.msgSender.setTrue();
        System.out.println("Joined actual members: " + this.msgSender.getMember().toString());

        this.messageBuffer.updateFIFOQueue(this.msgSender.getMember());
        this.messageBuffer.reset();
    }

    public void generateendMap() {
        this.endMap = new HashMap<>();
        for(String a : msgSender.getMember()) {
            this.endMap.put(a, false);
        }
    }

    public boolean checkendMap() {
        for(Map.Entry<String, Boolean> entry : this.endMap.entrySet()) {
            if(!entry.getValue()) {
                return false;
            }
        }
        return true;
    }

}