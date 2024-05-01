import java.util.*;

public class AckMap {
    private ReliableMsg msg;
    private HashMap<String, Boolean> ackMap;


    public AckMap(ReliableMsg msg, HashSet<String> memberList) {
        this.msg = msg;
        this.ackMap = new HashMap<>();
        for(String tmp : memberList) {
            this.ackMap.put(tmp, false);
        }
    }

    public AckMap(ReliableMsg msg) {
        this.msg = msg;
    }

    public void ack(String from) {
        this.ackMap.replace(from, false, true);
    }

    public ArrayList<String> missingAck() {
        ArrayList<String> tmp = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : ackMap.entrySet()) {
            if(!entry.getValue()) {
                tmp.add(entry.getKey());
            }
        }
        return tmp;
    }

    public ReliableMsg getMsg() {
        return this.msg;
    }

    public int hashCode() {
        return Objects.hashCode(this.msg.getFrom() + this.msg.getIndex());
    }

    //un confronto di messaggio non molto preciso
    //forse megloi un extend di Reliablemsg
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o.getClass() != this.msg.getClass()) {
            return false;
        }
        ReliableMsg tmp = (ReliableMsg) o;
        return this.msg.getIndex().equals(tmp.getIndex()) && this.msg.getCreator().equals(tmp.getCreator());
    }

}