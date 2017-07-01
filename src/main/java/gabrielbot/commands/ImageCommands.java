package gabrielbot.commands;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import gabrielbot.GabrielBot;
import gabrielbot.core.command.Argument;
import gabrielbot.core.command.Command;
import gabrielbot.core.command.CommandCategory;
import gabrielbot.core.command.CommandPermission;
import gabrielbot.core.data.GabrielData;
import gabrielbot.utils.cache.URLCache;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;

import java.awt.Color;
import java.io.File;

@SuppressWarnings("unused")
public class ImageCommands {
    private static final URLCache cache = new URLCache(new File(".urlcache"), 10);
    private static final String BASEURL = "http://catgirls.brussell98.tk/api/random";
    private static final String NSFWURL = "http://catgirls.brussell98.tk/api/nsfw/random";

    @Command(
            name = "catgirls",
            description = "Sends catgirl pictures",
            usage = "`>>catgirls`: sends catgirl safe image\n" +
                    "`>>catgirls nsfw`: sends lewd catgirl image",
            permission = CommandPermission.USER,
            category = CommandCategory.IMAGE
    )
    public static void catgirls(@Argument("event") GuildMessageReceivedEvent event, @Argument("args") String[] args, @Argument("channel")TextChannel channel) {
        try {
            boolean nsfw = args.length > 0 && args[0].equalsIgnoreCase("nsfw");
            if (nsfw) {
                if (!channel.isNSFW()) {
                    GabrielData.ChannelData data = GabrielData.channels().get().get(channel.getId());
                    if (data == null || !data.nsfw) {
                        channel.sendMessage("Not in a NSFW channel").queue();
                        return;
                    }
                }
            }
            JSONObject obj = Unirest.get(nsfw ? NSFWURL : BASEURL)
                    .asJson()
                    .getBody()
                    .getObject();
            if (!obj.has("url")) {
                channel.sendMessage("Unable to find image").queue();
            } else {
                checkVerification(event);
               channel.sendFile(cache.input(obj.getString("url")), "catgirls.png", null).queue();
            }
        } catch (UnirestException e) {
            GabrielBot.LOGGER.error(null, e);
            channel.sendMessage("Unable to find image").queue();
        }
    }

    @Command(
            name = "doge",
            description = "Sends doge images with custom texts",
            usage = "`>>doge wow \"such doge\"`",
            permission = CommandPermission.USER,
            category = CommandCategory.IMAGE
    )
    public static void doge(@Argument("event") GuildMessageReceivedEvent event, @Argument("args") String[] args, @Argument("channel") TextChannel channel) {
        checkVerification(event);
        String url = "http://dogr.io/" + String.join("/", args).replace(" ", "%20") + ".png?split=false";
        channel.sendMessage(new EmbedBuilder()
                .setColor(Color.YELLOW)
                .setTitle("Doge", "http://dogr.io")
                .setImage(url)
                .build()).queue();
    }

    private static void checkVerification(GuildMessageReceivedEvent event) {
        if (event.getGuild().getExplicitContentLevel() != Guild.ExplicitContentLevel.OFF) {
            event.getChannel().sendMessage("Warning: this guild had explicit content scanning enabled, so images may take a while to load").queue();
        }
    }
}
