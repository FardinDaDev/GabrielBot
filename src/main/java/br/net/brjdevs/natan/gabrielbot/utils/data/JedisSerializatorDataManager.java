package br.net.brjdevs.natan.gabrielbot.utils.data;

import com.google.common.base.Preconditions;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class JedisSerializatorDataManager<T> implements DataManager<SerializedData<T>> {
    private static Map<String, JedisPool> pools = new ConcurrentHashMap<>();

    private final JedisPool pool;
    private final SerializedData<T> data;

    public JedisSerializatorDataManager(JedisPool pool, String prefix) {
        this.pool = Preconditions.checkNotNull(pool, "pool");
        data = new SerializedData<>(null, (key, value)->{
            try(Jedis j = pool.getResource()) {
                j.set(prefix + key, value);
            }
        }, (key)->{
            try(Jedis j = pool.getResource()) {
                return j.get(prefix + key);
            }
        }, (key)->{
            try(Jedis j = pool.getResource()) {
                j.del(prefix + key);
            }
        });
    }

    public JedisSerializatorDataManager(String host, int port, String prefix) {
        this(pools.computeIfAbsent(host + port, k->new JedisPool(host, port)), prefix);
    }

    public JedisSerializatorDataManager(String host, int port) {
        this(host, port, "");
    }

    public JedisPool getPool() {
        return pool;
    }

    @Override
    public void save() {
        data.save();
        run(Jedis::bgsave);
    }

    @Override
    public SerializedData<T> get() {
        return data;
    }

    public void run(Consumer<Jedis> consumer) {
        try(Jedis j = pool.getResource()) {
            consumer.accept(j);
        }
    }
}
