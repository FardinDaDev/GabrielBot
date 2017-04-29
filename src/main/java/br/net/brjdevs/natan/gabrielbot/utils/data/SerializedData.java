package br.net.brjdevs.natan.gabrielbot.utils.data;

import com.esotericsoftware.kryo.pool.KryoPool;
import com.github.benmanes.caffeine.cache.*;

import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static br.net.brjdevs.natan.gabrielbot.utils.KryoUtils.*;

public class SerializedData<T> {
    private final Function<String, String> get;
    private final KryoPool kryoPool;
    private final Consumer<String> remove;
    private final BiConsumer<String, String> set;
    private final LoadingCache<String, Optional<T>> data;

    @SuppressWarnings("unchecked")
    public SerializedData(KryoPool kryoPool, int cacheSize, int expireTime, TimeUnit expireUnit, BiConsumer<String, String> set, Function<String, String> get, Consumer<String> remove) {
        this.kryoPool = kryoPool == null ? POOL : kryoPool;
        this.set = checkNotNull(set, "set");
        this.get = checkNotNull(get, "get");
        this.remove = checkNotNull(remove, "remove");
        this.data = Caffeine.newBuilder()
                .maximumSize(cacheSize)
                .initialCapacity(Math.min(cacheSize/10, 1))
                .expireAfterAccess(expireTime, expireUnit)
                .removalListener((key, value, cause)->{
                    switch(cause) {
                        case REPLACED:
                        case EXPLICIT:
                        case COLLECTED:
                            return;
                    }
                    Optional optional = (Optional)value;
                    if(!optional.isPresent()) return;
                    set.accept((String)key, Base64.getEncoder().encodeToString(serialize(kryoPool, optional.get())));
                })
                .build((key)->{
                    String value = get.apply(key);
                    if(value == null) return Optional.empty();
                    return Optional.of((T)unserialize(kryoPool, Base64.getDecoder().decode(value)));
                });
    }

    public T get(String key) {
        return data.get(key).orElse(null);
    }

    public String getString(String key) {
        return get.apply(key);
    }

    public void set(String key, T object) {
        data.put(key, Optional.of(object));
    }

    public void setString(String key, String value) {
        set.accept(key, value);
    }

    public void remove(String key) {
        data.invalidate(key);
        remove.accept(key);
    }

    public void save() {
        data.asMap().forEach((key, value)->{
            value.ifPresent(v->set.accept(key, Base64.getEncoder().encodeToString(serialize(kryoPool, v))));
        });
    }
}
