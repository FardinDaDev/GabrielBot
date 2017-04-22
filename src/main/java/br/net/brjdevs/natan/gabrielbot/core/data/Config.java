package br.net.brjdevs.natan.gabrielbot.core.data;

import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import br.net.brjdevs.natan.gabrielbot.utils.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class Config {
    public final String prefix;
    public final String token;
    public final boolean nas;
    public final long[] owners;
    public final Map<String, DBInfo> dbs;
    public final long console;

    private Config(String prefix, String token, boolean nas, long[] owners, Map<String, DBInfo> dbs, long console) {
        this.prefix = prefix;
        this.token = token;
        this.nas = nas;
        this.owners = owners;
        this.dbs = dbs;
        this.console = console;
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
        long console = obj.getLong("console");
        return new Config(prefix, token, nas, owners, dbs, console);
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
