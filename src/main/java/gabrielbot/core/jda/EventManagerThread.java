package gabrielbot.core.jda;

public class EventManagerThread extends Thread {
    private final int shardId, threadNumber;

    EventManagerThread(Runnable r, int shardId, int threadNumber) {
        super(r);
        this.shardId = shardId;
        this.threadNumber = threadNumber;
    }

    public Thread newThread(Runnable r, String name) {
        EventManagerThread e = new EventManagerThread(r, shardId, threadNumber);
        e.setUncaughtExceptionHandler(getUncaughtExceptionHandler());
        e.setName(name);
        e.setDaemon(true);
        e.setPriority(MIN_PRIORITY);
        return e;
    }

    public int getShardId() {
        return shardId;
    }

    public int getThreadNumber() {
        return threadNumber;
    }

    @Override
    public int hashCode() {
        return shardId<<25 | threadNumber;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EventManagerThread && obj.hashCode() == hashCode();
    }

    @Override
    public String toString() {
        return "EventManagerThread{shardId = " + shardId + ", id = " + threadNumber + "}";
    }

    public static EventManagerThread current() {
        Thread current = Thread.currentThread();
        return current instanceof EventManagerThread ? (EventManagerThread)current : null;
    }
}
