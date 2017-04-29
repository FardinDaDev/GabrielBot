package br.net.brjdevs.natan.gabrielbot.commands.custom.functions;

import java.util.Random;

public class RangeFunction extends JavaFunction {
    @Override
    public String apply(String[] args) {
        if(args.length < 2 || !(isInteger(args[0]) && isInteger(args[1]))) return "null";
        int a = Integer.parseInt(args[0]);
        int b = Integer.parseInt(args[1]);
        if(a == b) return args[0];
        if(a < b) {
            return String.valueOf(new Random().nextInt(b-a)+a);
        }
        return String.valueOf(new Random().nextInt(a-b)+b);
    }
}
