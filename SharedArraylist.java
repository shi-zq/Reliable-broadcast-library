import java.util.ArrayList;
import java.util.HashMap;

public class SharedArraylist {
    private HashMap<ReliableMsg, AckMap> notACKmsg;
    private ArrayList<ReliableMsg> ACKmsg;
    private String type;

    public SharedArraylist(String type) {
        notACKmsg = new HashMap<>();
        ACKmsg = new ArrayList<>();
        this.type = type;
    }

    public synchronized ReliableMsg getMsg() {
        if(ACKmsg.isEmpty()) {
            return null;
        }
        return ACKmsg.removeFirst();
    }

    public synchronized void updateAck(ReliableMsg msg) {
        AckMap tmp = this.notACKmsg.get(msg);
        tmp.ack(msg.getFrom());
        if(tmp.missingAck().isEmpty()) {
            ACKmsg.add();
        }
    }

    public synchronized void addMsg(ReliableMsg) {

    }
    public synchronized boolean isEmpty() {
        return msgBuffer.isEmpty();
    }

}
