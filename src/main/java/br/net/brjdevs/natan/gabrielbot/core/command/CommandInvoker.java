package br.net.brjdevs.natan.gabrielbot.core.command;

import br.net.brjdevs.natan.gabrielbot.utils.StringUtils;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.Map;

interface CommandInvoker {
    void invoke(Map<String, ?> map);

    static String[] advancedSplit(GuildMessageReceivedEvent event) {
        String[] parts = event.getMessage().getRawContent().split(" ", 2);
        if(parts.length == 1) return new String[0];
        return StringUtils.advancedSplitArgs(parts[1], 0);
    }

    static String[] split(GuildMessageReceivedEvent event) {
        String[] s = event.getMessage().getRawContent().split(" ", 2);
        return s.length == 1 ? new String[0] : s[1].split(" ");
    }

    static String getContent(GuildMessageReceivedEvent event) {
        String[] s = event.getMessage().getRawContent().split(" ", 2);
        return s.length == 1 ? "" : s[1];
    }

    @SuppressWarnings("unchecked")
    default void run(GuildMessageReceivedEvent event, boolean advancedSplit, Map<String, ?> map) {
        ((Map)map).put("args", advancedSplit ? advancedSplit(event) : split(event));
        ((Map)map).put("input", getContent(event));
        invoke(map);
    }
}
