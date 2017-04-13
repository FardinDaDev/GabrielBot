package br.net.brjdevs.natan.gabrielbot.utils.data;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.function.Consumer;

public class JedisSerializatorDataManager<T> implements DataManager<SerializedData<T>> {
    private final JedisPool pool;
    private final SerializedData<T> data;

    public JedisSerializatorDataManager(String host, int port, String prefix) {
        pool = new JedisPool(host, port);
        data = new SerializedData<T>(null, (key, value)->{
            try(Jedis j = pool.getResource()) {
                j.set(prefix + key, value);
            }
        }, (key)->{
            try(Jedis j = pool.getResource()) {
                return j.get(prefix + key);
            }
        });
    }

    public JedisSerializatorDataManager(String host, int port) {
        this(host, port, "");
    }

    @Override
    public void save() {
        data.save();
        run(Jedis::save);
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
