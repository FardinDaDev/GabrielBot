package br.net.brjdevs.natan.gabrielbot.commands;

import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import br.net.brjdevs.natan.gabrielbot.core.command.*;
import br.net.brjdevs.natan.gabrielbot.music.GuildMusicPlayer;
import br.net.brjdevs.natan.gabrielbot.music.Track;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import static br.net.brjdevs.natan.gabrielbot.core.localization.LocalizationManager.*;

@RegisterCommand.Class
public class MusicCommands {
    @RegisterCommand
    public static void play(CommandRegistry cr) {
        cr.register("play", SimpleCommand.builder(CommandCategory.MUSIC)
                .description("play", "Plays music")
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
    public static void skip(CommandRegistry cr) {
        cr.register("skip", SimpleCommand.builder(CommandCategory.MUSIC)
                .description("skip", "Skips the current song")
                .help((thiz, event)->thiz.helpEmbed(event, "skip",
                        "`>>skip`: Skips if you're the DJ, otherwise votes to skip"
                ))
                .code((event)->{
                    if(checkVC(event)) return;
                    GuildMusicPlayer gmp = GabrielBot.getInstance().getPlayer(event.getGuild().getIdLong());
                    if(gmp == null) {
                        event.getChannel().sendMessage(
                                getString(event.getGuild(), Music.NOT_PLAYING, "I'm not playing any song")
                        ).queue();
                        return;
                    }
                    Track current = gmp.scheduler.currentTrack();
                    if(current.dj == event.getAuthor().getIdLong() || isDJ(event.getMember())) {
                        gmp.getTextChannel().sendMessage(
                                getString(event.getGuild(), Music.DJ_SKIP, "The DJ has decided to skip")
                        ).queue();
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
                .description("stop", "Stops the player")
                .help((thiz, event)->thiz.helpEmbed(event, "stop", "`>>stop`"))
                .code((event)->{
                    if(checkVC(event)) return;
                    GuildMusicPlayer gmp = GabrielBot.getInstance().getPlayer(event.getGuild().getIdLong());
                    if(gmp == null) {
                        event.getChannel().sendMessage(
                                getString(event.getGuild(), Music.NOT_PLAYING, "I'm not playing any song")
                        ).queue();
                        return;
                    }
                    boolean dj = isDJ(event.getMember());
                    if(dj) {
                        gmp.getTextChannel().sendMessage(
                                getString(event.getGuild(), Music.DJ_STOP, "The DJ decided to stop")
                        ).queue();
                        GabrielBot.getInstance().removePlayer(gmp.guildId);
                    } else {
                        event.getChannel().sendMessage(
                                getString(event.getGuild(), Music.NOT_DJ, "You are not a DJ")
                        ).queue();
                    }
                })
                .build()
        );
    }

    @RegisterCommand
    public static void np(CommandRegistry cr) {
        cr.register("np", SimpleCommand.builder(CommandCategory.MUSIC)
                .description("np", "Shows currently playing music")
                .help((thiz, event)->thiz.helpEmbed(event, "np", "`>>np`"))
                .code((event)->{
                    GuildMusicPlayer gmp = GabrielBot.getInstance().getPlayer(event.getGuild().getIdLong());
                    if(gmp == null) {
                        event.getChannel().sendMessage(
                                getString(event.getGuild(), Music.NOT_PLAYING, "I'm not playing any song")
                        ).queue();
                        return;
                    }
                    Track current = gmp.scheduler.currentTrack();
                    Member m = event.getGuild().getMemberById(current.dj);
                    event.getChannel().sendMessage(
                            getString(event.getGuild(), Music.NP, "Now playing $identifier$, added by $user$")
                                    .replace("$identifier$", "`" + current.track.getInfo().title + "`")
                                    .replace("$user$", m == null ? "<@" + current.dj + ">" : m.getEffectiveName())
                    ).queue();
                })
                .build()
        );
    }

    private static boolean isDJ(Member member) {
        return CommandPermission.ADMIN.test(member) || member.getRoles().stream().filter(r->r.getName().equals("DJ")).count() > 0;
    }

    private static boolean checkVC(GuildMessageReceivedEvent event) {
        VoiceChannel mc = event.getMember().getVoiceState().getChannel();
        VoiceChannel bc = event.getGuild().getSelfMember().getVoiceState().getChannel();
        if(mc == null) {
            event.getChannel().sendMessage(bc == null ?
                getString(event.getGuild(), Music.NOT_CONNECTED, "You must be connected to a voice channel") :
                getString(event.getGuild(), Music.DIFFERENT_VC, "You must be on the same voice channel as me")
            ).queue();
            return true;
        }
        if(bc == null) return false;
        if(bc.getIdLong() != mc.getIdLong()) {
            event.getChannel().sendMessage(
                    getString(event.getGuild(), Music.DIFFERENT_VC, "You must be on the same voice channel as me")
            ).queue();
            return true;
        }
        return false;
    }
}
