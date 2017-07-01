package gabrielbot.utils;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Utils {
    private static final Logger LOGGER = LoggerFactory.getLogger("Utils");

    private Utils() {}

    public static String paste(String toSend) {
        try {
            String pasteToken = Unirest.post("https://hastebin.com/documents")
                    .header("User-Agent", "Gabriel")
                    .header("Content-Type", "text/plain")
                    .body(toSend)
                    .asJson()
                    .getBody()
                    .getObject()
                    .getString("key");
            return "https://hastebin.com/" + pasteToken;
        } catch (UnirestException e) {
            LOGGER.warn("Hastebin is being stupid, huh? Can't send or retrieve paste.", e);
            return "Gabriel threw ``" + e.getCause().getClass().getSimpleName() + "``" + " while trying to upload paste, check logs";
        }
    }

    public static String pasteIfLarger(String s, int chars) {
        if(s.length() > chars) return paste(s);
        return s;
    }

    public static void copyData(InputStream from, OutputStream to) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = from.read(buffer)) != -1) to.write(buffer, 0, read);
    }

    public static String getDuration(long time) {
        long hours = TimeUnit.MILLISECONDS.toHours(time) % TimeUnit.DAYS.toHours(1);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(time) % TimeUnit.HOURS.toMinutes(1);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(time) % TimeUnit.MINUTES.toSeconds(1);
        return (hours == 0 ? "" : hours + ":") +
                (minutes == 0 ? (hours == 0 ? "" : "0") + "0:" : minutes + ":") +
                (seconds == 0 ? "" : seconds < 10 ? "0" + seconds : seconds + "").replaceAll(":$", "");
    }

    @SuppressWarnings("unchecked")
    public static <K,V> Map<K,V> map(Object... mappings) {
        if(mappings.length % 2 == 1) throw new IllegalArgumentException("mappings.length must be even");
        Map<K, V> map = new HashMap<>();

        for(int i = 0; i < mappings.length; i += 2) {
            map.put((K)mappings[i], (V)mappings[i+1]);
        }

        return map;
    }

    public static String repeat(String sequence, int times) {
        StringBuilder sb = new StringBuilder(sequence.length() * times);
        for(int i = 0; i < times; i++) sb.append(sequence);
        return sb.toString();
    }

    public static String progressBar(double percentage, int width) {
        int filled = (int)(width*Math.min(Math.max(percentage, 0D), 1D));
        int empty = width-filled;

        return "▕" + repeat("█", filled) + repeat(" ", empty) + "▏";
    }
}
