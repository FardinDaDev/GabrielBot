package gabrielbot.commands;

import gabrielbot.GabrielBot;
import gabrielbot.core.command.Argument;
import gabrielbot.core.command.Command;
import gabrielbot.core.command.CommandCategory;
import gabrielbot.core.command.CommandPermission;
import gabrielbot.core.command.CommandReference;
import gabrielbot.utils.DBots;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.Arrays;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class DBotsCommand {
    @Command(
            name = "bots",
            description = "Gets discordbots.org bot info",
            usage = "`>>bots name <name>`: gets a bot by name\n" +
                    "`>>bots id <id>`: gets a bot by id\n" +
                    "`>>bots owner <id>`: gets a bot by owner id\n" +
                    "`>>bots prefix <prefix>`: gets a bot by prefix",
            permission = CommandPermission.USER,
            category = CommandCategory.UTIL
    )
    public static void dbots(@Argument("this") CommandReference thiz, @Argument("event") GuildMessageReceivedEvent event, @Argument("channel") TextChannel channel, @Argument("args") String[] args) {
        if (args.length < 2) {
            thiz.onHelp(event);
            return;
        }
        if (args[0].equals("name")) {
            channel.sendMessage(bot(event, DBots.byName(String.join(" ", Arrays.copyOfRange(args, 1, args.length))))).queue();
            return;
        }
        if(args[0].equals("prefix")) {
            channel.sendMessage(bot(event, DBots.byPrefix(String.join(" ", Arrays.copyOfRange(args, 1, args.length))))).queue();
            return;
        }
        long id;
        try {
            id = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            channel.sendMessage("`" + args[1] + "` is not a valid number").queue();
            return;
        }
        MessageEmbed embed;
        switch (args[0]) {
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
        channel.sendMessage(embed).queue();
    }

    private static MessageEmbed bot(GuildMessageReceivedEvent event, DBots.BotInfo[] infos) {
        if (infos.length == 0) {
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
                .addField("ID", "" + info.id, true)
                .addField("Prefix",
                        info.prefix.replace("*", "\\*").replace("_", "\\_").replace("~", "\\~").replace("`", "\\`"),
                        true)
                .addField("Invite", "[Click here](" + info.invite + ")", true)
                .addField("Owner" + (info.owners.length == 1 ? "" : "s"),
                        Arrays.stream(info.owners).mapToObj(id -> event.getGuild().getMemberById(id) != null ? "<@" + id + ">" : getName(id)).collect(Collectors.joining("\n")),
                        true)
                .addField("Short Description", info.shortDesc, false)
                .addField("Shard Count", info.shardCount == -1 ? "Unavailable" : "" + info.shardCount, true)
                .addField("Guild Count", info.guildCount == -1 ? "Unavailable" : "" + info.guildCount, true)
                .addField("Upvotes", "" + info.upvotes, true)
                .addField("Lib", info.lib, true)
                .addField("Certified", "" + info.certified, true)
                .setThumbnail(info.avatarUrl)
                .setColor(event.getMember().getColor())
                .setFooter(infos.length > 1 ? "Best match out of " + infos.length : "Only bot that matches", null)
                .build();

    }

    private static String getName(long id) {
        User u = GabrielBot.getInstance().getUserById(id);
        if (u != null) return u.getName() + "#" + u.getDiscriminator();
        return "<@" + id + ">";
    }
}
