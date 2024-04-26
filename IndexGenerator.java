public class IndexGenerator {
    private LogicalClock logicalClock;
    private String type;

    public IndexGenerator(LogicalClock logicalClock, String type) {
        this.logicalClock = logicalClock;
        this.type = type;
    }

    public String getIndex() {
        switch(type) {
            case ("FIFO"):
                return String.valueOf(logicalClock.getScalarclock());
            case ("CASUAL"):
                return null;
            case ("TOTAL"):
                return null;
            default:
                return null;
        }
    }

    public void resetIndex() {
        switch(type) {
            case ("FIFO"):
                logicalClock.reset();
                break;
            case ("CASUAL"):
                //return null;
            case ("TOTAL"):
                //return null;
        }
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }
}
