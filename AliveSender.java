public class AliveSender implements  Runnable{
    private MsgSender msgSender;

    public AliveSender(MsgSender msgSender) {
        this.msgSender = msgSender;
    }
    public void run() {
        while(!Thread.currentThread().isInterrupted()) {
            msgSender.sendAlive();
        }
    }
}
