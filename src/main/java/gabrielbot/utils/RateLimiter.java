package gabrielbot.utils;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("all")
public class RateLimiter {
    private static final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor(r->{
        Thread t = new Thread(r, "RateLimiterThread");
        t.setDaemon(true);
        return t;
    });

    private final ConcurrentHashMap<String, Pair<AtomicInteger, Long>> usersRateLimited = new ConcurrentHashMap<>();

    private final long max;
    private final long timeout;
    private final boolean resetAll;

    public RateLimiter(int max, int timeout, boolean resetAll) {
        this.max = max;
        this.timeout = timeout;
        this.resetAll = resetAll;
    }

    public RateLimiter(int max, int timeout) {
        this(max, timeout, false);
    }

    //Basically where you get b1nzy'd.
    public boolean process(String key) {
        Pair<AtomicInteger, Long> p = usersRateLimited.get(key);
        if(p == null) {
            usersRateLimited.put(key, p = new MutablePair<>(new AtomicInteger(), null));
        }
        AtomicInteger a = p.getKey();

        synchronized(a) {
            long i = a.get();
            if(i >= max) return false;

            a.incrementAndGet();
            long now = System.currentTimeMillis();
            Long tryAgain = p.getValue();
            if(tryAgain == null || tryAgain < now) {
                p.setValue(now + timeout);
            }

            ses.schedule(()->{
                if(resetAll || a.decrementAndGet() == 0) {
                    usersRateLimited.remove(key);
                }
            }, timeout, TimeUnit.MILLISECONDS);
            return true;
        }
    }

    public long tryAgainIn(String key) {
        Pair<AtomicInteger, Long> p = usersRateLimited.get(key);
        if(p == null || p.getValue() == null) return 0;
        return Math.max(p.getValue()-System.currentTimeMillis(), 0);
    }
}
