package gabrielbot.utils.http;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Response {
    private final byte[] data;
    private final int code;
    private final Map<String, List<String>> headers;

    public Response(byte[] data, int code, Map<String, List<String>> headers) {
        this.data = data;
        this.code = code;
        this.headers = Collections.unmodifiableMap(headers);
    }

    public int code() {
        return code;
    }

    public Map<String, List<String>> headers() {
        return headers;
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
