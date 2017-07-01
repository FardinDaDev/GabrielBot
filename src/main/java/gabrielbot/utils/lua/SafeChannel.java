package gabrielbot.utils.lua;

import net.dv8tion.jda.core.entities.MessageChannel;
import org.luaj.vm2.LuaError;

class SafeChannel extends SafeISnowflake<MessageChannel> {
    private final int maxMessages;
    private int messages = 0;

    SafeChannel(MessageChannel channel, int maxMessages) {
        super(channel);
        this.maxMessages = maxMessages;
    }

    public void sendMessage(String message) {
        if(++messages >= maxMessages) throw new LuaError("Maximum amount of messages reached");
        snowflake.sendMessage(message).queue();
    }
}
