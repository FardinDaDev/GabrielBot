package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import br.net.brjdevs.natan.gabrielbot.core.command.*;
import br.net.brjdevs.natan.gabrielbot.core.data.GabrielData;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.core.EmbedBuilder;
import org.json.JSONObject;

import java.util.Arrays;

@RegisterCommand.Class
public class CatGirlsCommand {
    private static final String BASEURL = "http://catgirls.brussell98.tk/api/random";
    private static final String NSFWURL = "http://catgirls.brussell98.tk/api/nsfw/random";

    @RegisterCommand
    public static void register(CommandRegistry registry) {
        registry.register("catgirls", SimpleCommand.builder(CommandCategory.IMAGE)
                .description("Sends catgirl pictures")
                .help(SimpleCommand.helpEmbed("catgirls", CommandPermission.USER,
                        "Sends catgirl images",
                        "`>>catgirls`: sends catgirl safe image\n`>>catgirls nsfw`: sends lewd catgirl image"
                ))
                .permission(CommandPermission.USER)
                .code((event, args)->{
                    try {
                        boolean nsfw = args.length > 0 && args[0].equalsIgnoreCase("nsfw");
                        if(nsfw) {
                            GabrielData.ChannelData data = GabrielData.channels().get().get(event.getChannel().getId());
                            if(data == null || !data.nsfw) {
                                event.getChannel().sendMessage("Not on a NSFW channel").queue();
                                return;
                            }
                        }
                        JSONObject obj = Unirest.get(nsfw ? NSFWURL : BASEURL)
                                .asJson()
                                .getBody()
                                .getObject();
                        if(!obj.has("url")) {
                            event.getChannel().sendMessage("Unable to find image").queue();
                        } else {
                            event.getChannel().sendMessage(new EmbedBuilder().setImage(obj.getString("url")).build()).queue();
                        }
                    } catch(UnirestException e) {
                        event.getChannel().sendMessage("Unable to find image").queue();
                    }
                })
                .build());
    }
}
