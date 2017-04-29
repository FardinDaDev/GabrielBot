package br.net.brjdevs.natan.gabrielbot.commands.custom.functions;

import br.net.brjdevs.natan.gabrielbot.commands.custom.Function;

public abstract class JavaFunction extends Function {
    public JavaFunction() {
        super(null, (String[])null);
    }

    @Override
    public abstract String apply(String[] args);
}
