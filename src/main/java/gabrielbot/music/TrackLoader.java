package gabrielbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import gabrielbot.GabrielBot;
import gabrielbot.core.listeners.operations.Operation;
import gabrielbot.core.listeners.operations.ReactionOperations;
import gabrielbot.utils.DiscordUtils;
import gabrielbot.utils.Utils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.function.IntConsumer;

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
            EmbedBuilder builder = new EmbedBuilder().setColor(Color.CYAN).setTitle("Song selection. Type the song number to continue.")
                    .setFooter("This timeouts in 10 seconds.", null);
            java.util.List<AudioTrack> tracks = playlist.getTracks();
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < 4 && i < tracks.size(); i++) {
                AudioTrack at = tracks.get(i);
                b.append('[').append(i + 1).append("] ").append(at.getInfo().title).append(" **(")
                        .append(Utils.getDuration(at.getInfo().length)).append(")**").append("\n");
            }
            builder.setDescription(b.toString());

            if(!event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION)) {
                event.getChannel().sendMessage(builder.build()).queue();
                IntConsumer consumer = (c) ->
                    trackLoaded(playlist.getTracks().get(c - 1), event.getAuthor(), false);

                DiscordUtils.selectInt(event, 5, consumer);
                return;
            }
            long id = event.getAuthor().getIdLong(); //just in case someone else uses play before timing out
            ReactionOperations.create(event.getChannel().sendMessage(builder.build()).complete(), 15, (e)->{
                if(e.getUser().getIdLong() != id) return Operation.IGNORED;
                int i = e.getReactionEmote().getName().charAt(0)-'\u0030';
                if(i < 1 || i > 4) return Operation.IGNORED;
                trackLoaded(playlist.getTracks().get(i - 1), event.getAuthor(), false);
                return Operation.COMPLETED;
            }, "\u0031\u20e3", "\u0032\u20e3", "\u0033\u20e3", "\u0034\u20e3");
        } else {
            long time = 0;
            long tracks = 0;
            for(AudioTrack t : playlist.getTracks()) {
                if(guildMusicPlayer.scheduler.tracks().size() > 300) {
                    event.getChannel().sendMessage("Max queue size reached (300 songs)").queue();
                    break;
                }
                trackLoaded(t, event.getAuthor(), true);
                time += t.getDuration();
                tracks++;
            }
            event.getChannel().sendMessage("Queued " + tracks + " songs from playlist **" + playlist.getName().replace("*", "\\*") + "** (" + Utils.getDuration(time) + ")").queue();
        }
    }

    @Override
    public void noMatches() {
        guildMusicPlayer.textChannel.sendMessage("No song that matches " + identifier + " found").queue();
    }

    @Override
    public void loadFailed(FriendlyException exception) {
        if(exception.severity == FriendlyException.Severity.COMMON) {
            guildMusicPlayer.textChannel.sendMessage("Unable to load " + identifier + ": " + exception.getMessage()).queue();
            return;
        }
        guildMusicPlayer.textChannel.sendMessage("An error occurred while loading, it's already been reported").queue();
        LOGGER.error("Error loading " + identifier, exception);
    }

    public void trackLoaded(AudioTrack track, User user, boolean silent) {
        if(event.getGuild().getSelfMember().getVoiceState().getChannel() == null) {
            event.getGuild().getAudioManager().openAudioConnection(event.getMember().getVoiceState().getChannel());
        }
        guildMusicPlayer.scheduler.queueTrack(track, user);
        if(!silent) {
            event.getChannel().sendMessage("Added to queue **" + track.getInfo().title.replace("*", "\\*") + "** (" + Utils.getDuration(track.getDuration()) + ")").queue();
        }
    }
}
