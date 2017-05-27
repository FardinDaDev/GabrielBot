package br.net.brjdevs.natan.gabrielbot.utils.cache;

import br.net.brjdevs.natan.gabrielbot.utils.Utils;
import com.google.common.base.Preconditions;
import com.mashape.unirest.http.Unirest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public class URLCache extends FileCache {
    private final File dir;

    public URLCache(File dir, long maxSize) {
        super(maxSize);
        this.dir = Preconditions.checkNotNull(dir);
        if(!dir.exists()) dir.mkdirs();
        Preconditions.checkArgument(dir.isDirectory(), dir + ": Not a directory");
        Preconditions.checkArgument(dir.canWrite(), dir + ": Can't write");
    }

    public URLCache(File dir) {
        this(dir, DEFAULT_MAX_SIZE);
    }

    @Override
    public InputStream input(String url) {
        return super.input(ensureDownloaded(url));
    }

    @Override
    public byte[] get(String url) {
        return super.get(ensureDownloaded(url));
    }

    private String ensureDownloaded(String url) {
        File f = new File(dir, url.replace('/', '_').replace(':', '_'));
        if(f.isFile()) return f.getAbsolutePath();

        synchronized(URLCache.class) {
            if(f.exists()) return f.getAbsolutePath();
            try {
                f.createNewFile();
                try(InputStream is = Unirest.get(url).asBinary().getRawBody();
                    FileOutputStream fos = new FileOutputStream(f)) {
                    Utils.copyData(is, fos);
                }
            } catch(Exception e) {
                f.delete();
                throw new UncheckedIOException(new IOException(e));
            }
        }
        return f.getAbsolutePath();
    }
}
