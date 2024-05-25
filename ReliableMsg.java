import java.io.Serializable;

public class ReliableMsg implements Serializable{
    private String type;
    private String from;
    private Long timestamp;
    private int view;
    private String body;
    private int sequenceNumber;
    private int scalarclock;

    public ReliableMsg(String type, String from, Long timestamp, int view, String body, int scalarclock, int sequenceNumber){
        this.type = type;
        this.from = from;
        this.timestamp = timestamp;
        this.view = view;
        this.body = body;
        this.scalarclock = scalarclock;
        this.sequenceNumber = sequenceNumber;
    }

    public String getType(){
        return type;
    }

    public String getFrom(){
        return from;
    }

    public Long getTimestamp(){
        return timestamp;
    }

    public int getView(){
        return view;
    }

    public String getBody(){
        return body;
    }
    
    public int getScalarclock(){
        return scalarclock;
    }
    
    public int getSequenceNumber(){
        return sequenceNumber;
    }
}
