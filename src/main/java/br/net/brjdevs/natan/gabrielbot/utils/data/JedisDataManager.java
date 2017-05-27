package br.net.brjdevs.natan.gabrielbot.utils.data;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class JedisDataManager extends AbstractMap<String, String> {
    private static Map<String, JedisPool> pools = new ConcurrentHashMap<>();

    private final JedisPool pool;
    private final String prefix;
    private volatile Set<Entry<String, String>> set;

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

    @Override
    @Nonnull
    public Set<Entry<String, String>> entrySet() {
        if(set == null) {
            synchronized(this) {
                if(set == null) {
                    set = new AbstractSet<Entry<String, String>>() {
                        @Override
                        @Nonnull
                        public Iterator<Entry<String, String>> iterator() {
                            Iterator<String> keys = run(j->{return j.keys("*");}).iterator();
                            return new Iterator<Entry<String, String>>() {
                                @Override
                                public boolean hasNext() {
                                    return keys.hasNext();
                                }

                                @Override
                                public Entry<String, String> next() {
                                    String s = keys.next();
                                    return new LazyLoadingMapEntry(s,
                                            ()->run(j->{return j.get(s);}),
                                            (v)->run(j->{if(v == null) {j.del(s);} else {j.set(s, v);}})
                                    );
                                }
                            };
                        }

                        @Override
                        public int size() {
                            return run(j->{return j.keys("*").size();});
                        }
                    };
                }
            }
        }
        return set;
    }

    private static class LazyLoadingMapEntry implements Map.Entry<String, String> {
        private final String key;
        private final Supplier<String> get;
        private final Consumer<String> set;
        private volatile String cachedValue;

        LazyLoadingMapEntry(String key, Supplier<String> get, Consumer<String> set) {
            this.key = key;
            this.get = get;
            this.set = set;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getValue() {
            return cachedValue == null ? cachedValue = get.get() : cachedValue;
        }

        @Override
        public String setValue(String value) {
            String s = get.get();
            set.accept(value);
            cachedValue = value;
            return s;
        }
    }
}
