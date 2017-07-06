package gabrielbot.utils.http;

import gabrielbot.utils.RateLimiter;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.impl.MessageEmbedImpl;
import net.dv8tion.jda.core.entities.impl.MessageImpl;
import org.json.JSONArray;
import org.json.JSONObject;

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

    private static final class Requester extends HTTPRequester {
        Requester() {
            super("WebhookRequester", new RateLimiter(5, 5000));
        }

        public Response post(String id, String token, JSONObject message) throws RequestingException {
            return newRequest(String.format(API_ENDPOINT, id, token), id).body(message).header("Content-Type", "application/json").post();
        }
    }
}
