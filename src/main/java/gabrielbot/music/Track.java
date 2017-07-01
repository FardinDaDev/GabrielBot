package gabrielbot.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class Track {
    public final AudioTrack track;
    public final long dj;

    public Track(AudioTrack track, long dj) {
        this.track = track;
        this.dj = dj;
    }
}
