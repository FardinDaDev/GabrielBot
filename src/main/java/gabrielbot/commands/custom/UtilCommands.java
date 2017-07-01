package gabrielbot.commands.custom;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import gabrielbot.GabrielBot;
import gabrielbot.core.command.Argument;
import gabrielbot.core.command.Command;
import gabrielbot.core.command.CommandCategory;
import gabrielbot.core.command.CommandPermission;
import gabrielbot.utils.Utils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.List;

public class UtilCommands {
    @Command(
            name = "ytsearch",
            description = "Search youtube for videos",
            usage = "`>>ytsearch <name of the video>`",
            permission = CommandPermission.USER,
            category = CommandCategory.UTIL
    )
    public static void ytsearch(@Argument("member") Member member, @Argument("channel") TextChannel channel, @Argument("input") String input) {
        GabrielBot.getInstance().playerManager.loadItem("ytsearch: " + input, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {

            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                EmbedBuilder eb = new EmbedBuilder().setTitle("Search results").setColor(member.getColor()).setThumbnail("https://www.seeklogo.net/wp-content/uploads/2016/11/youtube-logo-preview-1.png");
                List<AudioTrack> list = playlist.getTracks();
                for(int i = 0; i < Math.min(5, list.size()); i++) {
                    AudioTrackInfo info = list.get(i).getInfo();
                    eb.appendDescription(String.format("%d - **[%s](%s)** - Uploaded by %s `(%s)`%n%n",
                            i+1,
                            info.title.replace("]", "\\]"),
                            info.uri,
                            info.author,
                            Utils.getDuration(info.length)
                    ));
                }
                channel.sendMessage(eb.build()).queue();
            }

            @Override
            public void noMatches() {
                channel.sendMessage("No video found").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {

            }
        });
    }
}
