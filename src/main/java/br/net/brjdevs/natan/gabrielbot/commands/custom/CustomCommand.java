package br.net.brjdevs.natan.gabrielbot.commands.custom;

import br.net.brjdevs.natan.gabrielbot.commands.custom.functions.MathFunction;
import br.net.brjdevs.natan.gabrielbot.commands.custom.functions.RandomFunction;
import br.net.brjdevs.natan.gabrielbot.commands.custom.functions.RangeFunction;
import br.net.brjdevs.natan.gabrielbot.commands.custom.functions.URLFunction;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomCommand {
    public static final String ADVANCED_SPLIT_FLAG = "#advsplit";

    private static final Pattern
            funcCallPattern = Pattern.compile("\\$(\\S+?)\\(.*?\\)"),
            funcPattern = Pattern.compile("func(tion)?\\s+(\\S+?)\\s*\\(.*?\\)\\s*\\{(.+?)\\}"),
            ifPattern = Pattern.compile("#if\\s*(.+?)\\s*\\{.+?\\}"),
            ifElsePattern = Pattern.compile("#if\\s*(.+?)\\s*\\{.+?\\}\\s*else\\s*\\{.+?\\}"),
            equalsPattern = Pattern.compile("\\S+?\\s*==\\s*\\S+");
    private static final Map<String, Function> defaults = new HashMap<>();

    static {
        registerDefault("math", new MathFunction());
        registerDefault("random", new RandomFunction());
        registerDefault("range", new RangeFunction());
        registerDefault("url", new URLFunction());
    }

    public static void registerDefault(String name, Function function) {
        defaults.put(name, function);
    }

    private final Map<String, Function> functions;
    private final String body;
    private final boolean advancedSplit;

    private CustomCommand(String body, Map<String, Function> functions, boolean advancedSplit) {
        this.body = body;
        this.functions = functions;
        this.advancedSplit = advancedSplit;
    }

    public String getRaw() {
        StringBuilder sb = new StringBuilder(2000); //shouldn't be possible to get more than this anyway
        if(advancedSplit) sb.append(ADVANCED_SPLIT_FLAG);
        functions.forEach((name, function)->sb.append(function.getRaw(name)));
        sb.append(body);
        return sb.toString();
    }

    public String process(String input, String... vars) {
        if(vars.length % 2 == 1)
            throw new IllegalArgumentException("vars must have an even number of elements");
        String ret = body;
        for(int i = 0; i < vars.length; i+=2) {
            ret = ret.replace("%" + vars[i] + "%", vars[i+1]);
        }

        String[] inputSplitted;
        if(input != null) {
            ret = ret.replace("%input%", input);
            inputSplitted = input.split(" ");
        } else {
            inputSplitted = new String[0];
        }

        Matcher funcMatcher = funcCallPattern.matcher(ret);
        while(funcMatcher.find()) {
            String group = funcMatcher.group();
            String stripped = group.substring(1);
            String[] args = splitArgs(stripped.substring(stripped.indexOf('(')+1, stripped.indexOf(')')));
            for(int i = 0; i < args.length; i++) {
                args[i] = args[i].trim();
            }
            String name = stripped.substring(0, stripped.indexOf('('));

            if(name.equals("select")) {
                ret = ret.replace(group, select(args, inputSplitted));
            } else {
                Function f = defaults.get(name);
                if(f == null) {
                    f = functions.get(name);
                    if(f == null) continue;
                }
                ret = ret.replace(group, f.apply(args));
            }
        }

        Matcher ifElseMatcher = ifElsePattern.matcher(ret);
        while(ifElseMatcher.find()) {
            String group = ifElseMatcher.group();
            String condition = group.substring(4, group.indexOf(')'));

            int elseStart = group.indexOf('{', group.indexOf('{')+1)+1;
            String trueContents = group.substring(group.indexOf('{')+1, group.indexOf('}'));
            String falseContents = group.substring(elseStart, group.length()-1);

            Matcher equalsMatcher = equalsPattern.matcher(condition);
            if(equalsMatcher.find()) {
                String cond = equalsMatcher.group();
                String[] parts = cond.split("\\s*==\\s*");

                if(parts.length == 2 && parts[0].equals(parts[1])) {
                    ret = ret.replace(group, trueContents);
                } else {
                    ret = ret.replace(group, falseContents);
                }
            } else if(condition.equals("true") || (isNumber(condition) && Double.parseDouble(condition) > 0)) {
                ret = ret.replace(group, trueContents);
            } else {
                ret = ret.replace(group, falseContents);
            }
        }

        Matcher ifMatcher = ifPattern.matcher(ret);
        while(ifMatcher.find()) {
            String group = ifMatcher.group();
            String condition = group.substring(4, group.indexOf(')'));
            String contents = group.substring(group.indexOf('{')+1, group.length()-1);
            Matcher equalsMatcher = equalsPattern.matcher(condition);
            if(equalsMatcher.find()) {
                String cond = equalsMatcher.group();
                String[] parts = cond.split("\\s*==\\s*");
                if(parts.length == 2 && parts[0].equals(parts[1])) {
                    ret = ret.replace(group, contents);
                } else {
                    ret = ret.replace(group, "");
                }
            } else if(condition.equals("true") || (isNumber(condition) && Double.parseDouble(condition) > 0)) {
                ret = ret.replace(group, contents);
            } else {
                ret = ret.replace(group, "");
            }
        }

        return ret;
    }

    private String[] splitArgs(String s) {
        if(!advancedSplit) return s.split(",\\s*");
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inString = false, escaped = false;
        for(char c : s.toCharArray()) {
            if(c == '"') {
                if(escaped) {
                    escaped = false;
                    current.append('"');
                } else {
                    inString = !inString;
                }
            } else if(c == '\\') {
                if(escaped) {
                    current.append('\\');
                    escaped = false;
                } else {
                    escaped = true;
                }
            } else if(c == ',') {
                if(escaped) {
                    escaped = false;
                    current.append(',');
                    continue;
                }
                if(inString) {
                    current.append(',');
                } else {
                    parts.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }

        if(current.length() != 0) parts.add(current.toString());
        return parts.toArray(new String[0]);
    }

    private static String select(String[] args, String[] input) {
        if(args.length == 0 || !isInteger(args[0]))
            return "null";
        int i = Integer.parseInt(args[0])-1;
        if(i < 0 || i > input.length) return "null";
        return input[i];
    }

    private static boolean isInteger(String s) {
        char[] chars = s.toCharArray();
        for(int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if(i == 0 && c == '-') continue;
            if(!Character.isDigit(c)) return false;
        }
        return true;
    }

    private static boolean isNumber(String s) {
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

    public static CustomCommand of(String text) {
        Map<String, Function> funcs = new HashMap<>();
        boolean advancedSplit = false;

        if(text.startsWith(ADVANCED_SPLIT_FLAG)) {
            text = text.substring(ADVANCED_SPLIT_FLAG.length());
            advancedSplit = true;
        }

        String cmd = text;

        Matcher m = funcPattern.matcher(text);

        while(m.find()) {
            String group = m.group();
            cmd = cmd.replace(group, "");
            group = group.substring(group.indexOf(' ')+1).trim();
            String name = group.substring(0, group.indexOf('('));
            String params = group.substring(group.indexOf('(')+1);
            params = params.substring(0, params.indexOf(')'));
            String body = group.substring(group.indexOf('{')+1);
            body = body.substring(0, body.length()-1);
            List<String> paramsList = Arrays.asList(params.split(",\\s*"));
            funcs.put(name, new Function(body, paramsList));
        }

        cmd = cmd.trim();
        return new CustomCommand(cmd, Collections.unmodifiableMap(funcs), advancedSplit);
    }
}
