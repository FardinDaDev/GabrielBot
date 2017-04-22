package br.net.brjdevs.natan.gabrielbot.utils.cache;

import br.net.brjdevs.natan.gabrielbot.utils.Utils;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.io.*;
import java.util.concurrent.ExecutionException;

public class FileCache {
    public static final int DEFAULT_CONCURRENCY_LEVEL = 5;
    public static final int DEFAULT_MAX_SIZE = 20;

    private final LoadingCache<String, byte[]> cache;

    public FileCache(long maxSize, int concurrencyLevel) {
        cache = CacheBuilder.newBuilder()
                .maximumSize(maxSize)
                .concurrencyLevel(concurrencyLevel)
                .build(new CacheLoader<String, byte[]>() {
                    @Override
                    public byte[] load(String key) throws Exception {
                        File f = new File(key);
                        if(!f.isFile()) throw new IllegalArgumentException(f + ": Not a file");
                        if(!f.canRead()) throw new IllegalArgumentException(f + ": Can't read");
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        try(FileInputStream fis = new FileInputStream(f)) {
                            Utils.copyData(fis, baos);
                        }
                        return baos.toByteArray();
                    }
                });
    }

    public FileCache(long maxSize) {
        this(maxSize, DEFAULT_CONCURRENCY_LEVEL);
    }

    public FileCache() {
        this(DEFAULT_MAX_SIZE, DEFAULT_CONCURRENCY_LEVEL);
    }

    public InputStream input(String file) {
        return new ByteArrayInputStream(get(file, false));
    }

    public byte[] get(String file) {
        return get(file, true);
    }

    private byte[] get(String file, boolean copy) {
        try {
            byte[] b = cache.get(file);
            return copy ? b.clone() : b;
        } catch(ExecutionException e) {
            throw new UncheckedExecutionException(e.getCause());
        }
    }
}
