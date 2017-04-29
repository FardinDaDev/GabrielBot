package br.net.brjdevs.natan.gabrielbot.music;

import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import br.net.brjdevs.natan.gabrielbot.utils.DiscordUtils;
import br.net.brjdevs.natan.gabrielbot.utils.Utils;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.function.IntConsumer;

import static br.net.brjdevs.natan.gabrielbot.core.localization.LocalizationManager.*;

public class TrackLoader implements AudioLoadResultHandler {
    public static final Logger LOGGER = LoggerFactory.getLogger(TrackLoader.class);

    private final GuildMusicPlayer guildMusicPlayer;
    private GuildMessageReceivedEvent event;
    private String identifier;

    TrackLoader(GuildMusicPlayer player) {
        this.guildMusicPlayer = player;
    }

    public void load(GuildMessageReceivedEvent event, String identifier) {
        this.event = event;
        this.identifier = identifier;
        GabrielBot.getInstance().playerManager.loadItem(identifier, this);
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        trackLoaded(track, event.getAuthor(), false);
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        if (playlist.isSearchResult()) {
            EmbedBuilder builder = new EmbedBuilder().setColor(Color.CYAN).setTitle(
                        getString(event.getGuild(), Music.SONG_SELECTION, "Song selection. Type the song number to continue."),
                    null)
                    .setFooter(
                        getString(event.getGuild(), Music.SONG_SELECTION_TIMEOUT, "This timeouts in 10 seconds."),
                    null);
            java.util.List<AudioTrack> tracks = playlist.getTracks();
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < 4 && i < tracks.size(); i++) {
                AudioTrack at = tracks.get(i);
                b.append('[').append(i + 1).append("] ").append(at.getInfo().title).append(" **(")
                        .append(Utils.getDurationMinutes(at.getInfo().length)).append(")**").append("\n");
            }

            event.getChannel().sendMessage(builder.setDescription(b.toString()).build()).queue();
            IntConsumer consumer = (c)->{
                trackLoaded(playlist.getTracks().get(c - 1), event.getAuthor(), false);
            };
            DiscordUtils.selectInt(event, 5, consumer);
        } else {
            playlist.getTracks().forEach(t->trackLoaded(t, event.getAuthor(), true));
        }
    }

    @Override
    public void noMatches() {
        guildMusicPlayer.getTextChannel().sendMessage(
                getString(event.getGuild(), Music.NO_MATCHES, "No song that matches $identifier$ found").replace("$identifier$", identifier)
        ).queue();
    }

    @Override
    public void loadFailed(FriendlyException exception) {
        if(exception.severity == FriendlyException.Severity.COMMON) {
            guildMusicPlayer.getTextChannel().sendMessage(
                    getString(event.getGuild(), Music.UNABLE_TO_LOAD_COMMON, "Unable to load $identifier$: $msg$").replace("$identifier$", identifier).replace("$msg$", exception.getMessage())
            ).queue();
            return;
        }
        guildMusicPlayer.getTextChannel().sendMessage(
                getString(event.getGuild(), Music.UNABLE_TO_LOAD_REPORTED, "An error occurred while loading, it's already been reported")
        ).queue();
        LOGGER.error("Error loading " + identifier, exception);
    }

    public void trackLoaded(AudioTrack track, User user, boolean silent) {
        if(event.getGuild().getSelfMember().getVoiceState().getChannel() == null) {
            event.getGuild().getAudioManager().openAudioConnection(event.getMember().getVoiceState().getChannel());
        }
        guildMusicPlayer.scheduler.queueTrack(track, user);
    }
}
