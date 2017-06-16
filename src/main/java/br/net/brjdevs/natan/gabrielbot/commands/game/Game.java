package br.net.brjdevs.natan.gabrielbot.commands.game;

import br.net.brjdevs.natan.gabrielbot.core.listeners.operations.InteractiveOperation;
import gnu.trove.set.TLongSet;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public abstract class Game implements InteractiveOperation {
    private final TextChannel channel;
    private final TLongSet players;

    public Game(TextChannel channel, TLongSet players) {
        this.channel = channel;
        this.players = players;
    }

    @Override
    public int run(GuildMessageReceivedEvent event) {
        if(!players.contains(event.getAuthor().getIdLong())) return IGNORED;
        return onMessage(event) ? COMPLETED : RESET_TIMEOUT;
    }

    protected void say(String text) {
        channel.sendMessage(text).queue();
    }

    protected abstract boolean onMessage(GuildMessageReceivedEvent event);

    @Override
    public void onCancel() {

    }
}
