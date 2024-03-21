public class ReliableBroadcast implements Runnable{
    private MsgSender msgSender;
    private String multicastAddress;
    public ReliableBroadcast(String multicastAddress) {
        this.msgSender = new MsgSender(multicastAddress);
        this.multicastAddress = multicastAddress;

    }

    @Override
    public void run() {

    }
}
