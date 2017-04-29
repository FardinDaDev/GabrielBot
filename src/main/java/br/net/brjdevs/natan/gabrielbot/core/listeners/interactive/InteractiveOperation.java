package br.net.brjdevs.natan.gabrielbot.core.listeners.interactive;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public interface InteractiveOperation {
    boolean run(GuildMessageReceivedEvent event);

    default void onExpire() {
    }
}
