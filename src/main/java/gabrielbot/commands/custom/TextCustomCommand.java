package gabrielbot.commands.custom;

import gabrielbot.utils.Randoms;
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
            s = s.replace("$(event.args." + i + ")", i < args.length ? args[i] : "");
        }
        return parseRandom(map(s, mappings));
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
            int i = Integer.parseInt(s);
            if(!l.contains(i)) l.add(i);
        }
        return l;
    }

    private static String parseRandom(String s) {
        int idx = s.indexOf("$random(");
        if(idx == -1) {
            return s;
        }
        StringBuilder str = new StringBuilder();
        while(idx != -1) {
            str.append(s.substring(0, idx));
            s = s.substring(idx+"$random(".length());
            StringBuilder sb = new StringBuilder();
            int p = 1;
            boolean inString = false;
            boolean escaped = false;
            for(char c : s.toCharArray()) {
                if(c == '(') p++;
                if(c == ')') {
                    if(!inString) p--;
                    if(p == 0) {
                        break;
                    }
                }
                if(c == '\\') escaped = !escaped;
                if(c == '"') {
                    if(!escaped)
                        inString = !inString;
                    escaped = false;
                }
                sb.append(c);
            }
            if(p != 0) {
                str.append(sb);
                break;
            }
            List<String> options = new ArrayList<>();
            StringBuilder opt = new StringBuilder();
            for(char c : sb.toString().toCharArray()) {
                if(c == '\\') escaped = !escaped;
                if(c == '"') {
                    if(!escaped) {
                        inString = !inString;
                        if(opt.length() == 0) continue;
                        options.add(opt.toString());
                        opt = new StringBuilder();
                    }
                }
                if(inString) opt.append(c);
            }
            str.append(options.get(Randoms.nextInt(options.size())));
            idx = s.indexOf("$random(", idx);
        }
        return str.toString();
    }
}
