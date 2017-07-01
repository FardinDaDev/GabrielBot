package gabrielbot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import gabrielbot.GabrielBot;
import gabrielbot.core.command.Argument;
import gabrielbot.core.command.Command;
import gabrielbot.core.command.CommandCategory;
import gabrielbot.core.command.CommandPermission;
import gabrielbot.core.command.CommandReference;
import gabrielbot.core.data.Config;
import gabrielbot.core.data.GabrielData;
import gabrielbot.music.GuildMusicPlayer;
import gabrielbot.music.Track;
import gabrielbot.utils.DiscordUtils;
import gabrielbot.utils.Utils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.awt.Color;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

@SuppressWarnings("unused")
public class MusicCommands {
    @Command(
            name = "play",
            description = "Plays music",
            usage = "`>>play <name>`: searches for the given name on youtube\n" +
                    "`>>play soundcloud <name>`: searches for the given name on soundcloud\n" +
                    "`>>play <url>`: plays the given video/playlist",
            permission = CommandPermission.USER,
            category = CommandCategory.MUSIC
    )
    public static void play(@Argument("this") CommandReference thiz, @Argument("event")GuildMessageReceivedEvent event, @Argument("args") String[] args, @Argument("channel") TextChannel channel, @Argument("guild") Guild guild) {
        if(checkEnabled(event)) return;
        if(args.length == 0) {
            thiz.onHelp(event);
            return;
        }

        if(checkVC(event)) return;

        String identifier;
        try {
            new URL(args[0]);
            identifier = args[0];
        } catch(MalformedURLException e) {
            if(args[0].equals("soundcloud")) {
                if(args.length == 1) {
                    thiz.onHelp(event);
                    return;
                }
                identifier = "scsearch:" + String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            } else {
                identifier = "ytsearch:" + String.join(" ", args);
            }
        }

        GuildMusicPlayer gmp = GabrielBot.getInstance().createPlayer(guild.getIdLong(), channel.getIdLong(), event.getMember().getVoiceState().getChannel().getIdLong());

        gmp.loader.load(event, identifier);
    }
    /* TODO: implement these:
        move
        pause
        queue
        removetrack
     */

    @Command(
            name = "queue",
            description = "Shows the songs on the queue",
            usage = "`>>queue`",
            permission = CommandPermission.USER,
            category = CommandCategory.MUSIC
    )
    public static void queue(@Argument("event")GuildMessageReceivedEvent event, @Argument("channel") TextChannel channel, @Argument("guild") Guild guild) {
        GuildMusicPlayer gmp = GabrielBot.getInstance().getPlayer(guild.getIdLong());
        if(gmp == null) {
            channel.sendMessage(new EmbedBuilder().setTitle("Queue for server " + guild.getName()).setDescription("Nothing playing").build()).queue();
            return;
        }
        String[] tracks = gmp.scheduler.tracks().stream()
                .map(t->{
                    Member m = guild.getMemberById(t.dj);
                    String dj = m == null ? "Unknown" : m.getUser().getName();
                    AudioTrackInfo info = t.track.getInfo();
                    return "[" + info.title.replace("]", "\\]") + "](" + info.uri + ") [" + dj + "]";
                }).toArray(String[]::new);
        if(tracks.length == 0) {
            channel.sendMessage(new EmbedBuilder().setTitle("Queue for server " + guild.getName()).setDescription("Nothing playing").build()).queue();
            return;
        }
        DiscordUtils.list(event, 30, false, (page, total)->{
            AudioTrackInfo current = gmp.scheduler.currentTrack().track.getInfo();
            return new EmbedBuilder()
                    .setColor(Color.CYAN)
                    .setTitle("Queue for server " + guild.getName())
                    .setFooter("Page " + page + "/" + total, null)
                    .addField("Currently playing", "[" + current.title.replace("[", "\\[") + "](" + current.uri + ")", false)
                    .addField("Queue size", "`" + gmp.scheduler.tracks().size() + "`", true)
                    .addField("Queue duration", "`" + Utils.getDuration(gmp.scheduler.tracks().stream().mapToLong(t -> t.track.getDuration()).sum()) + "`", true);
            }, tracks
        );
    }

    @Command(
            name = "volume",
            description = "Changes volume for this server",
            usage = "`>>volume <new volume>`: Changes the volume\n" +
                    "`>>volume 100`: Resets the volume",
            permission = CommandPermission.PREMIUM,
            category = CommandCategory.MUSIC
    )
    public static void volume(@Argument("this") CommandReference thiz, @Argument("event")GuildMessageReceivedEvent event, @Argument("args") String[] args, @Argument("channel") TextChannel channel, @Argument("guild") Guild guild) {
        if(checkVC(event)) return;
        if(args.length == 0) {
            thiz.onHelp(event);
            return;
        }
        int i;
        try {
            i = Integer.parseInt(args[0]);
        } catch(NumberFormatException e) {
            channel.sendMessage(args[0] + " is not a valid integer").queue();
            return;
        }
        if(i < 0) {
            i = 100;
        } else if (i > 150) {
            i = 150;
        }
        GuildMusicPlayer gmp = GabrielBot.getInstance().getPlayer(guild.getIdLong());
        if(gmp == null) {
            channel.sendMessage("I'm not playing any music").queue();
            return;
        }
        gmp.player.setVolume(i);
        channel.sendMessage("Volume set to " + i).queue();
    }

    @Command(
            name = "skip",
            description = "Skips the current song",
            usage = "`>>skip`: Skips if you're the DJ, otherwise votes to skip",
            permission = CommandPermission.USER,
            category = CommandCategory.MUSIC
    )
    public static void skip(@Argument("event")GuildMessageReceivedEvent event, @Argument("channel") TextChannel channel, @Argument("guild") Guild guild) {
        if(checkVC(event)) return;
        GuildMusicPlayer gmp = GabrielBot.getInstance().getPlayer(guild.getIdLong());
        if(gmp == null) {
            channel.sendMessage("I'm not playing any song").queue();
            return;
        }
        Track current = gmp.scheduler.currentTrack();
        if(current.dj == event.getAuthor().getIdLong() || isDJ(event.getMember())) {
            gmp.textChannel.sendMessage("The DJ has decided to skip").queue();
            gmp.scheduler.nextTrack();
            return;
        }
        gmp.scheduler.voteskip(event.getAuthor().getIdLong());
    }

    @Command(
            name = "stop",
            description = "Stops the player",
            usage = "`>>stop`",
            permission = CommandPermission.USER,
            category = CommandCategory.MUSIC
    )
    public static void stop(@Argument("event")GuildMessageReceivedEvent event, @Argument("channel") TextChannel channel, @Argument("guild") Guild guild) {
        if(checkVC(event)) return;
        GuildMusicPlayer gmp = GabrielBot.getInstance().getPlayer(guild.getIdLong());
        if(gmp == null) {
            channel.sendMessage("I'm not playing any song").queue();
            return;
        }
        boolean dj = isDJ(event.getMember());
        if(dj) {
            gmp.textChannel.sendMessage("The DJ decided to stop").queue();
            GabrielBot.getInstance().interruptPlayer(gmp.guildId);
        } else {
            channel.sendMessage("You are not a DJ").queue();
        }
    }

    @Command(
            name = "np",
            description = "Shows currently playing music",
            usage = "`>>np`",
            permission = CommandPermission.USER,
            category = CommandCategory.MUSIC
    )
    public static void np(@Argument("event")GuildMessageReceivedEvent event, @Argument("channel") TextChannel channel, @Argument("guild") Guild guild) {
        GuildMusicPlayer gmp = GabrielBot.getInstance().getPlayer(guild.getIdLong());
        if(gmp == null) {
            channel.sendMessage("I'm not playing any song").queue();
            return;
        }
        Track current = gmp.scheduler.currentTrack();
        Member m = guild.getMemberById(current.dj);
        channel.sendMessage("Now playing `" + current.track.getInfo().title + "`, added by " + (m == null ? "<@" + current.dj + ">" : m.getEffectiveName())).queue();
    }

    private static boolean checkEnabled(GuildMessageReceivedEvent event) {
        Config config = GabrielData.config();
        if(config.music) return false;
        event.getChannel().sendMessage(config.musicDisableReason).queue();
        return true;
    }

    private static boolean isDJ(Member member) {
        return CommandPermission.ADMIN.test(member) || member.getRoles().stream().anyMatch(r -> r.getName().equals("DJ"));
    }

    private static boolean checkVC(GuildMessageReceivedEvent event) {
        VoiceChannel mc = event.getMember().getVoiceState().getChannel();
        VoiceChannel bc = event.getGuild().getSelfMember().getVoiceState().getChannel();
        if (mc == null) {
            event.getChannel().sendMessage(bc == null ?
                    "You must be connected to a voice channel" :
                    "You must be on the same voice channel as me"
            ).queue();
            return true;
        }
        if (bc == null) return false;
        if (bc.getIdLong() != mc.getIdLong()) {
            event.getChannel().sendMessage("You must be on the same voice channel as me").queue();
            return true;
        }
        return false;
    }
}
