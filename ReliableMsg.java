import java.io.Serializable;

public class ReliableMsg implements Serializable {
    // il classe usato per la comunicazione tra processi

    private String body;
    private String type;
    private String msgId;
    private String from;

    private int view;

    public ReliableMsg(String body, String type, String msgId, String from, int view) {
        this.body = body;
        this.type = type;
        this.msgId = msgId;
        this.from = from;
        this.view = view;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public int getView() {
        return view;
    }

    public void setView(int view) {
        this.view = view;
    }
}
