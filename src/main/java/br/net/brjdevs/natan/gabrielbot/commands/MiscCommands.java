package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import br.net.brjdevs.natan.gabrielbot.core.command.Command;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandCategory;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandRegistry;
import br.net.brjdevs.natan.gabrielbot.core.command.RegisterCommand;
import br.net.brjdevs.natan.gabrielbot.core.command.SimpleCommand;
import br.net.brjdevs.natan.gabrielbot.core.listeners.operations.ReactionOperation;
import br.net.brjdevs.natan.gabrielbot.core.listeners.operations.ReactionOperations;
import br.net.brjdevs.natan.gabrielbot.utils.PrologBuilder;
import br.net.brjdevs.natan.gabrielbot.utils.commands.EmoteReference;
import br.net.brjdevs.natan.gabrielbot.utils.stats.AsyncInfoMonitor;
import br.net.brjdevs.natan.gabrielbot.utils.stats.MessageStats;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@RegisterCommand.Class
public class MiscCommands {
    static {
        AsyncInfoMonitor.start();
    }

    @RegisterCommand
    public static void help(CommandRegistry cr) {
        cr.register("help", SimpleCommand.builder(CommandCategory.MISC)
                .description("Shows this screen")
                .help((thiz, event)->thiz.helpEmbed(event, "help",
                        "`>>help`: Lists all commands\n`>>help <command>`: Shows help for the specified command"
                ))
                .code((event, args)->{
                    if(args.length > 0) {
                        Command cmd = GabrielBot.getInstance().registry.commands().get(args[0]);
                        if(cmd == null || cmd.isHiddenFromHelp()) {
                            event.getChannel().sendMessage("No command named " + args[0]).queue();
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
                    if(!event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION)) {
                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setDescription("Command help. For extended usage please use >>help <command>");
                        cmds.forEach((category, names)->{
                            String name = category.name().toLowerCase();
                            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                            eb.addField(name, names.stream().map(c->"`" + c.getLeft() + "`: " + c.getRight()).collect(Collectors.joining("\n")), false);
                        });
                        event.getChannel().sendMessage(eb.build()).queue();
                        return;
                    }

                    Map<String, Pair<String, MessageEmbed>> embeds = new HashMap<>();
                    cmds.entrySet().forEach(e->{
                        String name = e.getKey().name().toLowerCase();
                        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                        EmbedBuilder eb = new EmbedBuilder().setTitle(name + " Commands").setDescription("\u200D");
                        e.getValue().forEach(p->eb.addField(p.getKey(), p.getValue(), false));
                        embeds.put(e.getKey().emote.toString().replace(" ", ""), new ImmutablePair<>(name, eb.build()));
                    });
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle("Help").setFooter("Requested by " + event.getAuthor().getName(), null).appendDescription("Only " + event.getAuthor().getName() + "'s reactions will be considered\n\n");
                    embeds.entrySet().forEach(e->{
                        eb.appendDescription(e.getKey() + " -> " + e.getValue().getKey() + " Commands\n");
                    });
                    Message m = event.getChannel().sendMessage(eb.build()).complete();
                    ReactionOperation op;
                    if(!event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_MANAGE)) {
                        AtomicReference<ReactionOperation> back = new AtomicReference<>();
                        op = (e)->{
                            if(e.getUser().getIdLong() != event.getAuthor().getIdLong()) return false;
                            Pair<String, MessageEmbed> p = embeds.get(e.getReactionEmote().getName());
                            if(p != null) {
                                e.getTextChannel().deleteMessageById(e.getMessageIdLong()).queue();
                                e.getChannel().sendMessage(p.getValue()).queue(msg->{
                                    ReactionOperations.create(msg, 30, back.get(), "\u2b05");
                                });
                                return true;
                            }
                            return false;
                        };
                        back.set((e)->{
                            if(e.getUser().getIdLong() != event.getAuthor().getIdLong()) return false;
                            if(e.getReactionEmote().getName().equals("\u2b05")) {
                                e.getTextChannel().deleteMessageById(e.getMessageIdLong()).queue();
                                Message msg = e.getChannel().sendMessage(eb.build()).complete();
                                ReactionOperations.create(msg, 30, op, embeds.keySet().toArray(new String[0]));
                                return true;
                            }
                            return false;
                        });
                        ReactionOperations.create(m, 30, op, embeds.keySet().toArray(new String[0]));
                    } else {
                        AtomicReference<Future<Void>> future = new AtomicReference<>();
                        AtomicReference<ReactionOperation> back = new AtomicReference<>();
                        op = (e)->{
                            if(e.getUser().getIdLong() != event.getAuthor().getIdLong()) return false;
                            Emote emote = e.getReactionEmote().getEmote();
                            String s = emote == null ? e.getReactionEmote().getName() : emote.getAsMention();
                            Pair<String, MessageEmbed> p = embeds.get(s);
                            if(p != null) {
                                future.get().cancel(true);
                                m.editMessage(p.getValue()).queueAfter(250, TimeUnit.MILLISECONDS, done1->{
                                    m.clearReactions().queue(done2->{
                                        future.set(ReactionOperations.create(m, 30, back.get(), "\u2b05"));
                                    });
                                });
                                return true;
                            }
                            return false;
                        };
                        back.set((e)->{
                            if(e.getUser().getIdLong() != event.getAuthor().getIdLong()) return false;
                            if(e.getReactionEmote().getName().equals("\u2b05")) {
                                future.get().cancel(true);
                                m.editMessage(eb.build()).queueAfter(250, TimeUnit.MILLISECONDS, done1->{
                                    m.clearReactions().queue(done2->{
                                        future.set(ReactionOperations.create(m, 30, op, embeds.keySet().toArray(new String[0])));
                                    });
                                });
                                return true;
                            }
                            return false;
                        });
                        future.set(ReactionOperations.create(m, 30, op, embeds.keySet().toArray(new String[0])));
                    }
                })
                .build());
    }

    @RegisterCommand
    public static void stats(CommandRegistry registry) {
        registry.register("stats", SimpleCommand.builder(CommandCategory.INFO)
                .description("Shows info about me")
                .help((thiz, event)-> thiz.helpEmbed(event, "stats",
                        "`>>stats`"
                ))
                .code((event, args)->{
                    int connectedShards, totalShards; {
                        int[] i = new int[2];
                        Arrays.stream(GabrielBot.getInstance().getShards()).forEach(shard->{
                            i[0]++;
                            if(shard.getJDA().getStatus() == JDA.Status.CONNECTED) i[1]++;
                        });
                        totalShards = i[0];
                        connectedShards = i[1];
                    }
                    int players, queueSize; {
                        int[] i = new int[2];
                        GabrielBot.getInstance().streamPlayers().forEach(p->{
                            i[0]++;
                            i[1] += p.scheduler.tracks().size();
                        });
                        players = i[0];
                        queueSize = i[1];
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
                    pb.addEmptyLine().addLabel("Music")
                            .addField("Connections", players)
                            .addField("Queue Size", queueSize);
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
                .description("Shows my ping")
                .help((thiz, event)->thiz.helpEmbed(event, "ping",
                        "`>>ping`"
                ))
                .code((event, args)->{
                    long now = System.currentTimeMillis();
                    event.getChannel().sendTyping().queue(done->{
                        long apiPing = System.currentTimeMillis() - now;
                        long heartbeat = event.getJDA().getPing();
                        event.getChannel().sendMessageFormat("%sAPI Ping: %d ms - %s\n%sWebsocket Ping: %d ms - %s",
                                EmoteReference.PING_PONG,
                                apiPing,
                                ratePing(apiPing),
                                EmoteReference.HEARTBEAT,
                                heartbeat,
                                ratePing(heartbeat)
                        ).queue();
                    });
                })
                .build());
    }

    private static String ratePing(long ping) {
        if (ping <= 1) return "supersonic speed! :upside_down:"; //just in case...
        if (ping <= 10) return "faster than Sonic! :smiley:";
        if (ping <= 100) return "great! :smiley:";
        if (ping <= 200) return "nice! :slight_smile:";
        if (ping <= 300) return "decent. :neutral_face:";
        if (ping <= 400) return "average... :confused:";
        if (ping <= 500) return "slightly slow. :slight_frown:";
        if (ping <= 600) return "kinda slow.. :frowning2:";
        if (ping <= 700) return "slow.. :worried:";
        if (ping <= 800) return "too slow. :disappointed:";
        if (ping <= 800) return "bad. :weary:";
        if (ping <= 900) return "awful. :sob: (helpme)";
        if (ping <= 1600) return "#BlameDiscord. :angry:";
        if (ping <= 10000) return "this makes no sense :thinking: #BlameSteven";
        return "slow af. :dizzy_face:";
    }

    @RegisterCommand
    public static void reg(CommandRegistry cr) {
        cr.register("reg", SimpleCommand.builder(CommandCategory.FUN)
                .description("Converts text to regional indicators")
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
