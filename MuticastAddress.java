public class MuticastAddress {
    private String multicastAddress;

    public MuticastAddress(String multicastAddress) {
        this.multicastAddress = multicastAddress;
    }

    synchronized public String get() {
        return multicastAddress;
    }
}
