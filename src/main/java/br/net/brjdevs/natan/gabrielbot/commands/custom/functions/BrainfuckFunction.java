package br.net.brjdevs.natan.gabrielbot.commands.custom.functions;

import br.net.brjdevs.natan.gabrielbot.commands.fun.BrainfuckInterpreter;

public class BrainfuckFunction extends JavaFunction {
    private static final BrainfuckInterpreter interpreter = new BrainfuckInterpreter(5_000, 1<<10); //5K ops, 1K ram

    @Override
    public String apply(String[] args) {
        return args.length == 0 ? "null" : args.length == 1 ? process(args[0], "") : process(args[0], args[1]);
    }

    private static String process(String code, String input) {
        try {
            return interpreter.process(code.toCharArray(), input, null);
        } catch(Throwable t) {
            return "error: " + t.getMessage();
        }
    }
}
