package gabrielbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import gabrielbot.GabrielBot;
import net.dv8tion.jda.core.audio.AudioSendHandler;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.managers.AudioManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class GuildMusicPlayer implements AudioSendHandler {
    public enum LeaveReason {
        MUTED(" i was muted"),
        ALONE(" i was left alone");
        public final String reason;

        LeaveReason(String reason) {
           this.reason = reason;
        }
    }

    public static final ScheduledExecutorService LEAVE_EXECUTOR = Executors.newScheduledThreadPool(5, r->{
        Thread t = new Thread(r, "VoiceLeaveThread");
        t.setDaemon(true);
        return t;
    });

    public final AudioPlayer player;
    public final long guildId;
    public final TextChannel textChannel;
    public final VoiceChannel voiceChannel;
    public final TrackScheduler scheduler;
    public final TrackLoader loader;
    private AudioFrame lastFrame;
    private ScheduledFuture<?> leaveTask;

    public GuildMusicPlayer(long guildId, long textChannelId, long voiceChannelId) {
        this.player = GabrielBot.getInstance().playerManager.createPlayer();
        this.guildId = guildId;
        Guild guild = GabrielBot.getInstance().getGuildById(guildId);
        this.textChannel = guild.getTextChannelById(textChannelId);
        this.voiceChannel = guild.getVoiceChannelById(voiceChannelId);
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

    public Guild getGuild() {
        return GabrielBot.getInstance().getGuildById(guildId);
    }

    public void leave() {
        leaveTask = null;
        AudioManager manager = getGuild().getAudioManager();
        manager.setSendingHandler(null);
        manager.closeAudioConnection();
        scheduler.clear();
        player.stopTrack();
        player.destroy();
        GabrielBot.getInstance().removePlayer(guildId);
        if(GabrielBot.getInstance().getPlayer(guildId) != null) {
            GabrielBot.LOGGER.error("Player was not removed: " + this + " " + guildId);
        }
    }

    private void leaveTask() {
        textChannel.sendMessage("Player stopped").queue();
        leave();
    }

    public void scheduleLeave(LeaveReason reason) {
        if(leaveTask != null) leaveTask.cancel(true);
        player.setPaused(true);
        textChannel.sendMessage("I'll leave the voice channel in 2 minutes because" + reason.reason).queue();
        leaveTask = LEAVE_EXECUTOR.schedule(this::leaveTask, 2, TimeUnit.MINUTES);
    }

    public boolean isLeaveScheduled() {
        return leaveTask != null;
    }

    public void cancelLeave() {
        if(leaveTask != null) {
            leaveTask.cancel(true);
            textChannel.sendMessage("Leave timer stopped").queue();
            player.setPaused(false);
        }
        leaveTask = null;
    }
}
