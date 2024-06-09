import java.util.*;



public class MessageBuffer {

    private Map<String, Queue<ReliableMsg>> FIFOQueue;
    private Map<String, Integer> expectedSequenceNumber;  // Used for FIFO
    private PriorityQueue<ReliableMsg> messageQueue;
    private ACKManager ackManager;  // 加入ACK管理器
    private Boolean debug;

    public MessageBuffer(Boolean debug) {
        this.FIFOQueue = new HashMap<>();  // New HashMap to buffer msg from different id
        this.expectedSequenceNumber = new HashMap<>(); //初始化用于存放FIFO递增序列号的表
        this.messageQueue = new PriorityQueue<>(Comparator  // 初始化 PriorityQueue，按照 scalar clock 和 process ID 排序
                .comparingInt(ReliableMsg::getScalarclock)
                .thenComparing(ReliableMsg::getFrom));
        this.ackManager = new ACKManager();  // 初始化ACK管理器
        this.debug = debug;
    }


    // 接到新消息后执行，这一层是判断FIFO的
    public synchronized void newMessage(ReliableMsg msg, Set<String> members) {
        //System.out.println(msg.getFrom());
        ackManager.removeMessageFromWaiting(getMessageId(msg));
        String processId = msg.getFrom();
        int seqNumber = msg.getSequenceNumber();
        Queue<ReliableMsg> queue = FIFOQueue.get(processId);
        // 检查新接收到的msg sequenceNumber是否符合预期
        //System.out.println("before"+expectedSequenceNumber.get(processId));
        if (seqNumber == expectedSequenceNumber.get(processId)) {
            if (Objects.equals(msg.getType(), "MSG")){
                messageQueue.offer(msg);
                expectedSequenceNumber.put(processId, seqNumber + 1);
                checkAndProcessBufferedMessages(processId, members);
                //System.out.println("after"+expectedSequenceNumber.get(processId));
            } else if (Objects.equals(msg.getType(), "ACK")) {
                ackManager.addACK(msg);
                delivery(members);
                expectedSequenceNumber.put(processId, seqNumber + 1);
                checkAndProcessBufferedMessages(processId, members);
            }
        } else {
            queue.add(msg);
        }
        //
        if(debug) {
            System.out.println("This is queue of" + processId);
            for (ReliableMsg tmp : queue) {
                System.out.println("type : " + tmp.getType() + ";" + "from " + tmp.getFrom() + ";" + "sequence number : " + tmp.getSequenceNumber() + ";" + "clock : " + tmp.getScalarclock());
            }
//            System.out.println("These are members");
//            for (String tmp : members) {
//                System.out.println(tmp);
//            }
            System.out.println("This is expected SN");
            for (Map.Entry<String, Integer> entry : expectedSequenceNumber.entrySet()) {
                System.out.println("ip : " + entry.getKey() + ";" + "expectedSequenceNumber : " + entry.getValue());
            }
            System.out.println("This is message SN" + seqNumber);
        }
    }

    public synchronized void delivery(Set<String> members) {
        while (!ackManager.WaitingForIt.isEmpty()) {
            // 如果WaitingForIt不为空，则暂停delivery
            return;
        }
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
    // 检查之前缓存过的消息是否可以被继续处理
    private void checkAndProcessBufferedMessages(String processId, Set<String> members) {
        Queue<ReliableMsg> queue = FIFOQueue.get(processId);
        while (!queue.isEmpty() && queue.peek().getSequenceNumber() == expectedSequenceNumber.get(processId)) {
            ReliableMsg msg = queue.poll();
            assert msg != null;
            if(msg.getType().equals("MSG")) {
                messageQueue.offer(msg);
                expectedSequenceNumber.put(processId, expectedSequenceNumber.get(processId) + 1);
            } else if (Objects.equals(msg.getType(), "ACK")) {
                ackManager.addACK(msg);
                delivery(members);
            }

        }
    }

    public synchronized void newMessageACK(ReliableMsg msg) {
        // 其他消息处理逻辑...
        // 调用ACKManager来处理ACK
        ackManager.initialACK(msg);  // 假设ACK来自消息的发送者
    }

    // 生成消息的唯一标识符
    public String getMessageId(ReliableMsg message) {
        return message.getFrom() + ":" + message.getScalarclock();
    }

    public synchronized boolean isMessageQueueEmpty() {
        return messageQueue.isEmpty();
    }

    public synchronized void reset() {
        this.ackManager.reset();
    }

}