package br.net.brjdevs.natan.gabrielbot.commands.custom.functions;

import br.net.brjdevs.natan.gabrielbot.commands.fun.Jokes;

public class JokeFunction extends JavaFunction {
    @Override
    public String apply(String[] args) {
        return Jokes.getJoke(args.length == 0 ? "Chuck Norris" : args[0]);
    }
}
