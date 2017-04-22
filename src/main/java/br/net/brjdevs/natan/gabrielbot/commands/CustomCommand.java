package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import br.net.brjdevs.natan.gabrielbot.core.command.*;
import br.net.brjdevs.natan.gabrielbot.core.data.GabrielData;
import net.dv8tion.jda.core.EmbedBuilder;

import java.util.Arrays;
import java.util.stream.Collectors;

import static br.net.brjdevs.natan.gabrielbot.core.localization.LocalizationManager.*;

@RegisterCommand.Class
public class CustomCommand {
    @RegisterCommand
    public static void register(CommandRegistry cr) {
        cr.register("custom", SimpleCommand.builder(CommandCategory.MISC)
                .description("custom", "Manages this guild's custom commands")
                .help((thiz, event)->thiz.helpEmbed(event, "custom",
                        "`>>custom add <name> <response>`: adds a custom command\n" +
                                "`>>custom remove <name>`: removes a custom command\n" +
                                "`>>custom <ls|list>`: lists custom commands\n" +
                                "`>>custom raw <name>`: shows the response of a custom command\n" +
                                "`>>custom rename <name> <newname>`: renames a custom command"
                ))
                .code((thiz, event, args)->{
                    if(args.length == 0) {
                        thiz.onHelp(event);
                        return;
                    }
                    GabrielData.GuildData data = GabrielData.guilds().get().get(event.getGuild().getId());

                    if(args[0].equals("ls") || args[0].equals("list")) {
                        if(data == null || data.customCommands.isEmpty()) {
                            event.getChannel().sendMessage(
                                    getString(event.getGuild(), CustomCommands.NO_COMMANDS, "This guild has no custom commands")
                            ).queue();
                            return;
                        }
                        event.getChannel().sendMessage(new EmbedBuilder()
                                .setDescription(
                                        data.customCommands.keySet().stream().map(name->'`' + name + '`').collect(Collectors.joining(", "))
                                )
                                .build()
                        ).queue();
                        return;
                    }

                    if(args.length == 1) {
                        thiz.onHelp(event);
                        return;
                    }

                    switch(args[0].toLowerCase()) {
                        case "add":
                            if(args.length < 3) {
                                thiz.onHelp(event);
                                return;
                            }
                            if(data == null) {
                                GabrielData.guilds().get().set(event.getGuild().getId(), data = new GabrielData.GuildData());
                            }
                            String name = args[1];
                            if(GabrielBot.getInstance().registry.commands().containsKey(name)) {
                                event.getChannel().sendMessage(
                                        getString(event.getGuild(), CustomCommands.NAME_CONFLICT_DEFAULT, "A regular command with that name already exists")
                                ).queue();
                                return;
                            }
                            String s = data.customCommands.putIfAbsent(name, String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                            if(s == null) {
                                event.getChannel().sendMessage(
                                        getString(event.getGuild(), CustomCommands.ADDED_SUCCESSFULLY, "Added successfully")
                                ).queue();
                            } else {
                                event.getChannel().sendMessage(
                                        getString(event.getGuild(), CustomCommands.COMMAND_EXISTS, "A command with that name already exists")
                                ).queue();
                            }
                            break;
                        case "remove":
                            if(data == null) {
                                event.getChannel().sendMessage(
                                        getString(event.getGuild(), CustomCommands.NO_COMMANDS, "This guild has no custom commands")
                                ).queue();
                                return;
                            }
                            if(data.customCommands.remove(args[1]) != null) {
                                event.getChannel().sendMessage(
                                        getString(event.getGuild(), CustomCommands.REMOVED_SUCCESSFULLY, "Removed successfully")
                                ).queue();
                            } else {
                                event.getChannel().sendMessage(
                                        getString(event.getGuild(), CustomCommands.COMMAND_NOT_FOUND, "Command not found")
                                ).queue();
                            }
                            break;
                        case "raw":
                            if(data == null) {
                                event.getChannel().sendMessage(
                                        getString(event.getGuild(), CustomCommands.NO_COMMANDS, "This guild has no custom commands")
                                ).queue();
                                return;
                            }
                            String raw = data.customCommands.get(args[1]);
                            if(raw == null) {
                                event.getChannel().sendMessage(
                                        getString(event.getGuild(), CustomCommands.COMMAND_NOT_FOUND, "Command not found")
                                ).queue();
                            } else {
                                event.getChannel().sendMessage(new EmbedBuilder()
                                        .setDescription(raw)
                                        .build()
                                ).queue();
                            }
                            break;
                        case "rename":
                            if(args.length < 3) {
                                thiz.onHelp(event);
                                return;
                            }
                            if(data == null) {
                                event.getChannel().sendMessage(
                                        getString(event.getGuild(), CustomCommands.NO_COMMANDS, "This guild has no custom commands")
                                ).queue();
                                return;
                            }
                            String cmd = data.customCommands.remove(args[1]);
                            if(cmd == null) {
                                event.getChannel().sendMessage(
                                        getString(event.getGuild(), CustomCommands.COMMAND_NOT_FOUND, "Command not found")
                                ).queue();
                                return;
                            }
                            if(data.customCommands.get(args[2]) != null) {
                                event.getChannel().sendMessage(
                                        getString(event.getGuild(), CustomCommands.NAME_CONFLICT, "There is already a custom command with the new name")
                                ).queue();
                                return;
                            }
                            data.customCommands.put(args[2], cmd);
                            event.getChannel().sendMessage(
                                    getString(event.getGuild(), CustomCommands.RENAME_SUCCESS, "Renamed successfully")
                            ).queue();
                            break;
                        default:
                            thiz.onHelp(event);
                            break;
                    }
                })
                .build()
        );
    }
}
