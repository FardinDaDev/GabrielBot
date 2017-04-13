package br.net.brjdevs.natan.gabrielbot.core.jda;

import br.net.brjdevs.natan.gabrielbot.core.listeners.MainListener;
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.audio.factory.DefaultSendFactory;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.security.auth.login.LoginException;

public class Shard {
    private final JDABuilder builder;
    private JDA jda;

    public Shard(String token, int shardId, int totalShards, boolean nas) throws LoginException, InterruptedException, RateLimitedException {
        builder = new JDABuilder(AccountType.BOT)
                .setToken(token)
                .setWebSocketTimeout(10000)
                .setAudioEnabled(true)
                .setAudioSendFactory(nas ? new NativeAudioSendFactory() : new DefaultSendFactory())
                .setAutoReconnect(true)
                .setCorePoolSize(10)
                .setEventManager(new EventManager(shardId))
                .addEventListener(new MainListener())
                .setGame(Game.of(">>help"));
        if(totalShards > 1) {
            builder.useSharding(shardId, totalShards);
        }
        restartJDA();
    }

    public void startJDA() throws LoginException, InterruptedException, RateLimitedException {
        jda = builder.buildBlocking();
    }

    public void restartJDA() throws LoginException, InterruptedException, RateLimitedException {
        if(jda != null) {
            jda.shutdown(false);
        }
        startJDA();
    }

    public JDA getJDA() {
        return jda;
    }
}
