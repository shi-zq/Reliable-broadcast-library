import java.io.Serializable;
import java.util.Objects;

public class ReliableMsg implements Serializable {
    private String type;
    private String from;
    private Long timestamp;
    private String body;
    private String index; // part of the logical clock
    public ReliableMsg(String type, String from, Long timestamp, String body, String index) {
        this.type = type;
        this.from = from;
        this.timestamp = timestamp;
        this.body = body;
        this.index = index;
    }

    public void print() {
        System.out.println("type: " + type);
        System.out.println("from: " + from);
        System.out.println("timestamp: " + timestamp);
        System.out.println("index: " + index);
        System.out.println("body: " + body);
    }

    public String getType() {
        return type;
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

    public int hashCode() {
        return Objects.hashCode(this.from + this.index);
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o.getClass() != this.getClass()) {
            return false;
        }
        ReliableMsg tmp = (ReliableMsg) o;
        return this.index.equals(tmp.getIndex()) && this.from.equals(tmp.getFrom());
    }

}
