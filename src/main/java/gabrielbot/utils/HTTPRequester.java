package gabrielbot.utils;

import com.mashape.unirest.http.Unirest;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HTTPRequester {
    public static final HTTPRequester DEFAULT = new HTTPRequester("Default");

    protected final String identifier;
    private RateLimiter rateLimiter;

    public HTTPRequester(String identifier) {
        this(identifier, null);
    }

    public HTTPRequester(String identifier, RateLimiter rateLimiter) {
        this.identifier = identifier;
        this.rateLimiter = rateLimiter;
    }

    public void shutdown() {
        try {
            Unirest.shutdown();
        } catch(IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Request newRequest(String url) {
        return newRequest(url, "no-key");
    }

    public Request newRequest(String url, String rateLimitKey) {
        return new Request(this, url, rateLimitKey);
    }

    protected long processRateLimit(String rateLimitKey) {
        if(rateLimiter == null) return -1;
        if(rateLimiter.process(rateLimitKey)) return -1;
        return rateLimiter.tryAgainIn(rateLimitKey);
    }

    protected Response get(Request request) throws RequestingException {
        long l = processRateLimit(request.rateLimitKey);
        if(l != -1 && !onRateLimited(request, l)) throw new RateLimitedException(l);
        try {
            InputStream is = Unirest.get(request.url)
                    .headers(request.headers)
                    .asBinary()
                    .getRawBody();
            return new Response(IOUtils.readFully(is, is.available()));
        } catch(Exception e) {
            throw new RequestingException(request, identifier, e);
        }
    }

    protected Response post(Request request) throws RequestingException {
        long l = processRateLimit(request.rateLimitKey);
        if(l != -1 && !onRateLimited(request, l)) throw new RateLimitedException(l);
        try {
            InputStream is = Unirest.post(request.url)
                    .headers(request.headers)
                    .body(request.body)
                    .asBinary()
                    .getRawBody();
            return new Response(is == null ? new byte[0] : IOUtils.readFully(is, is.available()));
        } catch(Exception e) {
            throw new RequestingException(request, identifier, e);
        }
    }

    protected boolean onRateLimited(Request request, long tryAgainIn) {
        try {
            Thread.sleep(tryAgainIn);
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return true;
    }

    protected void setRateLimiter(RateLimiter limiter) {
        this.rateLimiter = limiter;
    }

    public static class Request {
        private final Map<String, String> headers = new HashMap<>();
        private final HTTPRequester requester;
        private final String url;
        private final String rateLimitKey;
        private byte[] body;

        public Request(HTTPRequester requester, String url, String rateLimitKey) {
            this.requester = requester;
            this.url = url;
            this.rateLimitKey = rateLimitKey;
        }

        public Request header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public Request body(byte[] data) {
            this.body = data;
            return this;
        }

        public Request body(String data) {
            this.body = data.getBytes(StandardCharsets.UTF_8);
            return this;
        }

        public Request body(JSONObject data) {
            return body(data.toString());
        }

        public Request body(JSONArray data) {
            return body(data.toString());
        }

        public Response get() throws RequestingException {
            return requester.get(this);
        }

        public Response post() throws RequestingException {
            return requester.post(this);
        }

        public HTTPRequester getRequester() {
            return requester;
        }
    }

    public static class Response {
        private final byte[] data;

        public Response(byte[] data) {
            this.data = data;
        }

        public InputStream asStream() {
            return new ByteArrayInputStream(data);
        }

        public String asString(Charset charset) {
            return new String(data, charset);
        }

        public String asString() {
            return asString(StandardCharsets.UTF_8);
        }

        public JSONObject asObject() {
            return new JSONObject(asString());
        }

        public JSONArray asArray() {
            return new JSONArray(asString());
        }
    }

    public static class RateLimitedException extends RuntimeException {
        private final long tryAgainIn;

        public RateLimitedException(long tryAgainIn) {
            this.tryAgainIn = tryAgainIn;
        }

        public long getTryAgainIn() {
            return tryAgainIn;
        }
    }

    public static class RequestingException extends IOException {
        private final Request request;
        private final String requesterIdentifier;

        public RequestingException(Request request, String requesterIdentifier, Throwable cause) {
            super(cause);
            this.request = request;
            this.requesterIdentifier = requesterIdentifier;
        }

        public Request getRequest() {
            return request;
        }

        public String getRequesterIdentifier() {
            return requesterIdentifier;
        }
    }
}
