package br.net.brjdevs.natan.gabrielbot;

import br.net.brjdevs.natan.gabrielbot.core.command.CommandRegistry;
import br.net.brjdevs.natan.gabrielbot.core.data.Config;
import br.net.brjdevs.natan.gabrielbot.core.data.GabrielData;
import br.net.brjdevs.natan.gabrielbot.core.jda.Shard;
import br.net.brjdevs.natan.gabrielbot.log.DebugPrintStream;
import br.net.brjdevs.natan.gabrielbot.log.DiscordLogBack;
import br.net.brjdevs.natan.gabrielbot.music.GuildMusicPlayer;
import br.net.brjdevs.natan.gabrielbot.music.SerializedPlayer;
import br.net.brjdevs.natan.gabrielbot.music.SerializedTrack;
import br.net.brjdevs.natan.gabrielbot.music.Track;
import br.net.brjdevs.natan.gabrielbot.utils.KryoUtils;
import br.net.brjdevs.natan.gabrielbot.utils.data.JedisDataManager;
import com.mashape.unirest.http.Unirest;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.utils.SimpleLog;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GabrielBot {
    public static final boolean DEBUG = System.getProperty("gabriel.debug", null) != null;
    public static final Logger LOGGER = LoggerFactory.getLogger(GabrielBot.class);

    private static GabrielBot instance;
    private static boolean loaded = false;

    public final CommandRegistry registry;
    public final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    private final Shard[] shards;

    private GabrielBot() throws Throwable {
        GabrielData.blacklist().runNoReply((j)->{
            LOGGER.info("Successfully established connection to database");
        }, (t)->{
            LOGGER.warn("Unable to connect to the database");
            LOGGER.info("Attempting to start database...");
            File dbstart = new File("dbstart.bat");
            if(!dbstart.isFile()) dbstart = new File("dbstart.sh");
            if(!dbstart.isFile()) {
                LOGGER.error("No dbstart.sh/dbstart.bat found, exiting");
                System.exit(-1);
            }
            try {
                Runtime.getRuntime().exec(dbstart.getAbsolutePath()).waitFor();
                Thread.sleep(5000);
                GabrielData.blacklist().runNoReply((j)->{
                    LOGGER.info("Successfully established connection to database");
                }, (e)->{
                    LOGGER.error("DB not started, exiting", e);
                    System.exit(-1);
                });
            } catch(IOException|InterruptedException e) {
                LOGGER.error("Error running dbstart file", e);
                System.exit(-1);
            }
        });

        AudioSourceManagers.registerRemoteSources(playerManager);

        Reflections r = new Reflections("br.net.brjdevs.natan.gabrielbot.commands", new SubTypesScanner(false));

        registry = new CommandRegistry(GabrielBot.class.getClassLoader());
        long l = System.currentTimeMillis();
        for(String name : r.getAllTypes()) {
            registry.register(Class.forName(name));
        }
        long ll = System.currentTimeMillis()-l;
        LOGGER.info("Registered {} commands in {} ms", registry.commands().size(), ll);

        Thread dataSaver = new Thread(()->{
            while(true) {
                try {
                    Thread.sleep(3_600_000);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
                GabrielData.save();
            }
        }, "DataSaverThread");
        dataSaver.setDaemon(true);
        dataSaver.start();

        Runtime.getRuntime().addShutdownHook(new Thread(GabrielData::save, "ShutdownHookSaverThread"));

        Config config = GabrielData.config();

        shards = new Shard[getRecommendedShards(config)];
        for(int i = 0; i < shards.length; i++) {
            if(i != 0) Thread.sleep(5000);
            shards[i] = new Shard(config.token, i, shards.length, config.nas);
        }
        DiscordLogBack.enable();
        playerManager.setItemLoaderThreadPoolSize(10);


        LOGGER.info("Loading done!");
        loaded = true;
    }

    private void trackSetup() {
        JedisDataManager manager = GabrielData.guilds();
        Set<String> keys = manager.keySet("p_*");
        LOGGER.info("Unserializing music tracks for {} guilds", keys.size());
        keys.forEach(key->{
            String value = manager.get(key);
            manager.remove(key);
            if(value == null) {
                LOGGER.error("Null serialized track data on key {}", key);
                return;
            }
            SerializedPlayer serializedPlayer = KryoUtils.unserialize(Base64.getDecoder().decode(value));
            List<Track> unserialized = Arrays.stream(serializedPlayer.tracks).map(SerializedTrack::toTrack).collect(Collectors.toList());
            Track playing = unserialized.remove(0);
            playing.track.setPosition(serializedPlayer.position);
            GuildMusicPlayer gmp = createPlayer(serializedPlayer.guildId, serializedPlayer.textChannelId, serializedPlayer.voiceChannelId);
            gmp.getGuild().getAudioManager().openAudioConnection(gmp.getVoiceChannel());
            gmp.scheduler.fromSerialized(playing, unserialized);
            gmp.getTextChannel().sendMessage("Successfully resumed music").queue();
        });
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public Shard[] getShards() {
        return shards.clone();
    }

    public Shard getShard(long guildId) {
        return shards[calculateShardId(guildId)];
    }

    public GuildMusicPlayer getPlayer(long guildId) {
        return shards[calculateShardId(guildId)].getPlayer(guildId);
    }

    public GuildMusicPlayer createPlayer(long guildId, long textChannelId, long voiceChannelId) {
        return shards[calculateShardId(guildId)].createPlayer(guildId, textChannelId, voiceChannelId);
    }

    public GuildMusicPlayer removePlayer(long guildId) {
        return shards[calculateShardId(guildId)].removePlayer(guildId);
    }

    public GuildMusicPlayer interruptPlayer(long guildId) {
        return shards[calculateShardId(guildId)].interruptPlayer(guildId);
    }

    public List<GuildMusicPlayer> players() {
        return streamPlayers().collect(Collectors.toList());
    }

    public Stream<GuildMusicPlayer> streamPlayers() {
        return Arrays.stream(shards).flatMap(s->s.players().stream());
    }

    public int calculateShardId(long guildId) {
        return (int) ((guildId >> 22) % shards.length);
    }

    public User getUserById(long id) {
        for(Shard s : shards) {
            User u = s.getJDA().getUserById(id);
            if(u != null) return u;
        }
        return null;
    }

    public Stream<User> streamUsers() {
        return Arrays.stream(shards).flatMap(s->s.getJDA().getUsers().stream()).distinct();
    }

    public List<User> getUsers() {
        return streamUsers().collect(Collectors.toList());
    }

    public TextChannel getTextChannelById(long id) {
        for(Shard s : shards) {
            TextChannel tc = s.getJDA().getTextChannelById(id);
            if(tc != null) return tc;
        }
        return null;
    }

    public Stream<TextChannel> streamTextChannels() {
        return Arrays.stream(shards).flatMap(s->s.getJDA().getTextChannels().stream());
    }

    public List<TextChannel> getTextChannels() {
        return streamTextChannels().collect(Collectors.toList());
    }

    public VoiceChannel getVoiceChannelById(long id) {
        for(Shard s : shards) {
            VoiceChannel vc = s.getJDA().getVoiceChannelById(id);
            if(vc != null) return vc;
        }
        return null;
    }

    public Stream<VoiceChannel> streamVoiceChannels() {
        return Arrays.stream(shards).flatMap(s->s.getJDA().getVoiceChannels().stream());
    }

    public List<VoiceChannel> getVoiceChannels() {
        return streamVoiceChannels().collect(Collectors.toList());
    }

    public Guild getGuildById(long id) {
        return shards[calculateShardId(id)].getJDA().getGuildById(id);
    }

    public Stream<Guild> streamGuilds() {
        return Arrays.stream(shards).flatMap(s->s.getJDA().getGuilds().stream());
    }

    public List<Guild> getGuilds() {
        return streamGuilds().collect(Collectors.toList());
    }

    public void log(String s) {
        System.out.println(s);
        TextChannel tc = getTextChannelById(GabrielData.config().console);
        if(tc == null) {
            return;
        }
        tc.sendMessage(s).queue();
    }

    public static void main(String... args) throws Throwable {
        if(DEBUG) {
            System.setOut(new DebugPrintStream(System.out));
            System.setErr(new DebugPrintStream(System.err));
        }
        SimpleLog.LEVEL = SimpleLog.Level.OFF;
        SimpleLog.addListener(new SimpleLog.LogListener() {
            @Override
            public void onLog(SimpleLog log, SimpleLog.Level logLevel, Object message) {
                Logger l = LoggerFactory.getLogger(log.name);
                switch(logLevel) {
                    case TRACE:
                        if (l.isTraceEnabled()) {
                            l.trace(message.toString());
                        }
                        break;
                    case DEBUG:
                        if (l.isDebugEnabled()) {
                            l.debug(message.toString());
                        }
                        break;
                    case INFO:
                        l.info(message.toString());
                        break;
                    case WARNING:
                        l.warn(message.toString());
                        break;
                    case FATAL:
                        l.error(message.toString());
                        break;
                }
            }

            @Override
            public void onError(SimpleLog log, Throwable err) {
                LoggerFactory.getLogger(log.name).error("Failure", err);
            }
        });
        try {
            instance = new GabrielBot();
            instance.trackSetup();
        } catch(Throwable t) {
            LOGGER.error("Error during startup", t);
            System.exit(-1);
        }
    }

    public static GabrielBot getInstance() {
        return instance;
    }

    private static int getRecommendedShards(Config config) {
        if (DEBUG) return 2;
        try {
            return Unirest.get("https://discordapp.com/api/gateway/bot")
                    .header("Authorization", "Bot " + config.token)
                    .header("Content-Type", "application/json")
                    .asJson()
                    .getBody().getObject().getInt("shards");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }
}
