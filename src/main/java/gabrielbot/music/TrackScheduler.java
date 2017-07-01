package gabrielbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TrackScheduler extends AudioEventAdapter {
    public static final Logger LOGGER = LoggerFactory.getLogger(TrackScheduler.class);
    public static final int MAX_QUEUE_SIZE = 300;

    private final ConcurrentLinkedQueue<Track> tracks = new ConcurrentLinkedQueue<>();
    private final GuildMusicPlayer guildMusicPlayer;
    private final TLongSet voteskips = new TLongHashSet();
    private long playingMessageId;
    private volatile Track currentTrack;

    TrackScheduler(GuildMusicPlayer player) {
        this.guildMusicPlayer = player;
    }

    public void voteskip(long userId) {
        TextChannel tc = guildMusicPlayer.textChannel;
        int votes = getRequiredVotes(guildMusicPlayer.voiceChannel);
        if(voteskips.contains(userId)) {
            voteskips.remove(userId);
            tc.sendMessage("Your vote has been removed! " + (votes-voteskips.size()) + " more to skip").queue();
        } else {
            voteskips.add(userId);
            if(voteskips.size() >= votes) {
                tc.sendMessage("Reached required number of votes, skipping song").queue();
                nextTrack();
            } else {
                tc.sendMessage("Your vote has been added! " + (votes-voteskips.size()) + " more to skip").queue();
            }
        }
    }

    public void nextTrack() {
        if(tracks.size() == 0) {
            guildMusicPlayer.textChannel.sendMessage("Finished playing").queue();
            guildMusicPlayer.leave();
        } else {
            voteskips.clear();
            guildMusicPlayer.player.startTrack((currentTrack = tracks.poll()).track, false);
        }
    }

    public void queueTrack(AudioTrack track, User user) {
        if(currentTrack == null) {
            currentTrack = new Track(track, user.getIdLong());
            guildMusicPlayer.player.startTrack(track, false);
        } else {
            tracks.offer(new Track(track, user.getIdLong()));
        }
    }

    public void clear() {
        tracks.clear();
    }

    public Track currentTrack() {
        return currentTrack;
    }

    public void fromSerialized(Track current, List<Track> queue) {
        currentTrack = current;
        guildMusicPlayer.player.startTrack(current.track, false);
        tracks.addAll(queue);
    }

    private void deletePlayingMessage() {
        if(playingMessageId != 0) {
            guildMusicPlayer.textChannel.deleteMessageById(playingMessageId).queue();
        }
    }

    public ConcurrentLinkedQueue<Track> tracks() {
        return tracks;
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        deletePlayingMessage();
        guildMusicPlayer.textChannel.sendMessage("Now playing " + track.getInfo().title).queue(m->
            playingMessageId = m.getIdLong()
        );
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
            guildMusicPlayer.textChannel.sendMessage("Unable to play " + track.getInfo().title + ": " + exception.getMessage()).queue();
            nextTrack();
            return;
        }
        guildMusicPlayer.textChannel.sendMessage("An error occurred while playing, it's already been reported").queue();
        LOGGER.error("Error playing " + track.getIdentifier(), exception);
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        deletePlayingMessage();
        guildMusicPlayer.textChannel.sendMessage("Track got stuck, skipping...").queue();
        nextTrack();
    }

    private static int getRequiredVotes(VoiceChannel voiceChannel) {
        return (int) ((voiceChannel
                        .getMembers()
                        .stream()
                        .filter(m->
                                !m.getUser()
                                        .isBot()
                        ).count() - 1) / .65) + 1;
    }
}
