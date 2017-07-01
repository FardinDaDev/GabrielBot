package gabrielbot.utils.data;

import com.esotericsoftware.kryo.pool.KryoPool;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.TimeUnit;

public class JedisSerializatorDataManager<T> extends JedisDataManager implements DataManager<SerializedData<T>> {
    private SerializedData<T> data;

    public JedisSerializatorDataManager(JedisPool pool, String prefix, KryoPool kryo, int cacheSize, int expireAfter, TimeUnit unit) {
        super(pool, prefix);
        data = new SerializedData<>(kryo, cacheSize, expireAfter, unit, this::set, this::get, this::remove);
    }

    public JedisSerializatorDataManager(String host, int port, String prefix, KryoPool kryo, int cacheSize, int expireAfter, TimeUnit unit) {
        super(host, port, prefix);
        data = new SerializedData<>(kryo, cacheSize, expireAfter, unit, this::set, this::get, this::remove);
    }

    public JedisSerializatorDataManager(String host, int port, KryoPool kryo, int cacheSize, int expireAfter, TimeUnit unit) {
        super(host, port, "");
        data = new SerializedData<>(kryo, cacheSize, expireAfter, unit, this::set, this::get, this::remove);
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
}
