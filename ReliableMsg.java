import java.io.Serializable;
import java.util.Objects;

public class ReliableMsg implements Serializable {
    private String type;
    private String creator; //who create the original message
    private String from;
    private Long timestamp;
    private String body;
    private String index; // part of the logical clock
    public ReliableMsg(String type, String creator, String from, Long timestamp, String body, String index) {
        this.type = type;
        this.creator = creator;
        this.from = from;
        this.timestamp = timestamp;
        this.body = body;
        this.index = index;
    }

    public void print() {
        System.out.println("type: " + type);
        System.out.println("creator: " + creator);
        System.out.println("from: " + from);
        System.out.println("timestamp: " + timestamp);
        System.out.println("index: " + index);
        System.out.println("body: " + body);
    }

    public String getType() {
        return type;
    }

    public String getCreator() {
        return creator;
    }

    public String getFrom() {
        return from;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getBody() {
        return body;
    }

    public String getIndex() {
        return this.index;
    }


}
