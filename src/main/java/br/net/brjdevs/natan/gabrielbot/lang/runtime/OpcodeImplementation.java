package br.net.brjdevs.natan.gabrielbot.lang.runtime;

public interface OpcodeImplementation {
	void run(Interpreter interpreter, int[] args);
}
