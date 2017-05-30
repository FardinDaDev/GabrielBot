package br.net.brjdevs.natan.gabrielbot.core.jda;

import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import br.net.brjdevs.natan.gabrielbot.core.data.GabrielData;
import br.net.brjdevs.natan.gabrielbot.core.listeners.MainListener;
import br.net.brjdevs.natan.gabrielbot.core.listeners.StarboardListener;
import br.net.brjdevs.natan.gabrielbot.core.listeners.operations.InteractiveOperations;
import br.net.brjdevs.natan.gabrielbot.core.listeners.operations.ReactionOperations;
import br.net.brjdevs.natan.gabrielbot.music.GuildMusicPlayer;
import br.net.brjdevs.natan.gabrielbot.music.MusicListener;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.audio.factory.DefaultSendFactory;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.managers.AudioManager;

import javax.security.auth.login.LoginException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Shard {
    private final Logger logger;
    private final JDABuilder builder;
    private final List<GuildMusicPlayer> players = new CopyOnWriteArrayList<>();
    private JDA jda;

    public Shard(String token, int shardId, int totalShards, boolean nas) throws LoginException, InterruptedException, RateLimitedException {
        logger = LoggerFactory.getLogger("Shard " + shardId);
        builder = new JDABuilder(AccountType.BOT)
                .setToken(token)
                .setWebSocketTimeout(10000)
                .setAudioEnabled(true)
                .setAudioSendFactory(nas ? new NativeAudioSendFactory() : new DefaultSendFactory())
                .setAutoReconnect(true)
                .setCorePoolSize(10)
                .setEventManager(new EventManager(shardId))
                .addEventListener(new MainListener(), new MusicListener(), new StarboardListener(), InteractiveOperations.listener(), ReactionOperations.listener())
                .setGame(Game.of(GabrielData.config().prefix + "help"));
        if(totalShards > 1) {
            builder.useSharding(shardId, totalShards);
        }
        startJDA();
    }

    public void startJDA() throws LoginException, InterruptedException, RateLimitedException {
        jda = builder.buildBlocking();
    }

    public void restartJDA(boolean wait) throws LoginException, InterruptedException, RateLimitedException {
        jda.shutdown(false);
        if(wait) {
            while(jda.getStatus() != JDA.Status.SHUTDOWN) {
                Thread.sleep(50);
            }
        }
        startJDA();
    }

    public void postStats() {
        if(GabrielBot.DEBUG) return;
        String token = GabrielData.config().dbotsToken;
        if(token == null || token.isEmpty()) return;
        JSONObject payload = new JSONObject().put("server_count", jda.getGuilds().size());
        JDA.ShardInfo info = jda.getShardInfo();
        if(info != null) {
            payload.put("shard_id", info.getShardId()).put("shard_count", info.getShardTotal());
        }
        try {
            Unirest.post("https://discordbots.org/api/bots/" + jda.getSelfUser().getId() + "/stats")
                    .header("Authorization", token)
                    .header("Content-Type", "application/json")
                    .body(payload)
                    .asJson();
        } catch(UnirestException e) {
            logger.error("Error posting stats to discordbots.org", e.getCause());
        }
    }

    public JDA getJDA() {
        return jda;
    }

    public GuildMusicPlayer getPlayer(long guildId) {
        return players.parallelStream().filter(p->p.guildId == guildId).findFirst().orElse(null);
    }

    public GuildMusicPlayer createPlayer(long guildId, long textChannelId, long voiceChannelId) {
        GuildMusicPlayer gmp = getPlayer(guildId);
        if(gmp == null) {
            synchronized(this) {
                if(gmp == null) {
                    gmp = new GuildMusicPlayer(guildId, textChannelId, voiceChannelId);
                    AudioManager manager = jda.getGuildById(guildId).getAudioManager();
                    manager.setSendingHandler(gmp);
                    players.add(gmp);
                }
            }
        }
        return gmp;
    }

    public GuildMusicPlayer removePlayer(long guildId) {
        GuildMusicPlayer[] gmp = new GuildMusicPlayer[1];
        players.removeIf(g->{
            if(g.guildId == guildId) {
                g.leave();
                gmp[0] = g;
                return true;
            }
            return false;
        });
        return gmp[0];
    }

    public List<GuildMusicPlayer> players() {
        return new ArrayList<>(players);
    }
}
