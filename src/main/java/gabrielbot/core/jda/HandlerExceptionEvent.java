package gabrielbot.core.jda;

public class HandlerExceptionEvent {
    private final Thread where;
    private final Throwable what;

    public HandlerExceptionEvent(Thread where, Throwable what) {
        this.where = where;
        this.what = what;
    }

    public Thread getThread() {
        return where;
    }

    public Throwable getError() {
        return what;
    }
}
