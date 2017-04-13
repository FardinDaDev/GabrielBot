package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandPermission;
import br.net.brjdevs.natan.gabrielbot.core.command.CommandRegistry;
import br.net.brjdevs.natan.gabrielbot.core.command.RegisterCommand;
import br.net.brjdevs.natan.gabrielbot.core.command.SimpleCommand;
import br.net.brjdevs.natan.gabrielbot.utils.PrologBuilder;
import br.net.brjdevs.natan.gabrielbot.utils.stats.MessageStats;
import net.dv8tion.jda.core.JDAInfo;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@RegisterCommand.Class
public class MiscCommands {
    @RegisterCommand
    public static void stats(CommandRegistry registry) {
        registry.register("stats", SimpleCommand.builder()
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
        registry.register("ping", SimpleCommand.builder()
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
