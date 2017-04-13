package br.net.brjdevs.natan.gabrielbot.utils;

public class PrologBuilder {
    private final StringBuilder sb;

    public PrologBuilder() {
        sb = new StringBuilder(128);
    }

    public PrologBuilder(PrologBuilder other) {
        sb = new StringBuilder(other.sb.length() + 128);
        sb.append(other.sb.toString());
    }

    public PrologBuilder addField(String name, String value) {
        sb.append(name).append(": ").append(value).append('\n');
        return this;
    }

    public PrologBuilder addField(String name, Object value) {
        return addField(name, value == null ? "null" : value.toString());
    }

    public PrologBuilder addLabel(String name) {
        sb.append("--").append(name).append("--").append('\n');
        return this;
    }

    public PrologBuilder addEmptyLine() {
        sb.append('\n');
        return this;
    }

    public String build() {
        return "```prolog\n" + sb.toString() + "```";
    }
}