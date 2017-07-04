package gabrielbot.core.jda;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import gabrielbot.GabrielBot;
import gabrielbot.core.data.GabrielData;
import gabrielbot.core.listeners.MainListener;
import gabrielbot.core.listeners.ReactListener;
import gabrielbot.core.listeners.StarboardListener;
import gabrielbot.core.listeners.operations.InteractiveOperations;
import gabrielbot.core.listeners.operations.ReactionOperations;
import gabrielbot.music.GuildMusicPlayer;
import gabrielbot.music.MusicListener;
import gabrielbot.utils.HTTPRequester;
import gnu.trove.impl.sync.TSynchronizedLongObjectMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.audio.factory.DefaultSendFactory;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.managers.AudioManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Shard {
    private static final HTTPRequester REQUESTER = new HTTPRequester("Shard Count Updater");

    private final Logger logger;
    private final EventManager eventManager;
    private final JDABuilder builder;
    private final TLongObjectMap<GuildMusicPlayer> players = new TSynchronizedLongObjectMap<>(new TLongObjectHashMap<>());
    private JDA jda;

    public Shard(String token, int shardId, int totalShards, boolean nas) throws LoginException, InterruptedException, RateLimitedException {
        logger = LoggerFactory.getLogger("Shard " + shardId);
        this.eventManager = new EventManager(shardId);
        this.builder = new JDABuilder(AccountType.BOT)
                .setToken(token)
                .setWebSocketTimeout(10000)
                .setAudioEnabled(GabrielData.config().music)
                .setAudioSendFactory(nas ? new NativeAudioSendFactory() : new DefaultSendFactory())
                .setAutoReconnect(true)
                .setCorePoolSize(10)
                .setEventManager(eventManager)
                .setIdle(true)
                .addEventListener(new MainListener(), new ReactListener(), new MusicListener(), new StarboardListener(), InteractiveOperations.listener(), ReactionOperations.listener())
                .setGame(Game.of("Loading..."));
        if(totalShards > 1) {
            builder.useSharding(shardId, totalShards);
        }
        startJDA();
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public void startJDA() throws LoginException, InterruptedException, RateLimitedException {
        jda = builder.buildBlocking();
    }

    public void restartJDA(boolean wait) throws LoginException, InterruptedException, RateLimitedException {
        jda.shutdown(false);
        if(wait) {
            while(jda.getStatus() != JDA.Status.SHUTDOWN) {
                Thread.yield();
            }
        }
        startJDA();
    }

    public void postStats() {
        if(GabrielBot.DEBUG) return;

        JSONObject payload = new JSONObject().put("server_count", jda.getGuilds().size());
        JDA.ShardInfo info = jda.getShardInfo();
        if(info != null) {
            payload.put("shard_id", info.getShardId()).put("shard_count", info.getShardTotal());
        }

        dbots: {
            String token = GabrielData.config().dbotsToken;
            if(token == null || token.isEmpty()) break dbots;
            try {
               REQUESTER.newRequest("https://discordbots.org/api/bots/" + jda.getSelfUser().getId() + "/stats")
                        .header("Authorization", token)
                        .header("Content-Type", "application/json")
                        .body(payload)
                        .post();
            } catch(Exception e) {
                logger.error("Error posting stats to discordbots.org", e.getCause());
            }
        }

        botspw: {
            String token = GabrielData.config().botsPwToken;
            if(token == null || token.isEmpty()) break botspw;
            try {
                REQUESTER.newRequest("https://bots.discord.pw/api/bots/" + jda.getSelfUser().getId() + "/stats")
                        .header("Authorization", token)
                        .header("Content-Type", "application/json")
                        .body(payload)
                        .post();;
            } catch(Exception e) {
                logger.error("Error posting stats to bots.discord.pw", e.getCause());
            }
        }
    }

    public JDA getJDA() {
        return jda;
    }

    public GuildMusicPlayer getPlayer(long guildId) {
        return players.get(guildId);
    }

    public GuildMusicPlayer createPlayer(long guildId, long textChannelId, long voiceChannelId) {
        GuildMusicPlayer gmp = getPlayer(guildId);
        if(gmp == null) {
            synchronized(this) {
                if(gmp == null) {
                    gmp = new GuildMusicPlayer(guildId, textChannelId, voiceChannelId);
                    AudioManager manager = jda.getGuildById(guildId).getAudioManager();
                    manager.setSendingHandler(gmp);
                    players.put(guildId, gmp);
                }
            }
        }
        return gmp;
    }

    public GuildMusicPlayer removePlayer(long guildId) {
        return players.remove(guildId);
    }

    public GuildMusicPlayer interruptPlayer(long guildId) {
        GuildMusicPlayer g = players.remove(guildId);
        if(g == null) return null;
        g.leave();
        return g;
    }

    public TLongObjectMap<GuildMusicPlayer> players() {
        return players;
    }
}
