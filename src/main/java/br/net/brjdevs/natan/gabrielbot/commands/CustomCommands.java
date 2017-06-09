package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import br.net.brjdevs.natan.gabrielbot.commands.custom.CustomCommand;
import br.net.brjdevs.natan.gabrielbot.core.command.Argument;
import br.net.brjdevs.natan.gabrielbot.core.command.Command;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandCategory;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandPermission;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandReference;
import br.net.brjdevs.natan.gabrielbot.core.data.GabrielData;
import br.net.brjdevs.natan.gabrielbot.utils.DiscordUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.Arrays;
import java.util.stream.Collectors;

public class CustomCommands {
    @Command(
            name = "custom",
            description = "Manages this guild's custom commands",
            usage = "`>>custom add <name> <response>`: adds a custom command\n" +
                    "`>>custom remove <name>`: removes a custom command\n" +
                    "`>>custom <ls|list>`: lists custom commands\n" +
                    "`>>custom raw <name>`: shows the response of a custom command\n" +
                    "`>>custom rename <name> <newname>`: renames a custom command",
            permission = CommandPermission.USER,
            category = CommandCategory.MISC,
            advancedSplit = false
    )
    public static void custom(@Argument("this") CommandReference thiz, @Argument("event") GuildMessageReceivedEvent event, @Argument("guild") Guild guild, @Argument("channel") TextChannel channel, @Argument("args") String[] args) {
        if (args.length == 0) {
            thiz.onHelp(event);
            return;
        }
        GabrielData.GuildData data = GabrielData.guilds().get().get(guild.getId());
        GabrielData.GuildCommandData commandData = GabrielData.guildCommands().get().get(guild.getId());

        if (args[0].equals("ls") || args[0].equals("list")) {
            if (commandData == null || commandData.custom.isEmpty()) {
                channel.sendMessage("This guild has no custom commands").queue();
                return;
            }
            String s = commandData.custom.stream().map(name -> '`' + name + '`').collect(Collectors.joining(", "));
            if(s.length() < 500) {
                channel.sendMessage(new EmbedBuilder().setDescription(s).setColor(event.getMember().getColor()).build()).queue();
                return;
            }
            int[] i = new int[1];
            DiscordUtils.list(event, 120, false, (page, total)->{
                return new EmbedBuilder()
                        .setTitle("Custom commands")
                        .setFooter("Times out in 2 minutes of inactivity", null)
                        .setColor(event.getMember().getColor());
            }, commandData.custom.stream().map(name -> i[0]++ + "- `" + name + '`').toArray(String[]::new));
            return;
        }

        if (args.length == 1) {
            thiz.onHelp(event);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "add":
                if (args.length < 3) {
                    thiz.onHelp(event);
                    return;
                }
                if (data == null) {
                    GabrielData.guilds().get().set(guild.getId(), data = new GabrielData.GuildData());
                }
                if (commandData == null) {
                    GabrielData.guildCommands().get().set(guild.getId(), commandData = new GabrielData.GuildCommandData());
                }
                String name = args[1];
                if (GabrielBot.getInstance().registry.commands().containsKey(name)) {
                    channel.sendMessage("A regular command with that name already exists").queue();
                    return;
                }
                CustomCommand c = CustomCommand.of(event, String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                if (c == null) return;
                CustomCommand s = data.customCommands.putIfAbsent(name, c);
                commandData.custom.add(name);
                if (s == null) {
                    channel.sendMessage("Added successfully").queue();
                } else {
                    channel.sendMessage("A command with that name already exists").queue();
                }
                break;
            case "remove":
                if (commandData == null) {
                    channel.sendMessage("This guild has no custom commands").queue();
                    return;
                }
                commandData.custom.remove(args[1]);
                if (data.customCommands.remove(args[1]) != null) {
                    channel.sendMessage("Removed successfully").queue();
                } else {
                    channel.sendMessage("Command not found").queue();
                }
                break;
            case "raw":
                if (data == null) {
                    channel.sendMessage("This guild has no custom commands").queue();
                    return;
                }
                CustomCommand raw = data.customCommands.get(args[1]);
                if (raw == null) {
                    channel.sendMessage("Command not found").queue();
                } else {
                    channel.sendMessage(new EmbedBuilder().setDescription("`" + raw.getRaw().replace("`", "\\`") + "`").build()).queue();
                }
                break;
            case "rename":
                if (args.length < 3) {
                    thiz.onHelp(event);
                    return;
                }
                if (data == null) {
                    channel.sendMessage("This guild has no custom commands").queue();
                    return;
                }
                CustomCommand cmd = data.customCommands.remove(args[1]);
                if (cmd == null) {
                    channel.sendMessage("Command not found").queue();
                    return;
                }
                if (data.customCommands.get(args[2]) != null) {
                    data.customCommands.put(args[1], cmd);
                    channel.sendMessage("There is already a custom command with the new name").queue();
                    return;
                }
                data.customCommands.put(args[2], cmd);
                commandData.custom.remove(args[1]);
                commandData.custom.add(args[2]);
                channel.sendMessage("Renamed successfully").queue();
                break;
            default:
                thiz.onHelp(event);
                break;
        }
    }
}
