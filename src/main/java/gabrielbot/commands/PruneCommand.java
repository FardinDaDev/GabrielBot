package gabrielbot.commands;

import gabrielbot.core.command.Argument;
import gabrielbot.core.command.Command;
import gabrielbot.core.command.CommandCategory;
import gabrielbot.core.command.CommandPermission;
import gabrielbot.core.command.CommandReference;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.utils.PermissionUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class PruneCommand {
    private static final Consumer<Throwable> IGNORE = t->{};

    @Command(
            name = "prune",
            description = "Prunes up to 1000 messages",
            usage = "`>>prune <x>`: prunes last x messages\n" +
                    "`>>prune <x> bot`: prunes last x messages sent by bots\n" +
                    "`>>prune <x> user`: prunes last x messages sent by users\n" +
                    "`>>prune <x> @mention`: prunes last x messages sent by the mentioned **user**\n" +
                    "\n" +
                    "The amount to prune ***must*** be between 2 and 1000 (inclusive)",
            permission = CommandPermission.ADMIN,
            category = CommandCategory.MODERATION
    )
    public static void prune(@Argument("this") CommandReference thiz, @Argument("event")GuildMessageReceivedEvent event, @Argument("args") String[] args, @Argument("guild") Guild guild, @Argument("channel")TextChannel channel) {
        if (!PermissionUtil.checkPermission(channel, guild.getSelfMember(), Permission.MESSAGE_MANAGE)) {
            channel.sendMessage("I need the Manage Messages permission").queue();
            return;
        }
        if (!PermissionUtil.checkPermission(channel, guild.getSelfMember(), Permission.MESSAGE_HISTORY)) {
            channel.sendMessage("I need the Read Message History permission").queue();
            return;
        }
        if (args.length == 0) {
            thiz.onHelp(event);
            return;
        }
        int messages;

        try {
            messages = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            channel.sendMessage("`" + args[0] + "` is not a valid integer").queue();
            return;
        }

        if (messages < 2 || messages > 1000) {
            thiz.onHelp(event);
            return;
        }

        Predicate<Message> filter;
        if (args.length > 1) {
            String target = args[1];
            if (target.equals("bot")) {
                filter = m -> m.getAuthor().isBot();
            } else if (target.equals("user")) {
                filter = m -> !m.getAuthor().isBot();
            } else if (target.matches("^<@!?\\d+?>")) {
                String id = target.replaceAll("^<@!?(\\d+?)>", "$1");
                long idLong;
                try {
                    idLong = Long.parseLong(id);
                } catch (NumberFormatException e) {
                    thiz.onHelp(event);
                    return;
                }
                filter = m -> m.getAuthor().getIdLong() == idLong;
            } else {
                thiz.onHelp(event);
                return;
            }
        } else {
            filter = m -> true;
        }


        List<Message> toDelete = new ArrayList<>();
        for(Message message : channel.getIterableHistory().limit(100)) {
            if(filter.test(message)) {
                toDelete.add(message);
            }
            if(--messages == 0) break;
        }
        int deleted = toDelete.size();

        while(toDelete.size() > 100) {
            channel.deleteMessages(toDelete.subList(0, 99)).queue(null, IGNORE);
            toDelete = toDelete.subList(100, toDelete.size()-1);
        }
        switch(toDelete.size()) {
            case 0: break;
            case 1:
                channel.deleteMessageById(toDelete.get(0).getIdLong()).queue(null, IGNORE);
                break;
            default:
                channel.deleteMessages(toDelete).queue(null, IGNORE);
        }

        channel.sendMessage("Successfully deleted " + deleted + " messages").queue();
    }
}
