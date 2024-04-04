import java.io.Serializable;

public class ReliableMsg implements Serializable {
    private String type;
    private String from;
    private Long timestamp;
    private String body;
    public ReliableMsg(String type, String from, Long timestamp, String body) {
        this.type = type;
        this.from = from;
        this.timestamp = timestamp;
        this.body = body;
    }

    public void print() {
        System.out.println("type: " + type);
        System.out.println("from: " + from);
        System.out.println("timestamp: " + timestamp);
        System.out.println("body: " + body);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
    //add part to extract information from body of the msg
}
