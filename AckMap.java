import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AckMap {
    private HashMap<String, Boolean> ackMap;

    public AckMap(ArrayList<String> memberList) {
        this.ackMap = new HashMap<>();
        for(String tmp : memberList) {
            this.ackMap.put(tmp, false);
        }
    }

    public void ack(String from) {
        this.ackMap.replace(from, false, true);
    }

    public ArrayList<String> missingAck() {
        ArrayList<String> tmp = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : ackMap.entrySet()) {
            if(!entry.getValue()) {
                tmp.add(entry.getKey());
            }
        }
        return tmp;
    }

}
