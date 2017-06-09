package br.net.brjdevs.natan.gabrielbot.utils.lua;

import net.dv8tion.jda.core.entities.Message;

class SafeMessage extends SafeISnowflake {
    private final Message message;

    SafeMessage(Message message) {
        super(message);
        this.message = message;
    }

    public String getContent() {
        return message.getContent();
    }

    public String getRawContent() {
        return message.getRawContent();
    }

    public String getStrippedContent() {
        return message.getStrippedContent();
    }
}
