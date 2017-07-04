package gabrielbot.utils.commands;

import gabrielbot.utils.http.HTTPRequester;
import org.json.JSONObject;

public class Jokes {
    private static final HTTPRequester REQUESTER = new HTTPRequester("Jokes");

    public static String getJoke(String user) {
        try {
            JSONObject object = REQUESTER.newRequest("http://api.icndb.com/jokes/random").get().asObject();

            if (!"success".equals(object.getString("type"))) {
                throw new RuntimeException("Couldn't gather joke ;|");
            }

            String joke = object.getJSONObject("value").getString("joke").replace("&quot;", "\"");
            if (user == null || user.equals("Chuck Norris")) return joke;
            return joke.replace("Chuck Norris", user);
        } catch (Exception ex) {
            return "Unable to get joke :(";
        }
    }
}
