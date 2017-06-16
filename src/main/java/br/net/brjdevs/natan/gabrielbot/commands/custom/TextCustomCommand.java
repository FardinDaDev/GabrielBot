package br.net.brjdevs.natan.gabrielbot.commands.custom;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextCustomCommand extends CustomCommand {
    private static final Pattern ARG_PATTERN = Pattern.compile("\\$\\(event.args.\\d+\\)");
    private static final Pattern ARG_SPLIT = Pattern.compile(" +");

    private final String text;

    public TextCustomCommand(String text) {
        this.text = text;
    }

    @Override
    public String process(GuildMessageReceivedEvent event, String input, Map<String, String> mappings) {
        String s = text;
        String[] args = ARG_SPLIT.split(input.trim());
        for(Integer i : usedArgs(text)) {
            s = s.replace("$(event.args." + (i+1) + ")", i < args.length ? args[i] : "");
        }
        return map(s, mappings);
    }

    @Override
    public String getRaw() {
        return text;
    }

    private static List<Integer> usedArgs(String t) {
        List<Integer> l = new ArrayList<>();
        Matcher m = ARG_PATTERN.matcher(t);
        while(m.find()) {
            String s = m.group();
            s = s.replaceAll("\\$\\(event.args.(\\d+)\\)", "$1");
            int i = Integer.parseInt(s)-1;
            if(!l.contains(i)) l.add(i);
        }
        return l;
    }
}
