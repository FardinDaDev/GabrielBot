package br.net.brjdevs.natan.gabrielbot.music;

import br.com.brjdevs.highhacks.eventbus.Listener;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;

public class MusicListener {
    @Listener
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {

    }

    @Listener
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {

    }

    @Listener
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {

    }
}
