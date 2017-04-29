package br.net.brjdevs.natan.gabrielbot.music;

import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;

import static br.net.brjdevs.natan.gabrielbot.core.localization.LocalizationManager.*;

public class TrackScheduler extends AudioEventAdapter {
    public static final Logger LOGGER = LoggerFactory.getLogger(TrackScheduler.class);
    public static final int MAX_QUEUE_SIZE = 300;

    private final ConcurrentLinkedQueue<Track> tracks = new ConcurrentLinkedQueue<>();
    private final GuildMusicPlayer guildMusicPlayer;
    private final TLongList voteskips = new TLongArrayList();
    private long playingMessageId;
    private Track currentTrack;

    TrackScheduler(GuildMusicPlayer player) {
        this.guildMusicPlayer = player;
    }

    public void voteskip(long userId) {
        TextChannel tc = guildMusicPlayer.getTextChannel();
        Guild guild = tc.getGuild();
        int votes = getRequiredVotes(guildMusicPlayer.getVoiceChannel());
        if(voteskips.contains(userId)) {
            voteskips.remove(userId);
            tc.sendMessage(
                    getString(guild, Music.VOTE_REMOVED, "Your vote has been removed! $votes$ more to skip").replace("$votes$", String.valueOf(votes-voteskips.size()))
            ).queue();
        } else {
            voteskips.add(userId);
            if(voteskips.size() >= votes) {
                tc.sendMessage(
                        getString(guild, Music.SKIPPING, "Reached required number of votes, skipping song")
                ).queue();
                nextTrack();
            } else {
                tc.sendMessage(
                        getString(guild, Music.VOTE_ADDED, "Your vote has been added! $votes$ more to skip").replace("$votes$", String.valueOf(votes-voteskips.size()))
                ).queue();
            }
        }
    }

    public void nextTrack() {
        if(tracks.size() == 0) {
            guildMusicPlayer.getTextChannel().sendMessage(
                    getString(guildMusicPlayer.getGuild(), Music.FINISHED_PLAYING, "Finished playing")
            ).queue();
            GabrielBot.getInstance().removePlayer(guildMusicPlayer.guildId);
        } else {
            voteskips.clear();
            guildMusicPlayer.player.startTrack((currentTrack = tracks.poll()).track, false);
        }
    }

    public void queueTrack(AudioTrack track, User user) {
        if(tracks.size() > MAX_QUEUE_SIZE) {
            return;
        }
        if(currentTrack == null) {
            currentTrack = new Track(track, user.getIdLong());
            guildMusicPlayer.player.startTrack(track, false);
        }
        else {
            tracks.add(new Track(track, user.getIdLong()));
        }
    }

    public void clear() {
        tracks.clear();
    }

    public Track currentTrack() {
        return currentTrack;
    }

    private void deletePlayingMessage() {
        if(playingMessageId != 0) {
            guildMusicPlayer.getTextChannel().deleteMessageById(playingMessageId).queue();
        }
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        deletePlayingMessage();
        guildMusicPlayer.getTextChannel().sendMessage(
                getString(guildMusicPlayer.getGuild(), Music.NOW_PLAYING, "Now playing $identifier$").replace("$identifier$", track.getInfo().title)
        ).queue(m->{
            playingMessageId = m.getIdLong();
        });
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if(endReason.mayStartNext) {
            nextTrack();
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        deletePlayingMessage();
        if(exception.severity == FriendlyException.Severity.COMMON) {
            guildMusicPlayer.getTextChannel().sendMessage(
                    getString(guildMusicPlayer.getGuild(), Music.UNABLE_TO_PLAY_COMMON, "Unable to play $identifier$: $msg$").replace("$identifier$", track.getInfo().title).replace("$msg$", exception.getMessage())
            ).queue();
            nextTrack();
            return;
        }
        guildMusicPlayer.getTextChannel().sendMessage(
                getString(guildMusicPlayer.getGuild(), Music.UNABLE_TO_PLAY_REPORTED, "An error occurred while playing, it's already been reported")
        ).queue();
        LOGGER.error("Error playing " + track.getIdentifier(), exception);
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        deletePlayingMessage();
        guildMusicPlayer.getTextChannel().sendMessage(
                getString(guildMusicPlayer.getGuild(), Music.TRACK_STUCK, "Track $identifier$ got stuck, skipping...").replace("$identifier$", track.getInfo().title)
        ).queue();
        nextTrack();
    }

    private static int getRequiredVotes(VoiceChannel voiceChannel) {
        return (int) ((voiceChannel.getMembers().stream().filter(m->!m.getUser().isBot()).count() - 1) / .65) + 1;
    }
}
