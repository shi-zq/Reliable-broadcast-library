import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ACKManager {
    private Map<String, Set<String>> acks;  // 存储消息标识符及对应的ACK IP地址集合
    private int totalMembers;  // 网络成员总数

    public ACKManager() {
        this.acks = new HashMap<>();
    }
    // attention :Consider thelastremoved ip
    // ACKManager 添加新消息
    public synchronized void initialACK(ReliableMsg message) {
        String messageId = getMessageId(message);
        String ip = message.getFrom();
        Set<String> ips = acks.getOrDefault(messageId, new HashSet<>());
        ips.add(ip);
        acks.put(messageId, ips);
    }

    // 添加ACK，使用消息的标识符（例如：序列号+发送者）
    public synchronized void addACK(ReliableMsg message) {
        String messageId = message.getBody();
        String ip = message.getFrom();
        Set<String> ips = acks.getOrDefault(messageId, new HashSet<>());
        ips.add(ip);
        acks.put(messageId, ips);
    }

    // 获取某条消息的ACK数量
    public synchronized int getAckCount(ReliableMsg message) {
        String messageId = getMessageId(message);
        return acks.getOrDefault(messageId, new HashSet<>()).size();
    }

    // 生成消息的唯一标识符
    private synchronized String getMessageId(ReliableMsg message) {
    	return message.getFrom() + ":" + String.valueOf(message.getTimestamp());
    }

    // Judging that weather a message is acknowledged by all.
    public synchronized boolean isFullyAcknowledged(ReliableMsg message, Set<String> members) {
        String messageId = getMessageId(message);
        Set<String> receivedAcks = acks.getOrDefault(messageId, new HashSet<>());

        return receivedAcks.containsAll(members);
    }
}
