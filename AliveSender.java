public class AliveSender implements  Runnable {
    private final MsgSender msgSender;
    private final boolean debug;

    public AliveSender(MsgSender msgSender, boolean debug){
        this.msgSender = msgSender;
        this.debug = debug;
    }

    public void run() {
        System.out.println("Alive sender ready");
        while(true){
            this.msgSender.sendAlive();
            this.msgSender.checkTimestamp();
            try {
                Thread.sleep(3000);
            }
            catch(InterruptedException e){
                throw new RuntimeException(e);
            }
        }
    }
}