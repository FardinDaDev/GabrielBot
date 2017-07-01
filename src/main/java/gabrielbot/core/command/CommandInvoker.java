package gabrielbot.core.command;

import gabrielbot.utils.StringUtils;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.Map;

abstract class CommandInvoker {
    abstract void invoke(Map<String, ?> map);

    private static String[] advancedSplit(GuildMessageReceivedEvent event) {
        String[] parts = event.getMessage().getRawContent().split(" ", 2);
        if(parts.length == 1) return new String[0];
        return StringUtils.advancedSplitArgs(parts[1], 0);
    }

    private static String[] split(GuildMessageReceivedEvent event) {
        String[] s = event.getMessage().getRawContent().split(" ", 2);
        return s.length == 1 ? new String[0] : s[1].split(" ");
    }

    private static String getContent(GuildMessageReceivedEvent event) {
        String[] s = event.getMessage().getRawContent().split(" ", 2);
        return s.length == 1 ? "" : s[1];
    }

    @SuppressWarnings("unchecked")
    void run(GuildMessageReceivedEvent event, boolean advancedSplit, Map<String, ?> map) {
        ((Map)map).put("args", advancedSplit ? advancedSplit(event) : split(event));
        ((Map)map).put("input", getContent(event));
        invoke(map);
    }
}
