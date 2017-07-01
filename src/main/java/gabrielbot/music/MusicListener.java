package gabrielbot.music;

import br.com.brjdevs.highhacks.eventbus.Listener;
import gabrielbot.GabrielBot;
import net.dv8tion.jda.core.entities.GuildVoiceState;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMuteEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class MusicListener implements EventListener {
    @Override
    public void onEvent(Event event) {
        if(event instanceof GuildVoiceMoveEvent) {
            onGuildVoiceMove((GuildVoiceMoveEvent)event);
        } else if(event instanceof GuildVoiceJoinEvent) {
            onGuildVoiceJoin((GuildVoiceJoinEvent)event);
        } else if(event instanceof GuildVoiceLeaveEvent) {
            onGuildVoiceLeave((GuildVoiceLeaveEvent)event);
        } else if(event instanceof GuildVoiceMuteEvent) {
            onGuildVoiceMute((GuildVoiceMuteEvent)event);
        }
    }

    @Listener
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        if(event.getChannelJoined().getMembers().contains(event.getGuild().getSelfMember()))
            onJoin(event.getChannelJoined());
        if(event.getChannelLeft().getMembers().contains(event.getGuild().getSelfMember()))
            onLeave(event.getChannelLeft());
    }

    @Listener
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        if(event.getChannelJoined().getMembers().contains(event.getGuild().getSelfMember()))
            onJoin(event.getChannelJoined());
    }

    @Listener
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        if(event.getChannelLeft().getMembers().contains(event.getGuild().getSelfMember()))
            onLeave(event.getChannelLeft());
    }

    @Listener
    public void onGuildVoiceMute(GuildVoiceMuteEvent event) {
        if(event.getMember().getUser().getIdLong() != event.getJDA().getSelfUser().getIdLong()) return;
        GuildVoiceState vs = event.getVoiceState();
        if(validate(vs)) return;
        GuildMusicPlayer gmp = GabrielBot.getInstance().getPlayer(event.getGuild().getIdLong());
        if(gmp != null) {
            if(event.isMuted()) {
                gmp.scheduleLeave(GuildMusicPlayer.LeaveReason.MUTED);
            } else {
                if(!isAlone(vs.getChannel())) {
                    gmp.cancelLeave();
                }
            }
        }
    }

    private void onJoin(VoiceChannel vc) {
        GuildVoiceState vs = vc.getGuild().getSelfMember().getVoiceState();
        if(validate(vs)) return;
        if(!isAlone(vc)) {
            GuildMusicPlayer gmp = GabrielBot.getInstance().getPlayer(vc.getGuild().getIdLong());
            if(gmp != null) {
                gmp.cancelLeave();
            }
        }
    }

    private void onLeave(VoiceChannel vc) {
        GuildVoiceState vs = vc.getGuild().getSelfMember().getVoiceState();
        if(validate(vs)) return;
        if(isAlone(vc)) {
            GuildMusicPlayer gmp = GabrielBot.getInstance().getPlayer(vc.getGuild().getIdLong());
            if(gmp != null) {
                gmp.scheduleLeave(GuildMusicPlayer.LeaveReason.ALONE);
            }
        }
    }

    private static boolean validate(GuildVoiceState state) {
        return state == null || !state.inVoiceChannel();
    }

    private static boolean isAlone(VoiceChannel vc) {
        return vc.getMembers().stream().filter(m -> !m.getUser().isBot()).count() == 0;
    }
}
