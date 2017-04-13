package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import br.net.brjdevs.natan.gabrielbot.core.command.*;
import net.dv8tion.jda.core.EmbedBuilder;

@RegisterCommand.Class
public class HelpCommand {
    @RegisterCommand
    public static void register(CommandRegistry registry) {
        registry.register("help", SimpleCommand.builder()
            .description("Shows this screen")
            .permission(CommandPermission.USER)
            .help(SimpleCommand.helpEmbed("help", CommandPermission.USER,
                    "Shows help about a command (What did you expect?!)",
                    "`>>help`: Lists all commands\n`>>help <command>`: Shows help for the specified command"
            ))
            .code((event, args)->{
                if(args.length > 0) {
                    Command cmd = GabrielBot.getInstance().registry.commands().get(args[0]);
                    if(cmd == null) {
                        event.getChannel().sendMessage("No command named " + args[0]).queue();
                        return;
                    }
                    try {
                        event.getChannel().sendMessage(cmd.help()).queue();
                    } catch(UnsupportedOperationException e) {
                        event.getChannel().sendMessage("No help available for this command").queue();
                    }
                    return;
                }
                StringBuilder sb = new StringBuilder();
                GabrielBot.getInstance().registry.commands().forEach((name, command)->{
                    if(command.permission().test(event.getGuild(), event.getMember())) sb.append("**").append(name).append("**: ").append(command.description()).append('\n');
                });
                event.getChannel().sendMessage(new EmbedBuilder()
                        .setDescription(sb.toString())
                        .build()).queue();
            })
        .build());
    }
}
