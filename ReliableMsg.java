import java.io.Serializable;
import java.util.Objects;

public class ReliableMsg implements Serializable {
    private String type;
    private String from;
    private Long timestamp;
    private String index;
    private String indexType;
    private int view;
    private String body;

    public ReliableMsg(String type, String from, Long timestamp, String index, String indexType, int view, String body) {
        this.type = type;
        this.from = from;
        this.timestamp = timestamp;
        this.index = index;
        this.indexType = indexType;
        this.view = view;
        this.body = body;
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

    public String getIndex() {
        return index;
    }

    public String getIndexType() {
        return indexType;
    }

    public int getView() {
        return view;
    }

    public String getBody() {
        return body;
    }
}
