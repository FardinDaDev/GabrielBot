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
    public String prefix = ">>";
    public String token = "<your-token-goes-here>";
    public boolean nas = true;
    public long consoleChannel = 0;
    public long[] owners = {};
    public Map<String, DBInfo> dbs = new HashMap<>();

    private Config(){}

    public static Config load(File from) throws IOException {
        Config cfg = new Config();
        if(!from.exists()) {
            String json = new JSONObject()
                    .put("prefix", cfg.prefix)
                    .put("token", cfg.token)
                    .put("nas", true)
                    .put("consoleChannel", cfg.consoleChannel)
                    .put("owners", new JSONArray())
                    .put("dbs", new JSONObject().put("<dbname>", new JSONObject().put("host", "<host>").put("port", "<port>")))
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
        cfg.prefix = obj.getString("prefix");
        cfg.token = obj.getString("token");
        cfg.nas = obj.getBoolean("nas");
        cfg.consoleChannel = obj.getLong("consoleChannel");
        JSONArray owners = obj.getJSONArray("owners");
        cfg.owners = new long[owners.length()];
        for(int i = 0; i < cfg.owners.length; i++) {
            cfg.owners[i] = owners.getLong(i);
        }
        JSONObject dbs = obj.getJSONObject("dbs");
        for(String name : dbs.keySet()) {
            JSONObject db = dbs.getJSONObject(name);
            cfg.dbs.put(name, new DBInfo(db.getString("host"), db.getInt("port")));
        }
        return cfg;
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
