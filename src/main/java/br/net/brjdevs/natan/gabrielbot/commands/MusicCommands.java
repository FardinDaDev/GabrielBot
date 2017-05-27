package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import br.net.brjdevs.natan.gabrielbot.core.command.*;
import br.net.brjdevs.natan.gabrielbot.music.GuildMusicPlayer;
import br.net.brjdevs.natan.gabrielbot.music.Track;
import br.net.brjdevs.natan.gabrielbot.utils.DiscordUtils;
import br.net.brjdevs.natan.gabrielbot.utils.Utils;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.awt.Color;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

@RegisterCommand.Class
public class MusicCommands {
    @RegisterCommand
    public static void play(CommandRegistry cr) {
        cr.register("play", SimpleCommand.builder(CommandCategory.MUSIC)
                .description("Plays music")
                .help((thiz, event)->thiz.helpEmbed(event, "play",
                        "`>>play <name>`: searches for the given name on youtube\n" +
                                "`>>play soundcloud <name>`: searches for the given name on soundcloud\n" +
                                "`>>play <url>`: plays the given video/playlist"
                ))
                .code((thiz, event, args)->{
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

                    GuildMusicPlayer gmp = GabrielBot.getInstance().createPlayer(event.getGuild().getIdLong(), event.getChannel().getIdLong(), event.getMember().getVoiceState().getChannel().getIdLong());

                    gmp.loader.load(event, identifier);
                })
                .build()
        );
    }
    /* TODO: implement these:
        move
        pause
        queue
        removetrack
     */

    @RegisterCommand
    public static void queue(CommandRegistry cr) {
        cr.register("queue", SimpleCommand.builder(CommandCategory.MUSIC)
                .description("Shows the queued songs")
                .help((thiz, event)->thiz.helpEmbed(event, "queue", "`>>queue`"))
                .code((thiz, event, args)->{
                    GuildMusicPlayer gmp = GabrielBot.getInstance().getPlayer(event.getGuild().getIdLong());
                    if(gmp == null) {
                        event.getChannel().sendMessage(new EmbedBuilder().setTitle("Queue for server " + event.getGuild().getName()).setDescription("Nothing playing").build()).queue();
                        return;
                    }
                    String[] tracks = gmp.scheduler.tracks().stream()
                            .map(t->{
                                Member m = event.getGuild().getMemberById(t.dj);
                                String dj = m == null ? "Unknown" : m.getUser().getName();
                                AudioTrackInfo info = t.track.getInfo();
                                return "[" + info.title.replace("]", "\\]") + "](" + info.uri + ") [" + dj + "]";
                            }).toArray(String[]::new);
                    if(tracks.length == 0) {
                        event.getChannel().sendMessage(new EmbedBuilder().setTitle("Queue for server " + event.getGuild().getName()).setDescription("Nothing playing").build()).queue();
                        return;
                    }
                    DiscordUtils.list(event, 30, false,
                            (page, total)-> {
                                AudioTrackInfo current = gmp.scheduler.currentTrack().track.getInfo();
                                return new EmbedBuilder()
                                        .setColor(Color.CYAN)
                                        .setTitle("Queue for server " + event.getGuild().getName())
                                        .setFooter("Page " + page + "/" + total, null)
                                        .addField("Currently playing", "[" + current.title.replace("[", "\\[") + "](" + current.uri + ")", false)
                                        .addField("Queue size", "`" + gmp.scheduler.tracks().size() + "`", true)
                                        .addField("Queue duration", "`" + Utils.getDuration(gmp.scheduler.tracks().stream().mapToLong(t->t.track.getDuration()).sum()) + "`", true);
                            },
                            tracks
                    );
                })
                .build()
        );
    }

    @RegisterCommand
    public static void volume(CommandRegistry cr) {
        cr.register("volume", SimpleCommand.builder(CommandCategory.MUSIC)
                .permission(CommandPermission.PREMIUM)
                .description("Changes volume for this guild")
                .help((thiz, event)->thiz.helpEmbed(event, "volume",
                        "`>>volume <new volume>`: Changes the volume\n" +
                                "`>>volume 100`: Resets the volume"
                ))
                .code((thiz, event, args) -> {
                    if(checkVC(event)) return;
                    if(args.length == 0) {
                        thiz.onHelp(event);
                        return;
                    }
                    int i;
                    try {
                        i = Integer.parseInt(args[0]);
                    } catch(NumberFormatException e) {
                        event.getChannel().sendMessage(args[0] + " is not a valid integer").queue();
                        return;
                    }
                    if(i < 0) {
                        i = 100;
                    } else if(i > 150) {
                        i = 150;
                    }
                    GuildMusicPlayer gmp = GabrielBot.getInstance().getPlayer(event.getGuild().getIdLong());
                    if(gmp == null) {
                        event.getChannel().sendMessage("I'm not playing any music").queue();
                        return;
                    }
                    gmp.player.setVolume(i);
                    event.getChannel().sendMessage("Volume set to " + i).queue();
                })
                .build()
        );
    }

    @RegisterCommand
    public static void skip(CommandRegistry cr) {
        cr.register("skip", SimpleCommand.builder(CommandCategory.MUSIC)
                .description("Skips the current song")
                .help((thiz, event)->thiz.helpEmbed(event, "skip",
                        "`>>skip`: Skips if you're the DJ, otherwise votes to skip"
                ))
                .code((event)->{
                    if(checkVC(event)) return;
                    GuildMusicPlayer gmp = GabrielBot.getInstance().getPlayer(event.getGuild().getIdLong());
                    if(gmp == null) {
                        event.getChannel().sendMessage("I'm not playing any song").queue();
                        return;
                    }
                    Track current = gmp.scheduler.currentTrack();
                    if(current.dj == event.getAuthor().getIdLong() || isDJ(event.getMember())) {
                        gmp.getTextChannel().sendMessage("The DJ has decided to skip").queue();
                        gmp.scheduler.nextTrack();
                        return;
                    }
                    gmp.scheduler.voteskip(event.getAuthor().getIdLong());
                })
                .build()
        );
    }

    @RegisterCommand
    public static void stop(CommandRegistry cr) {
        cr.register("stop", SimpleCommand.builder(CommandCategory.MUSIC)
                .description("Stops the player")
                .help((thiz, event)->thiz.helpEmbed(event, "stop", "`>>stop`"))
                .code((event)->{
                    if(checkVC(event)) return;
                    GuildMusicPlayer gmp = GabrielBot.getInstance().getPlayer(event.getGuild().getIdLong());
                    if(gmp == null) {
                        event.getChannel().sendMessage("I'm not playing any song").queue();
                        return;
                    }
                    boolean dj = isDJ(event.getMember());
                    if(dj) {
                        gmp.getTextChannel().sendMessage("The DJ decided to stop").queue();
                        GabrielBot.getInstance().removePlayer(gmp.guildId);
                    } else {
                        event.getChannel().sendMessage("You are not a DJ").queue();
                    }
                })
                .build()
        );
    }

    @RegisterCommand
    public static void np(CommandRegistry cr) {
        cr.register("np", SimpleCommand.builder(CommandCategory.MUSIC)
                .description("Shows currently playing music")
                .help((thiz, event)->thiz.helpEmbed(event, "np", "`>>np`"))
                .code((event)->{
                    GuildMusicPlayer gmp = GabrielBot.getInstance().getPlayer(event.getGuild().getIdLong());
                    if(gmp == null) {
                        event.getChannel().sendMessage("I'm not playing any song").queue();
                        return;
                    }
                    Track current = gmp.scheduler.currentTrack();
                    Member m = event.getGuild().getMemberById(current.dj);
                    event.getChannel().sendMessage("Now playing `" + current.track.getInfo().title + "`, added by " + (m == null ? "<@" + current.dj + ">" : m.getEffectiveName())).queue();
                })
                .build()
        );
    }

    private static boolean isDJ(Member member) {
        return CommandPermission.ADMIN.test(member) || member.getRoles().stream().anyMatch(r->r.getName().equals("DJ"));
    }

    private static boolean checkVC(GuildMessageReceivedEvent event) {
        VoiceChannel mc = event.getMember().getVoiceState().getChannel();
        VoiceChannel bc = event.getGuild().getSelfMember().getVoiceState().getChannel();
        if(mc == null) {
            event.getChannel().sendMessage(bc == null ?
                "You must be connected to a voice channel" :
                "You must be on the same voice channel as me"
            ).queue();
            return true;
        }
        if(bc == null) return false;
        if(bc.getIdLong() != mc.getIdLong()) {
            event.getChannel().sendMessage("You must be on the same voice channel as me").queue();
            return true;
        }
        return false;
    }
}
