package br.net.brjdevs.natan.gabrielbot;

import br.net.brjdevs.natan.gabrielbot.core.command.CommandRegistry;
import br.net.brjdevs.natan.gabrielbot.core.command.RegisterCommand;
import br.net.brjdevs.natan.gabrielbot.core.data.Config;
import br.net.brjdevs.natan.gabrielbot.core.data.GabrielData;
import br.net.brjdevs.natan.gabrielbot.core.jda.Shard;
import br.net.brjdevs.natan.gabrielbot.log.DebugPrintStream;
import br.net.brjdevs.natan.gabrielbot.log.DiscordLogBack;
import br.net.brjdevs.natan.gabrielbot.music.GuildMusicPlayer;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GabrielBot {
    public static final boolean DEBUG = System.getProperty("gabriel.debug", null) != null;
    public static final Logger LOGGER = LoggerFactory.getLogger(GabrielBot.class);

    private static GabrielBot instance;
    private static boolean loaded = false;

    public final CommandRegistry registry;
    public final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    private final Shard[] shards;

    private GabrielBot() throws Throwable {
        GabrielData.blacklist().run((j)->{
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
                GabrielData.blacklist().run((j)->{
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

        Reflections r = new Reflections("br.net.brjdevs.natan.gabrielbot.commands");

        registry = new CommandRegistry();
        long l = System.nanoTime();
        for(Class<?> cls : r.getTypesAnnotatedWith(RegisterCommand.Class.class)) {
            Object instance = null;
            for(Method m : cls.getMethods()) {
                if(m.getAnnotation(RegisterCommand.class) == null) continue;
                if(Modifier.isStatic(m.getModifiers())) {
                    m.invoke(null, registry);
                } else {
                    if(instance == null) try {
                        instance = m.getDeclaringClass().newInstance();
                    } catch(Exception e) {
                        LOGGER.error("Error instantiating a command class", e);
                        continue;
                    }
                    m.invoke(instance, registry);
                }
            }
        }
        long ll = System.nanoTime()-l;
        LOGGER.info("Registered {} commands in {} ns ({} ms)", registry.commands().size(), ll, ll/1_000_000);

        Thread t = new Thread(()->{
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
        t.setDaemon(true);
        t.start();

        Runtime.getRuntime().addShutdownHook(new Thread(GabrielData::save, "ShutdownHookSaverThread"));

        Config config = GabrielData.config();

        shards = new Shard[getRecommendedShards(config)];
        for(int i = 0; i < shards.length; i++) {
            if(i != 0) Thread.sleep(5000);
            shards[i] = new Shard(config.token, i, shards.length, config.nas);
        }
        DiscordLogBack.enable();
        LOGGER.info("Loading done!");
        loaded = true;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public Shard[] getShards() {
        return shards.clone();
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

    public int calculateShardId(long guildId) {
        return (int) ((guildId >> 22) % shards.length);
    }

    public User getUserById(long id) {
        return Arrays.stream(shards).map(s->s.getJDA().getUserById(id)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    public List<User> getUsers() {
        return Arrays.stream(shards).flatMap(s->s.getJDA().getUsers().stream()).distinct().collect(Collectors.toList());
    }

    public TextChannel getTextChannelById(long id) {
        return Arrays.stream(shards).map(s->s.getJDA().getTextChannelById(id)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    public List<TextChannel> getTextChannels() {
        return Arrays.stream(shards).flatMap(s->s.getJDA().getTextChannels().stream()).collect(Collectors.toList());
    }

    public VoiceChannel getVoiceChannelById(long id) {
        return Arrays.stream(shards).map(s->s.getJDA().getVoiceChannelById(id)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    public List<VoiceChannel> getVoiceChannels() {
        return Arrays.stream(shards).flatMap(s->s.getJDA().getVoiceChannels().stream()).collect(Collectors.toList());
    }

    public Guild getGuildById(long id) {
        return shards[calculateShardId(id)].getJDA().getGuildById(id);
    }

    public List<Guild> getGuilds() {
        return Arrays.stream(shards).flatMap(s->s.getJDA().getGuilds().stream()).collect(Collectors.toList());
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
        instance = new GabrielBot();
    }

    public static GabrielBot getInstance() {
        return instance;
    }

    private int getRecommendedShards(Config config) {
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
