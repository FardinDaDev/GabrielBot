package br.net.brjdevs.natan.gabrielbot.utils.data;

import com.google.common.base.Preconditions;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class JedisDataManager {
    private static Map<String, JedisPool> pools = new ConcurrentHashMap<>();

    private final JedisPool pool;
    private final String prefix;

    public JedisDataManager(JedisPool pool, String prefix) {
        this.pool = Preconditions.checkNotNull(pool, "pool");
        this.prefix = prefix == null ? "" : prefix;
    }

    public JedisDataManager(String host, int port, String prefix) {
        this(pools.computeIfAbsent(host + port, k->new JedisPool(host, port)), prefix);
    }

    public JedisDataManager(String host, int port) {
        this(host, port, "");
    }

    public void set(String key, String value) {
        run((Consumer<Jedis>)j->j.set(prefix + key, value));
    }

    public String get(String key) {
        return run((Function<Jedis, String>)j->j.get(prefix + key));
    }

    public void remove(String key) {
        run((Consumer<Jedis>)j->j.del(prefix + key));
    }

    public JedisPool getPool() {
        return pool;
    }

    public void save() {
        run(Jedis::save);
    }

    public <T> T run(Function<Jedis, T> function, Function<Throwable, T> fail) {
        try {
            try(Jedis j = pool.getResource()) {
                return function.apply(j);
            }
        } catch(Throwable t) {
            return fail == null ? null : fail.apply(t);
        }
    }

    public <T> T run (Function<Jedis, T> function) {
        return run(function, null);
    }

    public void run(Consumer<Jedis> consumer, Consumer<Throwable> fail) {
        run(j->{
            consumer.accept(j);
            return null;
        }, fail == null ? t->null : t->{
            fail.accept(t);
            return null;
        });
    }

    public void run(Consumer<Jedis> consumer) {
        run(consumer, null);
    }
}
