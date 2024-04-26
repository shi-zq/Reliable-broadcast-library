import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class SharedArraylist {
    private LinkedList<AckMap> notACKmsg;
    private ArrayList<ReliableMsg> ACKmsg;
    private String type;
    private MsgSender msgSender;

    public SharedArraylist(String type, MsgSender msgSender) {
        notACKmsg = new LinkedList<>();
        ACKmsg = new ArrayList<>();
        this.type = type;
        this.msgSender = msgSender;
    }

    public synchronized ReliableMsg getMsg() {
        if(ACKmsg.isEmpty()) {
            return null;
        }
        return ACKmsg.removeFirst();
    }

    public synchronized void updateAck(ReliableMsg msg) {
        AckMap tmp = new AckMap(msg);
        int index = notACKmsg.indexOf(tmp);
        notACKmsg.get(index).ack(msg.getFrom());
        while(!notACKmsg.isEmpty() && notACKmsg.getFirst().missingAck().isEmpty()) {
            ACKmsg.add(notACKmsg.removeFirst().getMsg());
        }
    }

    public synchronized void addMsg(ReliableMsg msg) {
        AckMap tmp = new AckMap(msg, msgSender.getMember());
        switch(type) {
            case ("FIFO"):
                notACKmsg.add(tmp);
                break;
            case ("CASUAL"):
                notACKmsg.add(tmp);
                break;
            case ("TOTAL"):
                notACKmsg.add(tmp);
                break;
            default:
                notACKmsg.add(tmp);
                break;
        }
    }

}
