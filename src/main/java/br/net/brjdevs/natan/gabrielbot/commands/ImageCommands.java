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
import org.json.JSONObject;

import java.awt.*;
import java.io.File;

import static br.net.brjdevs.natan.gabrielbot.core.localization.LocalizationManager.IMAGE_NOT_FOUND;
import static br.net.brjdevs.natan.gabrielbot.core.localization.LocalizationManager.NOT_NSFW;
import static br.net.brjdevs.natan.gabrielbot.core.localization.LocalizationManager.getString;

@RegisterCommand.Class
public class ImageCommands {
    private static final URLCache cache = new URLCache(new File(".urlcache"), 10);
    private static final String BASEURL = "http://catgirls.brussell98.tk/api/random";
    private static final String NSFWURL = "http://catgirls.brussell98.tk/api/nsfw/random";

    @RegisterCommand
    public static void catgirls(CommandRegistry cr) {
        cr.register("catgirls", SimpleCommand.builder(CommandCategory.IMAGE)
                .description("catgirls", "Sends catgirl pictures")
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
                                    event.getChannel().sendMessage(getString(event.getGuild(), NOT_NSFW, "Not in a NSFW channel")).queue();
                                    return;
                                }
                            }
                        }
                        JSONObject obj = Unirest.get(nsfw ? NSFWURL : BASEURL)
                                .asJson()
                                .getBody()
                                .getObject();
                        if(!obj.has("url")) {
                            event.getChannel().sendMessage(getString(event.getGuild(), IMAGE_NOT_FOUND, "Unable to find image")).queue();
                        } else {
                            event.getChannel().sendFile(cache.input(obj.getString("url")), "catgirls.png", null).queue();
                        }
                    } catch(UnirestException e) {
                        event.getChannel().sendMessage(getString(event.getGuild(), IMAGE_NOT_FOUND, "Unable to find image")).queue();
                    }
                })
                .build());
    }

    @RegisterCommand
    public static void doge(CommandRegistry cr) {
        cr.register("doge", SimpleCommand.builder(CommandCategory.IMAGE)
                .description("doge", "Sends doge images with custom texts")
                .help((thiz, event)->thiz.helpEmbed(event, "doge", "`>>doge wow \"such doge\"`"))
                .code((thiz, event, args)->{
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
}
