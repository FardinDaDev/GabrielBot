package gabrielbot.utils;

import com.mashape.unirest.http.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class DBots {
    public static BotInfo[] byName(String name) {
        try {
            return query("username," + URLEncoder.encode(name, "UTF-8"));
        } catch(UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    public static BotInfo[] byId(long id) {
        return query("id," + id);
    }

    public static BotInfo[] byOwner(long id) {
        return query("owners," + id);
    }

    public static BotInfo[] byPrefix(String prefix) {
        try {
            return query("prefix," + URLEncoder.encode(prefix, "UTF-8"));
        } catch(UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    public static BotInfo[] query(String search) {
        try {
            JSONArray res = Unirest.get("https://discordbots.org/api/bots?search=" + search)
                    .asJson()
                    .getBody()
                    .getObject()
                    .getJSONArray("results");
            BotInfo[] bots = new BotInfo[res.length()];
            for(int i = 0; i < bots.length; i++) {
                JSONObject b = res.getJSONObject(i);
                bots[i] = new BotInfo(
                        b.getString("username"),
                        b.getString("discriminator"),
                        getAvatar(b),
                        b.optString("invite", ""),
                        b.getString("shortdesc"),
                        b.getString("prefix"),
                        b.getString("lib"),
                        Long.parseLong(b.getString("id")),
                        b.getInt("points"),
                        b.getBoolean("certifiedBot"),
                        toLongArray(b.getJSONArray("owners")),
                        b.optInt("shard_count", -1),
                        b.optInt("server_count", -1)
                );
            }
            return bots;
        } catch(Exception e) {
            e.printStackTrace();
            return new BotInfo[0];
        }
    }

    private static long[] toLongArray(JSONArray array) {
        long[] longs = new long[array.length()];
        for(int i = 0; i < longs.length; i++) {
            longs[i] = array.getLong(i);
        }
        return longs;
    }

    private static String getAvatar(JSONObject object) {
        return "https://images.discordapp.net/avatars/" + object.getString("id") + "/" + object.optString("avatar", object.getString("defAvatar")) + ".png?size=512";
    }

    public static class BotInfo {
        public final String name;
        public final String discriminator;
        public final String avatarUrl;
        public final String invite;
        public final String shortDesc;
        public final String prefix;
        public final String lib;
        public final long id;
        public final int upvotes;
        public final boolean certified;
        public final long[] owners;
        public final int shardCount;
        public final int guildCount;

        public BotInfo(String name, String discriminator, String avatarUrl, String invite, String shortDesc, String prefix, String lib, long id, int upvotes, boolean certified, long[] owners, int shardCount, int guildCount) {
            this.name = name;
            this.discriminator = discriminator;
            this.avatarUrl = avatarUrl;
            this.invite = invite.isEmpty() ? "https://discordapp.com/oauth2/authorize?client_id=" + id + "&scope=bot" : invite;
            this.shortDesc = shortDesc;
            this.prefix = prefix;
            this.lib = lib;
            this.id = id;
            this.upvotes = upvotes;
            this.certified = certified;
            this.owners = owners;
            this.shardCount = shardCount;
            this.guildCount = guildCount;
        }
    }
}
