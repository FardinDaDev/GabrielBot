package br.net.brjdevs.natan.gabrielbot.commands.custom.functions;

public class ReplaceFunction extends JavaFunction {
    @Override
    public String apply(String[] args) {
        if(args.length < 3) return args.length > 0 ? args[0] : "null";
        return args[0].replace(args[1], args[2]);
    }
}
