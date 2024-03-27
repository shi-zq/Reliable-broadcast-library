import java.util.HashMap;

public class SharedResource {

    private boolean state;
    //true for sending false for waiting
    private HashMap<String, Long> viewMember;

    public SharedResource() {
        this.state = false;
        this.viewMember = new HashMap<String, Long>();
    }
    public synchronized boolean getState() {
        return state;
    }

    public synchronized void setFalse() {
        this.state = false;
    }

    public synchronized void update(String ip, Long time) {

    }


}
