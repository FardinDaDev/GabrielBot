package gabrielbot.utils.data;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JarTextFileDataManager implements DataManager<List<String>> {
    private final List<String> lines;

    public JarTextFileDataManager(String resourceName) {
        InputStream is = JarTextFileDataManager.class.getResourceAsStream(resourceName);
        if(is == null) throw new IllegalArgumentException("No resource named" + resourceName);
        List<String> l = new ArrayList<>();
        try {
            String s = IOUtils.toString(is, StandardCharsets.UTF_8);
            Collections.addAll(l, s.split("\r?\n"));
            l.removeIf(line->line.startsWith("//"));
        } catch(IOException e) {
            throw new AssertionError(e);
        }
        lines = Collections.unmodifiableList(l);
    }

    @Override
    public void save() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> get() {
        return lines;
    }
}
