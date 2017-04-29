package br.net.brjdevs.natan.gabrielbot.commands.custom.functions;

import br.net.brjdevs.natan.gabrielbot.utils.UnsafeUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

public class URLFunction extends JavaFunction {
    private static final Pattern multiSpacePattern = Pattern.compile("\\s+");

    @Override
    public String apply(String[] args) {
        return args.length == 0 ? "" : args.length == 1 ? encode(args[0], '+') : encode(args[0], args[1].isEmpty() ? '+' : args[1].toCharArray()[0]);
    }

    private static String encode(String text, char separator) {
        if(separator == '+') try {
            return URLEncoder.encode(text, "UTF-8");
        } catch(UnsupportedEncodingException e) {
            UnsafeUtils.throwException(e);
        }
        String[] parts = multiSpacePattern.split(text);
        for(int i = 0; i < parts.length; i++) {
            try {
                parts[i] = URLEncoder.encode(parts[i], "UTF-8");
            } catch(UnsupportedEncodingException e) {
                UnsafeUtils.throwException(e);
            }
        }
        StringBuilder sb = new StringBuilder(text.length());
        for(int i = 0; i < parts.length; i++) {
            if(i == 0) {
                sb.append(parts[i]);
            } else {
                sb.append(separator).append(parts[i]);
            }
        }
        return sb.toString();
    }
}
