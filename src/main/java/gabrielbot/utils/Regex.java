package gabrielbot.utils;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.regex.Pattern;

public class Regex {
    private static final LoadingCache<String, Pattern> CACHED_PATTERNS = Caffeine.newBuilder()
            .maximumSize(40)
            .build(Pattern::compile);

    public static Pattern pattern(String regex) {
        return CACHED_PATTERNS.get(regex);
    }
}
