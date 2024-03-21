import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MsgSender {
    //classe per invio dei messaggi e unirsi al gruppo
    private String multicastAddress;

    public MsgSender(String multicastAddress) {
        this.multicastAddress = multicastAddress;
    }

    public void sendMsg(String msg, String from, String msgType, int view) {
        LocalTime time = LocalTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("HHmmss");
        ReliableMsg rmsg = new ReliableMsg(msg, msgType, time.format(myFormatObj), from, view);
    }

    //per testing
    public static void main(String[] args) {

    }
}
