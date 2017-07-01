package gabrielbot.commands;

import gabrielbot.GabrielBot;
import gabrielbot.core.command.Argument;
import gabrielbot.core.command.Command;
import gabrielbot.core.command.CommandCategory;
import gabrielbot.core.command.CommandPermission;
import gabrielbot.core.command.CommandReference;
import gabrielbot.core.listeners.operations.Operation;
import gabrielbot.core.listeners.operations.ReactionOperation;
import gabrielbot.core.listeners.operations.ReactionOperations;
import gabrielbot.utils.PrologBuilder;
import gabrielbot.utils.commands.EmoteReference;
import gabrielbot.utils.stats.AsyncInfoMonitor;
import gabrielbot.utils.stats.MessageStats;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
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

@SuppressWarnings("unused")
public class MiscCommands {
    static {
        AsyncInfoMonitor.start();
    }

    @Command(
            name = "help",
            description = "Shows this screen",
            usage = "`>>help`: Lists all commands\n" +
                    "`>>help <command>`: Shows help for the specified command",
            permission = CommandPermission.USER,
            category = CommandCategory.MISC
    )
    public static void help(@Argument("event") GuildMessageReceivedEvent event, @Argument("args") String[] args, @Argument("channel") TextChannel channel) {
        if (args.length > 0) {
            CommandReference cmd = GabrielBot.getInstance().registry.commands().get(args[0]);
            if (cmd == null || cmd.command.isHiddenFromHelp()) {
                channel.sendMessage("No command named " + args[0]).queue();
                return;
            }
            cmd.onHelp(event);
            return;
        }
        EnumMap<CommandCategory, List<Pair<String, String>>> cmds = new EnumMap<>(CommandCategory.class);
        GabrielBot.getInstance().registry.commands().forEach((name, ref) -> {
            Command command = ref.command;
            if (command.permission().test(event.getMember()) && !command.isHiddenFromHelp())
                cmds.computeIfAbsent(command.category(), ignored -> new ArrayList<>()).add(new ImmutablePair<>(name, command.description()));
        });
        if (!event.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_ADD_REACTION)) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setDescription("Command help. For extended usage please use >>help <command>");
            cmds.forEach((category, names) -> {
                String name = category.name().toLowerCase();
                name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                eb.addField(name, names.stream().map(c -> "`" + c.getLeft() + "`: " + c.getRight()).collect(Collectors.joining("\n")), false);
            });
            channel.sendMessage(eb.build()).queue();
            return;
        }

        Map<String, Pair<String, MessageEmbed>> embeds = new HashMap<>();
        cmds.entrySet().forEach(e -> {
            String name = e.getKey().name().toLowerCase();
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
            EmbedBuilder eb = new EmbedBuilder().setTitle(name + " Commands").setDescription("\u200D");
            e.getValue().forEach(p -> eb.addField(p.getKey(), p.getValue(), false));
            embeds.put(e.getKey().emote.toString().replace(" ", ""), new ImmutablePair<>(name, eb.build()));
        });
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Help").setFooter("Requested by " + event.getAuthor().getName(), null).appendDescription("Only " + event.getAuthor().getName() + "'s reactions will be considered\n\n");
        embeds.entrySet().forEach(e ->
            eb.appendDescription(e.getKey() + " -> " + e.getValue().getKey() + " Commands\n")
        );
        Message m = channel.sendMessage(eb.build()).complete();
        ReactionOperation op;
        if (!event.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_MANAGE)) {
            AtomicReference<ReactionOperation> back = new AtomicReference<>();
            op = (e) -> {
                if (e.getUser().getIdLong() != event.getAuthor().getIdLong()) return Operation.IGNORED;
                Pair<String, MessageEmbed> p = embeds.get(e.getReactionEmote().getName());
                if (p != null) {
                    e.getTextChannel().deleteMessageById(e.getMessageIdLong()).queue();
                    e.getChannel().sendMessage(p.getValue()).queue(msg ->
                        ReactionOperations.create(msg, 30, back.get(), "\u2b05")
                    );
                    return Operation.COMPLETED;
                }
                return Operation.IGNORED;
            };
            back.set((e) -> {
                if (e.getUser().getIdLong() != event.getAuthor().getIdLong()) return Operation.IGNORED;
                if (e.getReactionEmote().getName().equals("\u2b05")) {
                    e.getTextChannel().deleteMessageById(e.getMessageIdLong()).queue();
                    Message msg = e.getChannel().sendMessage(eb.build()).complete();
                    ReactionOperations.create(msg, 30, op, embeds.keySet().toArray(new String[0]));
                    return Operation.COMPLETED;
                }
                return Operation.IGNORED;
            });
            ReactionOperations.create(m, 30, op, embeds.keySet().toArray(new String[0]));
        } else {
            AtomicReference<Future<Void>> future = new AtomicReference<>();
            AtomicReference<ReactionOperation> back = new AtomicReference<>();
            op = (e) -> {
                if (e.getUser().getIdLong() != event.getAuthor().getIdLong()) return Operation.IGNORED;
                Emote emote = e.getReactionEmote().getEmote();
                String s = emote == null ? e.getReactionEmote().getName() : emote.getAsMention();
                Pair<String, MessageEmbed> p = embeds.get(s);
                if (p != null) {
                    future.get().cancel(true);
                    m.editMessage(p.getValue()).queueAfter(250, TimeUnit.MILLISECONDS, done1->
                        m.clearReactions().queue(done2->
                            future.set(ReactionOperations.create(m, 30, back.get(), "\u2b05"))
                        )
                    );
                    return Operation.COMPLETED;
                }
                return Operation.IGNORED;
            };
            back.set((e) -> {
                if (e.getUser().getIdLong() != event.getAuthor().getIdLong()) return Operation.IGNORED;
                if (e.getReactionEmote().getName().equals("\u2b05")) {
                    future.get().cancel(true);
                    m.editMessage(eb.build()).queueAfter(250, TimeUnit.MILLISECONDS, done1->
                        m.clearReactions().queue(done2->
                            future.set(ReactionOperations.create(m, 30, op, embeds.keySet().toArray(new String[0])))
                        )
                    );
                    return Operation.COMPLETED;
                }
                return Operation.IGNORED;
            });
            future.set(ReactionOperations.create(m, 30, op, embeds.keySet().toArray(new String[0])));
        }
    }

    @Command(
            name = "stats",
            description = "Shows info about me",
            usage = "`>>stats`",
            permission = CommandPermission.USER,
            category = CommandCategory.INFO
    )
    public static void stats(@Argument("channel") TextChannel channel) {
        int connectedShards, totalShards; {
            int[] i = new int[2];
            Arrays.stream(GabrielBot.getInstance().getShards()).forEach(shard -> {
                i[0]++;
                if (shard.getJDA().getStatus() == JDA.Status.CONNECTED) i[1]++;
            });
            totalShards = i[0];
            connectedShards = i[1];
        }
        int players, queueSize; {
            int[] i = new int[2];
            GabrielBot.getInstance().getPlayers().forEachValue(p -> {
                i[0]++;
                i[1] += p.scheduler.tracks().size();
                return true;
            });
            players = i[0];
            queueSize = i[1];
        }
        PrologBuilder pb = new PrologBuilder();
        pb.addLabel("Info")
                .addField("Uptime", formatUptime(ManagementFactory.getRuntimeMXBean().getUptime()))
                .addField("RAM", (ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() >> 20) + " MB/" + (ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax() >> 20) + " MB")
                .addField("Threads", ManagementFactory.getThreadMXBean().getThreadCount())
                .addField("CPU Cores", Runtime.getRuntime().availableProcessors())
                .addField("CPU Usage", String.format("%.04f%%", AsyncInfoMonitor.getCpuUsage()))
                .addField("JDA Version", JDAInfo.VERSION)
                .addField("API Responses", Arrays.stream(GabrielBot.getInstance().getShards()).mapToLong(s -> s.getJDA().getResponseTotal()).sum());
        pb.addEmptyLine().addLabel("General")
                .addField("Guilds", GabrielBot.getInstance().streamGuilds().count())
                .addField("Users", GabrielBot.getInstance().streamUsers().count())
                .addField("Text Channels", GabrielBot.getInstance().streamTextChannels().count())
                .addField("Voice Channels", GabrielBot.getInstance().streamVoiceChannels().count())
                .addField("Received Messages", MessageStats.getMessages())
                .addField("Executed Commands", MessageStats.getCommands())
                .addField("Shards (C/T)", connectedShards + "/" + totalShards);
        pb.addEmptyLine().addLabel("Music")
                .addField("Connections", players)
                .addField("Queue Size", queueSize);
        channel.sendMessage(pb.build()).queue();
    }

    private static String formatUptime(long time) {
        long days = TimeUnit.MILLISECONDS.toDays(time);
        long hours = TimeUnit.MILLISECONDS.toHours(time) % TimeUnit.DAYS.toHours(1);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(time) % TimeUnit.HOURS.toMinutes(1);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(time) % TimeUnit.MINUTES.toSeconds(1);
        return ((days == 0 ? "" : days + " day" + (days == 1 ? "" : "s") + ", ") +
                (hours == 0 ? "" : hours + " hour" + (hours == 1 ? "" : "s") + ", ") +
                (minutes == 0 ? "" : minutes + " minute" + (minutes == 1 ? "" : "s") + ", ") +
                (seconds == 0 ? "" : seconds + " second" + (seconds == 1 ? "" : "s"))).replaceAll(", $", "");
    }

    @Command(
            name = "ping",
            description = "Shows my ping",
            usage = "`>>ping`",
            permission = CommandPermission.USER,
            category = CommandCategory.INFO
    )
    public static void ping(@Argument("channel") TextChannel channel, @Argument("jda") JDA jda) {
        long now = System.currentTimeMillis();
        channel.sendTyping().queue(done -> {
            long apiPing = System.currentTimeMillis() - now;
            long heartbeat = jda.getPing();
            channel.sendMessageFormat("%sAPI Ping: %d ms - %s\n%sWebsocket Ping: %d ms - %s",
                    EmoteReference.PING_PONG,
                    apiPing,
                    ratePing(apiPing),
                    EmoteReference.HEARTBEAT,
                    heartbeat,
                    ratePing(heartbeat)
            ).queue();
        });
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
}
