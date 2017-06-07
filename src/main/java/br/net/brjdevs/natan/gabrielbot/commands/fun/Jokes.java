package br.net.brjdevs.natan.gabrielbot.commands.fun;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;

public class Jokes {
    public static String getJoke(String user) {
        try {
            JSONObject object = Unirest.get("http://api.icndb.com/jokes/random").asJson().getBody().getObject();

            if (!"success".equals(object.getString("type"))) {
                throw new RuntimeException("Couldn't gather joke ;|");
            }

            String joke = object.getJSONObject("value").getString("joke").replace("&quot;", "\"");
            if (user == null || user.equals("Chuck Norris")) return joke;
            return joke.replace("Chuck Norris", user);
        } catch (UnirestException ex) {
            return "Unable to get joke :(";
        }
    }
}
