package br.net.brjdevs.natan.gabrielbot.core.data;

import br.net.brjdevs.natan.gabrielbot.commands.custom.CustomCommand;
import br.net.brjdevs.natan.gabrielbot.core.data.serializers.Since;
import br.net.brjdevs.natan.gabrielbot.core.data.serializers.Version;
import br.net.brjdevs.natan.gabrielbot.core.data.serializers.VersionSerializer;
import br.net.brjdevs.natan.gabrielbot.utils.UnsafeUtils;
import br.net.brjdevs.natan.gabrielbot.utils.data.DataManager;
import br.net.brjdevs.natan.gabrielbot.utils.data.JedisDataManager;
import br.net.brjdevs.natan.gabrielbot.utils.data.JedisSerializatorDataManager;
import br.net.brjdevs.natan.gabrielbot.utils.data.SerializedData;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoPool;
import gnu.trove.set.TLongSet;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static java.util.concurrent.TimeUnit.*;

public final class GabrielData {
    public static final VersionSerializer<GuildData> GUILD_SERIALIZER = new VersionSerializer<>(GuildData.class);
    public static final VersionSerializer<ChannelData> CHANNEL_SERIALIZER = new VersionSerializer<>(ChannelData.class);
    public static final VersionSerializer<UserData> USER_SERIALIZER = new VersionSerializer<>(UserData.class);
    public static final VersionSerializer<GuildCommandData> GUILD_COMMAND_SERIALIZER = new VersionSerializer<>(GuildCommandData.class);

    @SuppressWarnings("unchecked")
    private static final KryoPool POOL = new KryoPool.Builder(()->{
        Kryo k = new Kryo();
        Kryo.DefaultInstantiatorStrategy strategy = new Kryo.DefaultInstantiatorStrategy();
        strategy.setFallbackInstantiatorStrategy(new StdInstantiatorStrategy());
        k.setInstantiatorStrategy(strategy);
        k.addDefaultSerializer(GuildData.class, GUILD_SERIALIZER);
        k.addDefaultSerializer(ChannelData.class, CHANNEL_SERIALIZER);
        k.addDefaultSerializer(UserData.class, USER_SERIALIZER);
        k.addDefaultSerializer(GuildCommandData.class, GUILD_COMMAND_SERIALIZER);
        return k;
    }).build();

    private static DataManager<SerializedData<ChannelData>> channels;
    private static DataManager<SerializedData<GuildData>> guilds;
    private static DataManager<SerializedData<GuildCommandData>> guildCommands;
    private static DataManager<SerializedData<UserData>> users;
    private static JedisDataManager blacklist;
    private static Config config;

    private GabrielData(){}

    public static DataManager<SerializedData<ChannelData>> channels() {
        if(channels == null) {
            Config.DBInfo info = config().dbs.get("main");
            if(info == null) throw new UnsupportedOperationException("No db info specified in config file");
            channels = new JedisSerializatorDataManager<>(info.host, info.port, "channel:", POOL, 2000, 20, MINUTES);
        }
        return channels;
    }

    public static DataManager<SerializedData<GuildData>> guilds() {
        if(guilds == null) {
            Config.DBInfo info = config().dbs.get("main");
            if(info == null) throw new UnsupportedOperationException("No db info specified in config file");
            guilds = new JedisSerializatorDataManager<>(info.host, info.port, "guild:", POOL, 100, 5, MINUTES);
        }
        return guilds;
    }

    public static DataManager<SerializedData<GuildCommandData>> guildCommands() {
        if(guilds == null) {
            Config.DBInfo info = config().dbs.get("main");
            if(info == null) throw new UnsupportedOperationException("No db info specified in config file");
            guildCommands = new JedisSerializatorDataManager<>(info.host, info.port, "guildccs:", POOL, 100, 5, MINUTES);
        }
        return guildCommands;
    }

    public static DataManager<SerializedData<UserData>> users() {
        if(users == null) {
            Config.DBInfo info = config().dbs.get("main");
            if(info == null) throw new UnsupportedOperationException("No db info specified in config file");
            users = new JedisSerializatorDataManager<>(info.host, info.port, "user:", POOL, 1000, 20, MINUTES);
        }
        return users;
    }

    public static JedisDataManager blacklist() {
        if(blacklist == null) {
            Config.DBInfo info = config().dbs.get("main");
            if(info == null) throw new UnsupportedOperationException("No db info specified in config file");
            blacklist = new JedisDataManager(info.host, info.port, "blacklist:");
        }
        return blacklist;
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
        if(blacklist != null) blacklist.save();
        if(users != null) users.save();
        if(guildCommands != null) guildCommands.save();
    }

    @Version(0)
    public static class ChannelData {
        @Since(0)
        public boolean nsfw = false;
    }

    @Version(0)
    public static class GuildCommandData {
        @Since(0)
        public String prefix = null;
        @Since(0)
        public Set<String> custom = new CopyOnWriteArraySet<>();
    }

    @Version(2)
    public static class GuildData {
        @Since(0)
        public Map<String, CustomCommand> customCommands = new ConcurrentHashMap<>();
        @Since(0)
        public long premiumUntil = 0;
        @Since(1)
        public long starboardChannelId;
        @Since(2)
        public int minStars = 1;
        @Since(2)
        public TLongSet starboardBlacklist;
    }

    @Version(0)
    public static class UserData {
        @Since(0)
        public long premiumUntil = 0;
        @Since(0)
        public long money = 0;
    }
}
