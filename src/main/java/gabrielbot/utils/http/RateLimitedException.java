package gabrielbot.utils.http;

public class RateLimitedException extends RuntimeException {
    private final long tryAgainIn;

    public RateLimitedException(long tryAgainIn) {
        this.tryAgainIn = tryAgainIn;
    }

    public long getTryAgainIn() {
        return tryAgainIn;
    }
}
