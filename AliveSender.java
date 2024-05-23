public class AliveSender implements  Runnable{
    private final MsgSender msgSender;
    private final boolean debug;
    private final Running running;

    public AliveSender(MsgSender msgSender, boolean debug, Running running) {
        this.msgSender = msgSender;
        this.debug = debug;
        this.running = running;
    }
    public void run() {
        System.out.println("Alive sender ready");
        while(running.isRunning()) {
            msgSender.sendAlive();
            msgSender.checkTimestamp();
            try {
                Thread.sleep(5000);
                if(debug){
                    System.out.println("Alive sender sleep 5s");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("Alive sender terminated");
    }
}