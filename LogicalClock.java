public class LogicalClock {
    private int scalarclock;
    private final String processId;
    
    // To create a singleton logical clock.
    private LogicalClock(String processId) {
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
    
    // To get scalar clock
    public synchronized int getScalarclock() {
        return scalarclock;
    }
    
    // To update scalar clock
    public synchronized void updateScalarclock(int receivedclock) {
    	if (receivedclock >= this.scalarclock) {
    		this.scalarclock = receivedclock + 1;
    	} else {
    		this.scalarclock++;
    	}
    }
    
    public synchronized void incrementScalarclock() {
        this.scalarclock++;
    }
    
}