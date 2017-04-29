package br.net.brjdevs.natan.gabrielbot.commands.custom;

import java.util.Arrays;
import java.util.List;

public class Function {
    private final String[] args;
    private final String text;

    public Function(String text, String... params) {
        this.text = text == null ? null : text.trim();
        this.args = params;
    }

    public Function(String text, List<String> params) {
        this(text, params.toArray(new String[0]));
    }

    public String getRaw(String name) {
        StringBuilder sb = new StringBuilder(128);
        sb.append("func ").append(name).append('(');
        Arrays.stream(args).forEach(arg->sb.append(arg).append(','));
        sb.setCharAt(sb.length()-1, ')');
        sb.append('{').append(text).append('}');

        return sb.toString();
    }

    public String apply(String... args) {
        String s = text;
        int i = Math.min(args.length, this.args.length);
        int index = 0;
        while(i > 0) {
            String arg = this.args[index];
            String repl = args[index];
            s = s.replace("%" + arg + "%", repl);
            index++;
            i--;
        }
        return s;
    }

    protected static boolean isInteger(String s) {
        char[] chars = s.toCharArray();
        for(int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if(i == 0 && c == '-') continue;
            if(!Character.isDigit(c)) return false;
        }
        return true;
    }

    protected static boolean isNumber(String s) {
        char[] chars = s.toCharArray();
        boolean dot = false;
        for(int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if(c == '.' && i > 0) {
                if(dot) return false;
                dot = true;
                continue;
            }
            if(i == 0 && c == '-') continue;
            if(!Character.isDigit(c)) return false;
        }
        return true;
    }
}
