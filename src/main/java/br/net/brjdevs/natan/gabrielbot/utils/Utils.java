package br.net.brjdevs.natan.gabrielbot.utils;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

    public static void copyData(InputStream from, OutputStream to) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = from.read(buffer)) != -1) to.write(buffer, 0, read);
    }
}
