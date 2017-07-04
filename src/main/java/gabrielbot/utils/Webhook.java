package gabrielbot.utils;

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

    public HTTPRequester.Response rawPost(JSONObject message) throws HTTPRequester.RequestingException {
        if(avatarUrl != null) message.put("avatar_url", avatarUrl);
        if(username != null) message.put("username", username);
        return REQUESTER.post(id, token, message);
    }

    public HTTPRequester.Response post(Message message) throws HTTPRequester.RequestingException {
        return rawPost(((MessageImpl)message).toJSONObject());
    }

    public HTTPRequester.Response post(MessageEmbed... embeds) throws HTTPRequester.RequestingException {
        JSONObject object = new JSONObject();
        JSONArray array = new JSONArray();
        for(MessageEmbed embed : embeds) {
            array.put(((MessageEmbedImpl)embed).toJSONObject());
        }
        object.put("embeds", array);
        return rawPost(object);
    }

    public HTTPRequester.Response post(String message, boolean tts) throws HTTPRequester.RequestingException {
        return post(new MessageBuilder().append(message).setTTS(tts).build());
    }

    public HTTPRequester.Response post(String message) throws HTTPRequester.RequestingException {
        return post(new MessageBuilder().append(message).build());
    }

    private static final class Requester extends HTTPRequester {
        Requester() {
            super("WebhookRequester", new RateLimiter(5, 5000));
        }

        Response post(String id, String token, JSONObject message) throws RequestingException {
            System.out.println(message);
            return newRequest(String.format(API_ENDPOINT, id, token), id).body(message).header("Content-Type", "application/json").post();
        }
    }
}
