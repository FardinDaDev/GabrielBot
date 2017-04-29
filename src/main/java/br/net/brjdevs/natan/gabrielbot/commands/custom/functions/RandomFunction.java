package br.net.brjdevs.natan.gabrielbot.commands.custom.functions;

import java.util.Random;

public class RandomFunction extends JavaFunction {
    @Override
    public String apply(String[] args) {
        return args[new Random().nextInt(args.length)];
    }
}
