package br.net.brjdevs.natan.gabrielbot.music;

import java.util.List;

public class SerializedPlayer {
    public final SerializedTrack[] tracks;
    public final long position, guildId, textChannelId, voiceChannelId;

    public SerializedPlayer(List<SerializedTrack> tracks, long position, long guildId, long textChannelId, long voiceChannelId) {
        this.tracks = tracks.stream().toArray(SerializedTrack[]::new);
        this.position = position;
        this.guildId = guildId;
        this.textChannelId = textChannelId;
        this.voiceChannelId = voiceChannelId;
    }
}
