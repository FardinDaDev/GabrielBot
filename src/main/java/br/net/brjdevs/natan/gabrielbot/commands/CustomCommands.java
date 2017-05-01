package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import br.net.brjdevs.natan.gabrielbot.core.command.*;
import br.net.brjdevs.natan.gabrielbot.core.data.GabrielData;
import br.net.brjdevs.natan.gabrielbot.core.localization.LocalizationManager;
import net.dv8tion.jda.core.EmbedBuilder;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static br.net.brjdevs.natan.gabrielbot.core.localization.LocalizationManager.*;

@RegisterCommand.Class
public class CustomCommands {
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
                    GabrielData.GuildCommandData commandData = GabrielData.guildCommands().get().get(event.getGuild().getId());

                    if(args[0].equals("ls") || args[0].equals("list")) {
                        if(commandData == null || commandData.custom.isEmpty()) {
                            event.getChannel().sendMessage(
                                    getString(event.getGuild(), LocalizationManager.CustomCommands.NO_COMMANDS, "This guild has no custom commands")
                            ).queue();
                            return;
                        }
                        event.getChannel().sendMessage(new EmbedBuilder()
                                .setDescription(
                                        commandData.custom.stream().map(name->'`' + name + '`').collect(Collectors.joining(", "))
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
                            if(commandData == null) {
                                GabrielData.guildCommands().get().set(event.getGuild().getId(), commandData = new GabrielData.GuildCommandData());
                            }
                            String name = args[1];
                            if(GabrielBot.getInstance().registry.commands().containsKey(name)) {
                                event.getChannel().sendMessage(
                                        getString(event.getGuild(), LocalizationManager.CustomCommands.NAME_CONFLICT_DEFAULT, "A regular command with that name already exists")
                                ).queue();
                                return;
                            }
                            br.net.brjdevs.natan.gabrielbot.commands.custom.CustomCommand s = data.customCommands.putIfAbsent(name, br.net.brjdevs.natan.gabrielbot.commands.custom.CustomCommand.of(String.join(" ", Arrays.copyOfRange(args, 2, args.length))));
                            commandData.custom.add(name);
                            if(s == null) {
                                event.getChannel().sendMessage(
                                        getString(event.getGuild(), LocalizationManager.CustomCommands.ADDED_SUCCESSFULLY, "Added successfully")
                                ).queue();
                            } else {
                                event.getChannel().sendMessage(
                                        getString(event.getGuild(), LocalizationManager.CustomCommands.COMMAND_EXISTS, "A command with that name already exists")
                                ).queue();
                            }
                            break;
                        case "remove":
                            if(commandData == null) {
                                event.getChannel().sendMessage(
                                        getString(event.getGuild(), LocalizationManager.CustomCommands.NO_COMMANDS, "This guild has no custom commands")
                                ).queue();
                                return;
                            }
                            commandData.custom.remove(args[1]);
                            if(data.customCommands.remove(args[1]) != null) {
                                event.getChannel().sendMessage(
                                        getString(event.getGuild(), LocalizationManager.CustomCommands.REMOVED_SUCCESSFULLY, "Removed successfully")
                                ).queue();
                            } else {
                                event.getChannel().sendMessage(
                                        getString(event.getGuild(), LocalizationManager.CustomCommands.COMMAND_NOT_FOUND, "Command not found")
                                ).queue();
                            }
                            break;
                        case "raw":
                            if(data == null) {
                                event.getChannel().sendMessage(
                                        getString(event.getGuild(), LocalizationManager.CustomCommands.NO_COMMANDS, "This guild has no custom commands")
                                ).queue();
                                return;
                            }
                            br.net.brjdevs.natan.gabrielbot.commands.custom.CustomCommand raw = data.customCommands.get(args[1]);
                            if(raw == null) {
                                event.getChannel().sendMessage(
                                        getString(event.getGuild(), LocalizationManager.CustomCommands.COMMAND_NOT_FOUND, "Command not found")
                                ).queue();
                            } else {
                                event.getChannel().sendMessage(new EmbedBuilder()
                                        .setDescription(raw.getRaw())
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
                                        getString(event.getGuild(), LocalizationManager.CustomCommands.NO_COMMANDS, "This guild has no custom commands")
                                ).queue();
                                return;
                            }
                            br.net.brjdevs.natan.gabrielbot.commands.custom.CustomCommand cmd = data.customCommands.remove(args[1]);
                            if(cmd == null) {
                                event.getChannel().sendMessage(
                                        getString(event.getGuild(), LocalizationManager.CustomCommands.COMMAND_NOT_FOUND, "Command not found")
                                ).queue();
                                return;
                            }
                            if(data.customCommands.get(args[2]) != null) {
                                data.customCommands.put(args[1], cmd);
                                event.getChannel().sendMessage(
                                        getString(event.getGuild(), LocalizationManager.CustomCommands.NAME_CONFLICT, "There is already a custom command with the new name")
                                ).queue();
                                return;
                            }
                            data.customCommands.put(args[2], cmd);
                            commandData.custom.remove(args[1]);
                            commandData.custom.add(args[2]);
                            event.getChannel().sendMessage(
                                    getString(event.getGuild(), LocalizationManager.CustomCommands.RENAME_SUCCESS, "Renamed successfully")
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
