package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import br.net.brjdevs.natan.gabrielbot.core.command.*;
import br.net.brjdevs.natan.gabrielbot.utils.PrologBuilder;
import br.net.brjdevs.natan.gabrielbot.utils.stats.MessageStats;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDAInfo;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RegisterCommand.Class
public class MiscCommands {
    @RegisterCommand
    public static void register(CommandRegistry registry) {
        registry.register("help", SimpleCommand.builder(CommandCategory.MISC)
                .description("Shows this screen")
                .permission(CommandPermission.USER)
                .help(SimpleCommand.helpEmbed("help", CommandPermission.USER,
                        "Shows help about a command (What did you expect?!)",
                        "`>>help`: Lists all commands\n`>>help <command>`: Shows help for the specified command"
                ))
                .code((event, args)->{
                    if(args.length > 0) {
                        Command cmd = GabrielBot.getInstance().registry.commands().get(args[0]);
                        if(cmd == null || cmd.isHiddenFromHelp()) {
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
                    EnumMap<CommandCategory, List<Pair<String, String>>> cmds = new EnumMap<>(CommandCategory.class);
                    GabrielBot.getInstance().registry.commands().forEach((name, command)->{
                        if(command.permission().test(event.getGuild(), event.getMember()) && !command.isHiddenFromHelp())
                            cmds.computeIfAbsent(command.category(), ignored->new ArrayList<>()).add(new ImmutablePair<>(name, command.description()));
                    });
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setDescription("Command help. For extended usage please use >>help <command>");
                    cmds.forEach((category, names)->{
                        String name = category.name().toLowerCase();
                        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                        eb.addField(name, names.stream().map(c->"`" + c.getLeft() + "`: " + c.getRight()).collect(Collectors.joining("\n")), false);
                    });
                    event.getChannel().sendMessage(eb.build()).queue();
                })
                .build());
    }

    @RegisterCommand
    public static void stats(CommandRegistry registry) {
        registry.register("stats", SimpleCommand.builder(CommandCategory.INFO)
                .permission(CommandPermission.USER)
                .description("Shows info about me")
                .help(SimpleCommand.helpEmbed("stats", CommandPermission.USER,
                        "Shows info about me",
                        "`>>stats`"
                ))
                .code((event, args)->{
                    PrologBuilder pb = new PrologBuilder();
                    pb.addLabel("Info")
                            .addField("Uptime", formatUptime(ManagementFactory.getRuntimeMXBean().getUptime()))
                            .addField("RAM", (ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()>>20) + " MB/" + (ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax()>>20) + " MB")
                            .addField("Threads", ManagementFactory.getThreadMXBean().getThreadCount())
                            .addField("CPU Cores", Runtime.getRuntime().availableProcessors())
                            .addField("JAD Version", JDAInfo.VERSION)
                            .addField("API Responses", Arrays.stream(GabrielBot.getInstance().getShards()).mapToLong(s->s.getJDA().getResponseTotal()).sum());
                    pb.addEmptyLine().addLabel("General")
                            .addField("Guilds", GabrielBot.getInstance().getGuilds().size())
                            .addField("Users", GabrielBot.getInstance().getUsers().size())
                            .addField("Text Channels", GabrielBot.getInstance().getTextChannels().size())
                            .addField("Voice Channels", GabrielBot.getInstance().getVoiceChannels().size())
                            .addField("Received Messages", MessageStats.getMessages())
                            .addField("Executed Commands", MessageStats.getCommands());
                    event.getChannel().sendMessage(pb.build()).queue();
                })
                .build());
    }

    private static String formatUptime(long time) {
        long days = TimeUnit.MILLISECONDS.toDays(time);
        long hours = TimeUnit.MILLISECONDS.toHours(time) % TimeUnit.DAYS.toHours(1);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(time) % TimeUnit.HOURS.toMinutes(1);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(time) % TimeUnit.MINUTES.toSeconds(1);
        return ((days == 0 ? "" : days + " days, ") +
                (hours == 0 ? "" : hours + " hours, ") +
                (minutes == 0 ? "" : minutes + " minutes, ") +
                (seconds == 0 ? "" : seconds + " seconds")).replaceAll(",\\s$", "");
    }

    @RegisterCommand
    public static void ping(CommandRegistry registry) {
        registry.register("ping", SimpleCommand.builder(CommandCategory.INFO)
                .permission(CommandPermission.USER)
                .description("Shows my ping")
                .help(SimpleCommand.helpEmbed("ping", CommandPermission.USER,
                        "Shows my ping",
                        "`>>ping`"
                ))
                .code((event, args)->{
                    long now = System.currentTimeMillis();
                    event.getChannel().sendTyping().queue(done->{
                        long apiPing = System.currentTimeMillis() - now;
                        event.getChannel().sendMessage(String.format("API Ping: %d\nWS Ping: %d", apiPing, event.getJDA().getPing())).queue();
                    });
                })
                .build());
    }
}
