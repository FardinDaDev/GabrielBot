package gabrielbot.utils.http;

import java.io.IOException;

public class RequestingException extends IOException {
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
