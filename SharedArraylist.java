import java.util.ArrayList;

public class SharedArraylist {
    private ArrayList<ReliableMsg> msgBuffer;

    public SharedArraylist() {
        this.msgBuffer = new ArrayList<ReliableMsg>();
    }

    public synchronized ReliableMsg getMsg() {
        if(msgBuffer.isEmpty()) {
            return null;
        }
        return msgBuffer.removeFirst();
    }
    public synchronized void addMsg(ReliableMsg msg) {
        this.msgBuffer.add(msg);
    }

    public synchronized boolean isEmpty() {
        return msgBuffer.isEmpty();
    }

}
