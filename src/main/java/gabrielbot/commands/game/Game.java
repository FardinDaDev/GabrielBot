package gabrielbot.commands.game;

import gabrielbot.core.listeners.operations.InteractiveOperation;
import gnu.trove.set.TLongSet;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public abstract class Game implements InteractiveOperation {
    private final TextChannel channel;
    protected final TLongSet players;

    public Game(TextChannel channel, TLongSet players) {
        this.channel = channel;
        this.players = players;
    }

    @Override
    public int run(GuildMessageReceivedEvent event) {
        if(!players.contains(event.getAuthor().getIdLong())) return IGNORED;
        if(event.getMessage().getRawContent().equals("end")) {
            say("Ended game");
            return COMPLETED;
        }
        return onMessage(event) ? COMPLETED : RESET_TIMEOUT;
    }

    protected void say(String text) {
        channel.sendMessage(text).queue();
    }

    protected abstract boolean onMessage(GuildMessageReceivedEvent event);

    @Override
    public void onExpire() {
        say("Time over!");
    }

    @Override
    public void onCancel() {

    }
}
