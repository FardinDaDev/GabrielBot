package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.core.command.CommandCategory;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandRegistry;
import br.net.brjdevs.natan.gabrielbot.core.command.RegisterCommand;
import br.net.brjdevs.natan.gabrielbot.core.command.SimpleCommand;
import br.net.brjdevs.natan.gabrielbot.core.data.GabrielData;
import br.net.brjdevs.natan.gabrielbot.utils.cache.URLCache;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;

import java.awt.*;
import java.io.File;

@RegisterCommand.Class
public class ImageCommands {
    private static final URLCache cache = new URLCache(new File(".urlcache"), 10);
    private static final String BASEURL = "http://catgirls.brussell98.tk/api/random";
    private static final String NSFWURL = "http://catgirls.brussell98.tk/api/nsfw/random";

    @RegisterCommand
    public static void catgirls(CommandRegistry cr) {
        cr.register("catgirls", SimpleCommand.builder(CommandCategory.IMAGE)
                .description("Sends catgirl pictures")
                .help((thiz, event)->thiz.helpEmbed(event, "catgirls",
                        "`>>catgirls`: sends catgirl safe image\n`>>catgirls nsfw`: sends lewd catgirl image"
                ))
                .code((event, args)->{
                    try {
                        boolean nsfw = args.length > 0 && args[0].equalsIgnoreCase("nsfw");
                        if(nsfw) {
                            if(!event.getChannel().isNSFW())  {
                                GabrielData.ChannelData data = GabrielData.channels().get().get(event.getChannel().getId());
                                if(data == null || !data.nsfw) {
                                    event.getChannel().sendMessage("Not in a NSFW channel").queue();
                                    return;
                                }
                            }
                        }
                        JSONObject obj = Unirest.get(nsfw ? NSFWURL : BASEURL)
                                .asJson()
                                .getBody()
                                .getObject();
                        if(!obj.has("url")) {
                            event.getChannel().sendMessage("Unable to find image").queue();
                        } else {
                            checkVerification(event);
                            event.getChannel().sendFile(cache.input(obj.getString("url")), "catgirls.png", null).queue();
                        }
                    } catch(UnirestException e) {
                        event.getChannel().sendMessage("Unable to find image").queue();
                    }
                })
                .build());
    }

    @RegisterCommand
    public static void doge(CommandRegistry cr) {
        cr.register("doge", SimpleCommand.builder(CommandCategory.IMAGE)
                .description("Sends doge images with custom texts")
                .help((thiz, event)->thiz.helpEmbed(event, "doge", "`>>doge wow \"such doge\"`"))
                .code((thiz, event, args)->{
                    checkVerification(event);
                    String url = "http://dogr.io/" +  String.join("/", args).replace(" ", "%20") + ".png?split=false";
                    event.getChannel().sendMessage(new EmbedBuilder()
                            .setColor(Color.YELLOW)
                            .setTitle("Doge", "http://dogr.io")
                            .setImage(url)
                            .build()).queue();
                })
                .build()
        );
    }

    private static void checkVerification(GuildMessageReceivedEvent event) {
        if(event.getGuild().getExplicitContentLevel() != Guild.ExplicitContentLevel.OFF) {
            event.getChannel().sendMessage("Warning: this guild had explicit content scanning enabled, so images may take a while to load").queue();
        }
    }
}
