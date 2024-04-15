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
                return logicalClock.getScalarclock();
            case ("CASUAL"):
                return null;
            case ("TOTAL"):
                return null;
        }
    }

    public void resetIntex() {
        switch(type) {
            case ("FIFO"):
                logicalClock.reset();
            case ("CASUAL"):
                //return null;
            case ("TOTAL"):
                //return null;
        }
    }
}
