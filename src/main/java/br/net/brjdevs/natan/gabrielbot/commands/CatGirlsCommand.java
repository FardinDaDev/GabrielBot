package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.core.command.*;
import br.net.brjdevs.natan.gabrielbot.core.data.GabrielData;
import br.net.brjdevs.natan.gabrielbot.utils.cache.URLCache;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;

import java.io.File;

import static br.net.brjdevs.natan.gabrielbot.core.localization.LocalizationManager.*;

@RegisterCommand.Class
public class CatGirlsCommand {
    private static final URLCache cache = new URLCache(new File(".urlcache"), 10);
    private static final String BASEURL = "http://catgirls.brussell98.tk/api/random";
    private static final String NSFWURL = "http://catgirls.brussell98.tk/api/nsfw/random";

    @RegisterCommand
    public static void register(CommandRegistry registry) {
        registry.register("catgirls", SimpleCommand.builder(CommandCategory.IMAGE)
                .description("catgirls", "Sends catgirl pictures")
                .help((thiz, event)->thiz.helpEmbed(event, "catgirls",
                        "`>>catgirls`: sends catgirl safe image\n`>>catgirls nsfw`: sends lewd catgirl image"
                ))
                .code((event, args)->{
                    try {
                        boolean nsfw = args.length > 0 && args[0].equalsIgnoreCase("nsfw");
                        if(nsfw) {
                            GabrielData.ChannelData data = GabrielData.channels().get().get(event.getChannel().getId());
                            if(data == null || !data.nsfw) {
                                event.getChannel().sendMessage(getString(event.getGuild(), NOT_NSFW, "Not in a NSFW channel")).queue();
                                return;
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
}
