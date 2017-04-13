package br.net.brjdevs.natan.gabrielbot.core.data;

import br.net.brjdevs.natan.gabrielbot.utils.UnsafeUtils;
import br.net.brjdevs.natan.gabrielbot.utils.data.DataManager;
import br.net.brjdevs.natan.gabrielbot.utils.data.JedisSerializatorDataManager;
import br.net.brjdevs.natan.gabrielbot.utils.data.SerializedData;

import java.io.File;
import java.io.IOException;

public class GabrielData {
    private static DataManager<SerializedData<ChannelData>> channels;
    private static DataManager<SerializedData<GuildData>> guilds;
    private static Config config;

    public static DataManager<SerializedData<ChannelData>> channels() {
        if(channels == null) {
            Config.DBInfo info = config().dbs.get("channels");
            if(info == null) throw new UnsupportedOperationException("No db info specified in config file");
            channels = new JedisSerializatorDataManager<>(info.host, info.port, "channels:");
        }
        return channels;
    }

    public static DataManager<SerializedData<GuildData>> guilds() {
        if(guilds == null) {
            Config.DBInfo info = config().dbs.get("guilds");
            if(info == null) throw new UnsupportedOperationException("No db info specified in config file");
            guilds = new JedisSerializatorDataManager<>(info.host, info.port, "guilds:");
        }
        return guilds;
    }

    public static Config config() {
        if(config == null) {
            try {
                config = Config.load(new File("config.json"));
            } catch(IOException e) {
                UnsafeUtils.throwException(e);
            }
        }
        return config;
    }

    public static void save() {
        if(channels != null) channels.save();
        if(guilds != null) guilds.save();
    }

    public static class ChannelData {
        public boolean nsfw = false;
    }

    public static class GuildData {
        public String prefix = ">>";
    }
}
