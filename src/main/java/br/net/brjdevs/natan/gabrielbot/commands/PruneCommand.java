package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.core.command.*;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.utils.PermissionUtil;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RegisterCommand.Class
public class PruneCommand {
    @RegisterCommand
    public static void register(CommandRegistry cr) {
        cr.register("prune", SimpleCommand.builder(CommandCategory.MODERATION)
                .description("Prunes messages")
                .help((thiz, event)->thiz.helpEmbed(event, "prune",
                        "`>>prune <x>`: prunes last x messages\n" +
                               "`>>prune <x> bot`: prunes last x messages sent by bots\n" +
                               "`>>prune <x> user`: prunes last x messages sent by users\n" +
                               "`>>prune <x> @mention`: prunes last x messages sent by the mentioned **user**\n" +
                               "\n" +
                               "The amount to prune ***must*** be between 2 and 1000 (inclusive)"
                ))
                .code((thiz, event, args)->{
                    if(!PermissionUtil.checkPermission(event.getChannel(), event.getGuild().getSelfMember(), Permission.MESSAGE_MANAGE)) {
                        event.getChannel().sendMessage("I need the Manage Messages permission").queue();
                        return;
                    }
                    if(!PermissionUtil.checkPermission(event.getChannel(), event.getGuild().getSelfMember(), Permission.MESSAGE_HISTORY)) {
                        event.getChannel().sendMessage("I need the Read Message History permission").queue();
                        return;
                    }
                    if(args.length == 0) {
                        thiz.onHelp(event);
                        return;
                    }
                    int messages;

                    try {
                        messages = Integer.parseInt(args[0]);
                    } catch(NumberFormatException e) {
                        event.getChannel().sendMessage(thiz.help(event)).queue();
                        return;
                    }

                    if(messages < 2 || messages > 1000) {
                        event.getChannel().sendMessage(thiz.help(event)).queue();
                        return;
                    }

                    Predicate<Message> filter;
                    if(args.length > 1) {
                        String target = args[1];
                        if(target.equals("bot")) {
                            filter = m->m.getAuthor().isBot();
                        } else if(target.equals("user")){
                            filter = m->!m.getAuthor().isBot();
                        } else if(target.matches("^<@!?\\d+?>")) {
                            String id = target.replaceAll("^<@!?(\\d+?)>", "$1");
                            long idLong;
                            try {
                                idLong = Long.parseLong(id);
                            } catch(NumberFormatException e) {
                                event.getChannel().sendMessage(thiz.help(event)).queue();
                                return;
                            }
                            filter = m->m.getAuthor().getIdLong() == idLong;
                        } else {
                            event.getChannel().sendMessage(thiz.help(event)).queue();
                            return;
                        }
                    } else {
                        filter = m->true;
                    }

                    int deleted = 0;
                    while(messages > 0) {
                        List<Message> toDelete = event.getChannel().getHistory().retrievePast(Math.min(100, messages)).complete()
                                .stream().filter(filter).collect(Collectors.toList());
                        deleted += toDelete.size();
                        switch(toDelete.size()){
                            case 0:
                                continue;
                            case 1:
                                toDelete.get(0).delete().queue();
                                break;
                            default:
                                event.getChannel().deleteMessages(toDelete).queue();
                                break;
                        }
                        messages -= 100;
                    }
                    event.getChannel().sendMessage("Successfully deleted " + deleted + " messages").queue();
                })
                .build());
    }
}
