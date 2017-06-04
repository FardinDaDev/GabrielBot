package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.core.command.CommandCategory;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandRegistry;
import br.net.brjdevs.natan.gabrielbot.core.command.RegisterCommand;
import br.net.brjdevs.natan.gabrielbot.core.command.SimpleCommand;
import br.net.brjdevs.natan.gabrielbot.utils.DBots;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.Arrays;
import java.util.stream.Collectors;

@RegisterCommand.Class
public class DBotsCommand {
    @RegisterCommand
    public static void dbots(CommandRegistry cr) {
        cr.register("bots", SimpleCommand.builder(CommandCategory.MISC)
                .description("Gets discordbots.org bot info")
                .help((thiz, event)->thiz.helpEmbed(event, "bots",
                        "`>>bots name <name>`: gets a bot by name\n" +
                               "`>>bots id <id>`: gets a bot by id\n" +
                               "`>>bots owner <id>`: gets a bot by owner id"
                ))
                .code((thiz, event, args)->{
                    if(args.length < 2) {
                        thiz.onHelp(event);
                        return;
                    }
                    if(args[0].equals("name")) {
                        event.getChannel().sendMessage(bot(event, DBots.byName(args[1]))).queue();
                        return;
                    }
                    long id;
                    try {
                        id = Long.parseLong(args[1]);
                    } catch(NumberFormatException e) {
                        event.getChannel().sendMessage("`" + args[1] + "` is not a valid number").queue();
                        return;
                    }
                    MessageEmbed embed;
                    switch(args[0]) {
                        case "id":
                            embed = bot(event, DBots.byId(id));
                            break;
                        case "owner":
                            embed = bot(event, DBots.byOwner(id));
                            break;
                        default:
                            thiz.onHelp(event);
                            return;
                    }
                    event.getChannel().sendMessage(embed).queue();
                })
                .build()
        );
    }

    private static MessageEmbed bot(GuildMessageReceivedEvent event, DBots.BotInfo[] infos) {
        if(infos.length == 0) {
            return new EmbedBuilder()
                    .setDescription("No bot that matches the specified criteria found")
                    .setColor(event.getMember().getColor())
                    .setTitle("No bot found")
                    .build();
        }
        DBots.BotInfo info = infos[0];
        return new EmbedBuilder()
                .setAuthor(info.name + "#" + info.discriminator, null, info.avatarUrl)
                .setDescription("\u200D")
                .setTitle("Bot Info")
                .addField("ID", ""+info.id, true)
                .addField("Prefix",
                        info.prefix.replace("*", "\\*").replace("_", "\\_").replace("~", "\\~").replace("`", "\\`"),
                true)
                .addField("Invite", "[Click here](" + info.invite + ")", true)
                .addField("Owners", Arrays.stream(info.owners).mapToObj(id->"<@" + id + ">").collect(Collectors.joining("\n")), true)
                .addField("Short Description", info.shortDesc, false)
                .addField("Shard Count", info.shardCount == -1 ? "Unavailable" : ""+info.shardCount, true)
                .addField("Guild Count", info.guildCount == -1 ? "Unavailable" : ""+info.guildCount, true)
                .addField("Upvotes", ""+info.upvotes, true)
                .addField("Lib", info.lib, true)
                .addField("Certified", ""+info.certified, true)
                .setImage(info.avatarUrl)
                .setColor(event.getMember().getColor())
                .build();

    }
}
