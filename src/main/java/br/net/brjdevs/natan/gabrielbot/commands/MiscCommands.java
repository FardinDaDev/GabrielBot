package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import br.net.brjdevs.natan.gabrielbot.core.command.*;
import br.net.brjdevs.natan.gabrielbot.utils.PrologBuilder;
import br.net.brjdevs.natan.gabrielbot.utils.stats.AsyncInfoMonitor;
import br.net.brjdevs.natan.gabrielbot.utils.stats.MessageStats;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDAInfo;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static br.net.brjdevs.natan.gabrielbot.core.localization.LocalizationManager.*;

@RegisterCommand.Class
public class MiscCommands {
    static {
        AsyncInfoMonitor.start();
    }

    @RegisterCommand
    public static void help(CommandRegistry cr) {
        cr.register("help", SimpleCommand.builder(CommandCategory.MISC)
                .description("help", "Shows this screen")
                .help((thiz, event)->thiz.helpEmbed(event, "help",
                        "`>>help`: Lists all commands\n`>>help <command>`: Shows help for the specified command"
                ))
                .code((event, args)->{
                    if(args.length > 0) {
                        Command cmd = GabrielBot.getInstance().registry.commands().get(args[0]);
                        if(cmd == null || cmd.isHiddenFromHelp()) {
                            event.getChannel().sendMessage(
                                    getString(event.getGuild(), COMMAND_NOT_FOUND, "No command named $cmd$").replace("$cmd$", args[0])
                            ).queue();
                            return;
                        }

                        event.getChannel().sendMessage(cmd.help(event)).queue();
                        return;
                    }
                    EnumMap<CommandCategory, List<Pair<String, String>>> cmds = new EnumMap<>(CommandCategory.class);
                    GabrielBot.getInstance().registry.commands().forEach((name, command)->{
                        if(command.permission().test(event.getMember()) && !command.isHiddenFromHelp())
                            cmds.computeIfAbsent(command.category(), ignored->new ArrayList<>()).add(new ImmutablePair<>(name, command.description(event)));
                    });
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setDescription(
                            getString(event.getGuild(), HELP_LIST_DESCRIPTION, "Command help. For extended usage please use >>help <command>")
                    );
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
                .description("stats", "Shows info about me")
                .help((thiz, event)-> thiz.helpEmbed(event, "stats",
                        "`>>stats`"
                ))
                .code((event, args)->{
                    int connectedShards, totalShards;
                    {
                        int[] i = new int[2];
                        Arrays.stream(GabrielBot.getInstance().getShards()).forEach(shard->{
                            i[0]++;
                            if(shard.getJDA().getStatus() == JDA.Status.CONNECTED) i[1]++;
                        });
                        totalShards = i[0];
                        connectedShards = i[1];
                    }
                    PrologBuilder pb = new PrologBuilder();
                    pb.addLabel("Info")
                            .addField("Uptime", formatUptime(ManagementFactory.getRuntimeMXBean().getUptime()))
                            .addField("RAM", (ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()>>20) + " MB/" + (ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax()>>20) + " MB")
                            .addField("Threads", ManagementFactory.getThreadMXBean().getThreadCount())
                            .addField("CPU Cores", Runtime.getRuntime().availableProcessors())
                            .addField("CPU Usage", AsyncInfoMonitor.getCpuUsage())
                            .addField("JDA Version", JDAInfo.VERSION)
                            .addField("API Responses", Arrays.stream(GabrielBot.getInstance().getShards()).mapToLong(s->s.getJDA().getResponseTotal()).sum());
                    pb.addEmptyLine().addLabel("General")
                            .addField("Guilds", GabrielBot.getInstance().getGuilds().size())
                            .addField("Users", GabrielBot.getInstance().getUsers().size())
                            .addField("Text Channels", GabrielBot.getInstance().getTextChannels().size())
                            .addField("Voice Channels", GabrielBot.getInstance().getVoiceChannels().size())
                            .addField("Received Messages", MessageStats.getMessages())
                            .addField("Executed Commands", MessageStats.getCommands())
                            .addField("Shards (C/T)", connectedShards + "/" + totalShards);
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
                .description("ping", "Shows my ping")
                .help((thiz, event)->thiz.helpEmbed(event, "ping",
                        "`>>ping`"
                ))
                .code((event, args)->{
                    long now = System.currentTimeMillis();
                    event.getChannel().sendTyping().queue(done->{
                        long apiPing = System.currentTimeMillis() - now;
                        event.getChannel().sendMessage(String.format("API Ping: %d ms\nWS Ping: %d ms", apiPing, event.getJDA().getPing())).queue();
                    });
                })
                .build());
    }

    @RegisterCommand
    public static void reg(CommandRegistry cr) {
        cr.register("reg", SimpleCommand.builder(CommandCategory.FUN)
                .description("reg", "Converts text to regional indicators")
                .help((thiz, event)->thiz.helpEmbed(event, "reg",
                        "`>>reg <text>`"
                ))
                .code((thiz, event, args)->{
                    if(args.length == 0) {
                        thiz.onHelp(event);
                        return;
                    }
                    StringBuilder sb = new StringBuilder();
                    for(char c : String.join(" ", args).toLowerCase().toCharArray()) {
                        if(c >= 'a' && c <= 'z') {
                            sb.append(":regional_indicator_").append(c).append(":  ");
                            continue;
                        }
                        switch(c) {
                            case '0':
                                sb.append(":zero:  ");
                                break;
                            case '1':
                                sb.append(":one:  ");
                                break;
                            case '2':
                                sb.append(":two:  ");
                                break;
                            case '3':
                                sb.append(":three:  ");
                                break;
                            case '4':
                                sb.append(":four:  ");
                                break;
                            case '5':
                                sb.append(":five:  ");
                                break;
                            case '6':
                                sb.append(":six:  ");
                                break;
                            case '7':
                                sb.append(":seven:  ");
                                break;
                            case '8':
                                sb.append(":eight:  ");
                                break;
                            case '9':
                                sb.append(":nine:  ");
                                break;
                            case '!':
                                sb.append(":exclamation:  ");
                                break;
                            case '?':
                                sb.append(":question:  ");
                                break;
                            case '+':
                                sb.append(":heavy_plus_sign:  ");
                                break;
                            case '-':
                                sb.append(":heavy_minus_sign:  ");
                                break;
                            case '$':
                                sb.append(":heavy_dollar_sign:  ");
                                break;
                            default:
                                sb.append(":interrobang:  ");
                                break;
                        }
                    }
                    String s = sb.toString();
                    if(s.length() > 1990) {
                        event.getChannel().sendMessage("This message is too big to send. Sorry :(").queue();
                    } else {
                        event.getChannel().sendMessage(s).queue();
                    }
                })
                .build());
    }
}
