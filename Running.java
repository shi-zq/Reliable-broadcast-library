public class Running {
    private boolean running;

    public Running() {
        this.running = true;
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public synchronized void setRunning() {
        this.running = false;
    }
}
