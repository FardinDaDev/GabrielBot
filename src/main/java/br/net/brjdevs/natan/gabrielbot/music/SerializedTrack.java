package br.net.brjdevs.natan.gabrielbot.music;

import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SerializedTrack {
    public final long dj;
    public final byte[] track;

    public SerializedTrack(Track track) {
        this.dj = track.dj;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MessageOutput mo = new MessageOutput(baos);
        try {
            GabrielBot.getInstance().playerManager.encodeTrack(mo, track.track.makeClone());
        } catch(IOException e) {
            throw new AssertionError(e);
        }
        this.track = baos.toByteArray();
    }

    public Track toTrack() {
        ByteArrayInputStream bais = new ByteArrayInputStream(track);
        MessageInput mi = new MessageInput(bais);
        AudioTrack track;
        try {
            track = GabrielBot.getInstance().playerManager.decodeTrack(mi).decodedTrack;
        } catch(IOException e) {
            throw new AssertionError(e);
        }
        return new Track(track, dj);
    }
}
