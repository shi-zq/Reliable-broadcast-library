public class ReliableMsg {
    // il classe usato per la comunicazione tra processi

    private String body;
    private String type;
    private String msgId;
    private String from;

    public ReliableMsg(String body, String type, String msgId, String from) {
        this.body = body;
        this.type = type;
        this.msgId = msgId;
        this.from = from;
    }
}
