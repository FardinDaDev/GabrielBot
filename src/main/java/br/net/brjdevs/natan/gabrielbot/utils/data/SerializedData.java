package br.net.brjdevs.natan.gabrielbot.utils.data;

import com.esotericsoftware.kryo.pool.KryoPool;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static br.net.brjdevs.natan.gabrielbot.utils.KryoUtils.*;

public class SerializedData<T> {
    private final Function<String, String> get;
    private final KryoPool kryoPool;
    private final BiConsumer<String, String> set;
    private final Map<String, T> data = new ConcurrentHashMap<>();

    public SerializedData(KryoPool kryoPool, BiConsumer<String, String> set, Function<String, String> get) {
        this.kryoPool = kryoPool == null ? POOL : kryoPool;
        this.set = checkNotNull(set, "set");
        this.get = checkNotNull(get, "get");
    }

    @SuppressWarnings("unchecked")
    public T get(String key) {
        String value = get.apply(key);
        if (value == null) return null;
        return data.computeIfAbsent(key, (k)->(T)unserialize(kryoPool, Base64.getDecoder().decode(value)));
    }

    public String getString(String key) {
        return get.apply(key);
    }

    public void set(String key, T object) {
        data.put(key, object);
    }

    public void setString(String key, String value) {
        set.accept(key, value);
    }

    public void save() {
        data.forEach((key, value)->{
            set.accept(key, Base64.getEncoder().encodeToString(serialize(kryoPool, value)));
        });
    }
}
