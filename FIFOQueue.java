import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

public class FIFOQueue {
	private Queue<String> queue = new LinkedList<>();
    private AtomicInteger sequence = new AtomicInteger(0);
    
    // 添加消息到队列，同时增加序列号
    public void send(String message) {
        synchronized(queue) {
            queue.add(message + "|" + sequence.incrementAndGet());
            queue.notify();
        }
    }
    
    // 从队列接收消息，按FIFO顺序
    public String receive() {
        synchronized(queue) {
            while(queue.isEmpty()) {
                try {
                    queue.wait();
                } catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return queue.poll().split("\\|")[0]; // 移除并返回序列号之前的消息部分
        }
    }

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// 创建 MessageQueue 的实例
        FIFOQueue queue = new FIFOQueue();

        // 向队列中添加消息
        queue.send("Hello");
        queue.send("World"); 
        queue.send("This");
        queue.send("Is");
        queue.send("FIFO");

        // 从队列中接收消息
        System.out.println(queue.receive()); // 应该输出 Hello
        System.out.println(queue.receive()); // 应该输出 World
        System.out.println(queue.receive()); // 应该输出 This
        System.out.println(queue.receive()); // 应该输出 Is
        System.out.println(queue.receive()); // 应该输出 FIFO
	}

}
