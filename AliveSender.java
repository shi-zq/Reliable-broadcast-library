public class AliveSender implements  Runnable{
    private MsgSender msgSender;

    public AliveSender(MsgSender msgSender) {
        this.msgSender = msgSender;
    }
    public void run() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("AliveSender ready");
        while(true) {
            msgSender.sendAlive();
            msgSender.checkTimestamp();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}