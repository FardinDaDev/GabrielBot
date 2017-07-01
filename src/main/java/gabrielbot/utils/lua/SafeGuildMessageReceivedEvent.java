package gabrielbot.utils.lua;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

class SafeGuildMessageReceivedEvent {
    private final GuildMessageReceivedEvent event;
    private final SafeChannel channel;
    private final SafeUser author;
    private final SafeMember member;
    private final SafeGuild guild;
    private final SafeMessage message;

    SafeGuildMessageReceivedEvent(GuildMessageReceivedEvent event, int maxMessages) {
        this.event = event;
        this.channel = new SafeChannel(event.getChannel(), maxMessages);
        this.author = new SafeUser(event.getAuthor());
        this.member = new SafeMember(event.getMember());
        this.guild = new SafeGuild(event.getGuild(), channel);
        this.message = new SafeMessage(event.getMessage());
    }

    public SafeChannel getChannel() {
        return channel;
    }

    public SafeUser getAuthor() {
        return author;
    }

    public SafeMember getMember() {
        return member;
    }

    public SafeGuild getGuild() {
        return guild;
    }

    public SafeMessage getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return event.toString();
    }
}
