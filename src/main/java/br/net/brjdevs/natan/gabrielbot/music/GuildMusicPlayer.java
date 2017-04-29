package br.net.brjdevs.natan.gabrielbot.music;

import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dv8tion.jda.core.audio.AudioSendHandler;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuildMusicPlayer implements AudioSendHandler {
    public static final Logger LOGGER = LoggerFactory.getLogger(GuildMusicPlayer.class);

    public final AudioPlayer player;
    public final long guildId;
    public final long textChannelId;
    public final long voiceChannelId;
    public final TrackScheduler scheduler;
    public final TrackLoader loader;
    private AudioFrame lastFrame;

    public GuildMusicPlayer(long guildId, long textChannelId, long voiceChannelId) {
        this.player = GabrielBot.getInstance().playerManager.createPlayer();
        this.guildId = guildId;
        this.textChannelId = textChannelId;
        this.voiceChannelId = voiceChannelId;
        this.scheduler = new TrackScheduler(this);
        this.loader = new TrackLoader(this);
        player.addListener(scheduler);
    }

    @Override
    public boolean canProvide() {
        lastFrame = player.provide();
        return lastFrame != null;
    }

    @Override
    public byte[] provide20MsAudio() {
        return lastFrame.data;
    }

    @Override
    public boolean isOpus() {
        return true;
    }

    public TextChannel getTextChannel() {
        return GabrielBot.getInstance().getGuildById(guildId).getTextChannelById(textChannelId);
    }

    public VoiceChannel getVoiceChannel() {
        return GabrielBot.getInstance().getGuildById(guildId).getVoiceChannelById(voiceChannelId);
    }

    public Guild getGuild() {
        return GabrielBot.getInstance().getGuildById(guildId);
    }
}
