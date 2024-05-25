import java.util.*;



public class MessageBuffer {
	
	private Map<String, Queue<ReliableMsg>> FIFOQueue;
	private Map<String, Integer> expectedSequenceNumber;  // Used for FIFO	
    private PriorityQueue<ReliableMsg> messageQueue;
    private int roommates;
    private ACKManager ackManager;  // 加入ACK管理器

    public MessageBuffer() {
        this.FIFOQueue = new HashMap<>();  // New HashMap to buffer msg from different id
        this.expectedSequenceNumber = new HashMap<>(); //初始化用于存放FIFO递增序列号的表
        this.messageQueue = new PriorityQueue<>(Comparator  // 初始化 PriorityQueue，按照 scalar clock 和 process ID 排序
                .comparingInt(ReliableMsg::getScalarclock)
                .thenComparing(ReliableMsg::getFrom));
        this.ackManager = new ACKManager();  // 初始化ACK管理器
    }
    

    // 接到新消息后执行，这一层是判断FIFO的
    public synchronized void newMessage(ReliableMsg msg) {
    	//System.out.println(msg.getFrom());
    	
    	String processId = msg.getFrom();
        int seqNumber = msg.getSequenceNumber();
        Queue<ReliableMsg> queue = FIFOQueue.get(processId);
        // 检查新接收到的msg sequenceNumber是否符合预期
        //System.out.println("before"+expectedSequenceNumber.get(processId));
        if (seqNumber == expectedSequenceNumber.get(processId)) {
            messageQueue.offer(msg);
            expectedSequenceNumber.put(processId, seqNumber + 1);
            checkAndProcessBufferedMessages(processId);
            //System.out.println("after"+expectedSequenceNumber.get(processId));
        } else {
            queue.add(msg);
        }
    }

//    public synchronized void receiveACK(String body) {
//        // 遍历队列找到对应消息，增加acknowledged计数
//        for (ReliableMsg msg : messageQueue) {
//            if (msg.getBody().equals(body)) {
//                msg.incrementAcknowledged();
//                break;
//            }
//        }
//    }

    public synchronized void delivery(Set<String> members) {
        while (!messageQueue.isEmpty() && ackManager.isFullyAcknowledged(messageQueue.peek(), members)) {
            ReliableMsg msg = messageQueue.poll();
            System.out.println("Delivering message: " + msg.getBody());
            // 这里可以添加更多的消息处理逻辑
        }
    }
    // 更新FIFOQueue
    public synchronized void updateFIFOQueue(Set<String> members) {
        for(String ip : members) {
            FIFOQueue.put(ip, new LinkedList<>());
            expectedSequenceNumber.put(ip, 0);
        }
    }
    
    public synchronized void addRoommate() {
    	this.roommates++;
    }

    // 减少一个室友
    public synchronized void removeRoommate() {
        if (this.roommates > 0) { // roommates不会变成负数
            this.roommates--;
        }
    }
    // 检查之前缓存过的消息是否可以被继续处理
    private void checkAndProcessBufferedMessages(String processId) {
        Queue<ReliableMsg> queue = FIFOQueue.get(processId);
        while (!queue.isEmpty() && queue.peek().getSequenceNumber() == expectedSequenceNumber.get(processId)) {
            ReliableMsg msg = queue.poll();
            messageQueue.offer(msg);
            expectedSequenceNumber.put(processId, expectedSequenceNumber.get(processId) + 1);
        }
    }
    
    public synchronized void newMessageACK(ReliableMsg msg) {
        // 其他消息处理逻辑...
        // 调用ACKManager来处理ACK
        ackManager.initialACK(msg);  // 假设ACK来自消息的发送者
    }
    
    public synchronized void receiveACK(ReliableMsg msg) {
        // 其他消息处理逻辑...
        // 调用ACKManager来处理ACK
        ackManager.addACK(msg);  // 假设ACK来自消息的发送者
    }
    
    // 生成消息的唯一标识符
    public String getMessageId(ReliableMsg message) {
    	return message.getFrom() + ":" + String.valueOf(message.getTimestamp());
    }

    public synchronized boolean isMessageQueueEmpty() {
        return messageQueue.isEmpty();
    }
    
}