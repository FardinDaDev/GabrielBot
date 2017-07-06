package gabrielbot.core.data;

import gabrielbot.GabrielBot;
import gabrielbot.utils.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class Config {
    public final String prefix;
    public final String token;
    public final String dbotsToken;
    public final String botsPwToken;
    public final String musicDisableReason;
    public final boolean music;
    public final boolean nas;
    public final long[] owners;
    public final Map<String, DBInfo> dbs;
    public final String consoleWebhookId;
    public final String consoleWebhookToken;

    private Config(String prefix, String token, String dbotsToken, String botsPwToken, String musicDisableReason, boolean music, boolean nas, long[] owners, Map<String, DBInfo> dbs, String consoleWebhookId, String consoleWebhookToken) {
        this.prefix = prefix;
        this.token = token;
        this.dbotsToken = dbotsToken;
        this.botsPwToken = botsPwToken;
        this.musicDisableReason = musicDisableReason;
        this.music = music;
        this.nas = nas;
        this.owners = owners;
        this.dbs = dbs;
        this.consoleWebhookId = consoleWebhookId;
        this.consoleWebhookToken = consoleWebhookToken;
    }

    public static Config load(File from) throws IOException {
        if(!from.exists()) {
            String json = new JSONObject()
                    .put("prefix", ">>")
                    .put("token", "<your-token-goes-here>")
                    .put("nas", true)
                    .put("owners", new JSONArray())
                    .put("dbs", new JSONObject()
                            .put("<dbname>", new JSONObject()
                                    .put("host", "<host>")
                                    .put("port", "<port>")
                            )
                    )
                    .put("commandlog", new JSONObject()
                            .put("useWebhook", false)
                            .put("channel", "<channel-id-here>")
                            .put("webhook", new JSONObject()
                                    .put("id", "<id>")
                                    .put("token", "<token>")
                            )
                    )
                    .put("console", new JSONObject()
                            .put("useWebhook", false)
                            .put("channel", "<channel-id-here>")
                            .put("webhook", new JSONObject()
                                    .put("id", "<id>")
                                    .put("token", "<token>")
                            )
                    )
                    .toString(4);
            FileOutputStream fos = new FileOutputStream(from);
            Utils.copyData(new ByteArrayInputStream(json.getBytes(Charset.defaultCharset())), fos);
            fos.close();
            GabrielBot.LOGGER.error("No config found, an empty one has been generated.");
            GabrielBot.LOGGER.error("Please fill it with valid data");
            System.exit(1);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try(FileInputStream fis = new FileInputStream(from)) {
            Utils.copyData(fis, baos);
        }
        JSONObject obj = new JSONObject(new String(baos.toByteArray(), Charset.defaultCharset()));
        String prefix = obj.getString("prefix");
        String token = obj.getString("token");
        String dbotsToken = obj.getString("dbotsToken");
        String botsPwToken = obj.getString("botsPwToken");
        String musicDisableReason = obj.optString("musicDisableReason", "Sorry, music is currently disabled");
        boolean music = obj.getBoolean("music");
        boolean nas = obj.getBoolean("nas");
        long[] owners;
        {
            JSONArray o = obj.getJSONArray("owners");
            owners = new long[o.length()];
            for(int i = 0; i < owners.length; i++) {
                owners[i] = o.getLong(i);
            }
        }
        Map<String, DBInfo> dbs = new HashMap<>();
        {
            JSONObject d = obj.getJSONObject("dbs");
            for(String name : d.keySet()) {
                JSONObject db = d.getJSONObject(name);
                dbs.put(name, new DBInfo(db.getString("host"), db.getInt("port")));
            }
        }
        String webhookId = obj.getString("consoleWebhookId");
        String webhookToken = obj.getString("consoleWebhookToken");
        return new Config(prefix, token, dbotsToken, botsPwToken, musicDisableReason, music, nas, owners, dbs, webhookId, webhookToken);
    }

    public static class DBInfo {
        public final String host;
        public final int port;

        private DBInfo(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }
}
