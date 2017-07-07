package gabrielbot.utils.http;

import gabrielbot.utils.RateLimiter;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.impl.MessageEmbedImpl;
import net.dv8tion.jda.core.entities.impl.MessageImpl;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

public class Webhook {
    public static final String API_ENDPOINT = "https://canary.discordapp.com/api/webhooks/%s/%s"; //id and token
    public static final Requester REQUESTER = new Requester();

    private final String id;
    private final String token;
    private String avatarUrl;
    private String username;

    public Webhook(String id, String token) {
        this.id = id;
        this.token = token;
    }

    public Webhook setUsername(String username) {
        this.username = username;
        return this;
    }

    public Webhook setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        return this;
    }

    public Response rawPost(JSONObject message) throws RequestingException {
        if(avatarUrl != null && !message.has("avatar_url")) message.put("avatar_url", avatarUrl);
        if(username != null && !message.has("username")) message.put("username", username);
        return REQUESTER.post(id, token, message);
    }

    public Response post(Message message) throws RequestingException {
        return rawPost(((MessageImpl)message).toJSONObject());
    }

    public Response post(MessageEmbed... embeds) throws RequestingException {
        JSONObject object = new JSONObject();
        JSONArray array = new JSONArray();
        for(MessageEmbed embed : embeds) {
            array.put(((MessageEmbedImpl)embed).toJSONObject());
        }
        object.put("embeds", array);
        return rawPost(object);
    }

    public Response post(String message, boolean tts) throws RequestingException {
        return post(new MessageBuilder().append(message).setTTS(tts).build());
    }

    public Response post(String message) throws RequestingException {
        return post(new MessageBuilder().append(message).build());
    }

    public static final class Requester extends HTTPRequester {
        private boolean configured = false;

        Requester() {
            super("WebhookRequester", new RateLimiter(4, 5000), builder->
                builder.readTimeout(10, TimeUnit.SECONDS).connectTimeout(10, TimeUnit.SECONDS).writeTimeout(10, TimeUnit.SECONDS).build()
            );
        }

        public Response post(String id, String token, JSONObject message) throws RequestingException {
            Request req = newRequest(String.format(API_ENDPOINT, id, token), id).body(message).header("Content-Type", "application/json");
            Response res;
            int i = 0;
            do {
                i++;
                res = req.post();
                if(res.code() == 429) {
                    long tryAgainIn = res.asObject().getInt("retry_after");
                    if(!onRateLimited(req, tryAgainIn)) {
                        throw new RateLimitedException(tryAgainIn);
                    }
                }
            } while(res.code() != 204 && i < 4);

            if(!configured && res.code() == 204) {
                int limit = Integer.parseInt(res.headers().get("x-ratelimit-limit").get(0));
                int remaining = Integer.parseInt(res.headers().get("x-ratelimit-remaining").get(0));
                if(remaining == limit-1) {
                    long time = (Long.parseLong(res.headers().get("x-ratelimit-reset").get(0))- OffsetDateTime.now().toEpochSecond())/10;
                    setRateLimiter(new RateLimiter(remaining, (int)(time*1000)));
                    configured = true;
                }
            }
            return res;
        }
    }
}
