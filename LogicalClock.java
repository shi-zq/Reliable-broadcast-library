public class LogicalClock {
    private int scalarclock;
    private final String processId;

    // To create a singleton logical clock.
    LogicalClock(String processId) {
        this.scalarclock = 0;
        this.processId = processId;
    }

    // Solo instance
    private static LogicalClock instance = null;

    // Access point
    public static synchronized LogicalClock getInstance(String processId) {
        if (instance == null) {
            instance = new LogicalClock(processId);
        }
        return instance;
    }

    // To get scalar clock and add
    public synchronized int getScalarclock() {
        this.scalarclock++;
        return scalarclock-1;
    }
    // reset the clock
    public synchronized void reset() {
        this.scalarclock = 0;
    }

    // To update scalar clock
    public synchronized void updateScalarclock(int receivedclock) {
        if (receivedclock >= this.scalarclock) {
            this.scalarclock = receivedclock + 1;
        } else {
            this.scalarclock++;
        }
    }
}