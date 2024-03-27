import java.util.HashMap;

public class SharedResource {

    private String state;
    //true for sending false for waiting
    private HashMap<String, Long> viewMember;

    public SharedResource() {
        this.state = "new";
        this.viewMember = new HashMap<String, Long>();
    }
    public synchronized String getState() {
        return state;
    }

    public synchronized void setJoined() {
        this.state = "joined";
    }

    public synchronized void setChange() {
        this.state = "change";
    }

    public synchronized void update(String ip, Long time) {

    }


}
