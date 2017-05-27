package br.net.brjdevs.natan.gabrielbot.utils.cache;

import br.net.brjdevs.natan.gabrielbot.utils.Utils;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class FileCache {
    public static final int DEFAULT_MAX_SIZE = 20;

    private final LoadingCache<String, byte[]> cache;

    public FileCache(long maxSize) {
        cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .build(key->{
                    File f = new File(key);
                    if(!f.isFile()) throw new IllegalArgumentException(f + ": Not a file");
                    if(!f.canRead()) throw new IllegalArgumentException(f + ": Can't read");
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try(FileInputStream fis = new FileInputStream(f)) {
                        Utils.copyData(fis, baos);
                    }
                    return baos.toByteArray();
                });
    }

    public FileCache() {
        this(DEFAULT_MAX_SIZE);
    }

    public InputStream input(String file) {
        return new ByteArrayInputStream(get(file, false));
    }

    public byte[] get(String file) {
        return get(file, true);
    }

    private byte[] get(String file, boolean copy) {
        byte[] b = cache.get(file);
        return copy ? b.clone() : b;
    }
}
