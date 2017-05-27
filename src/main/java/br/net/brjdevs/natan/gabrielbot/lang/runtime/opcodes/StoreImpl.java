package br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes;

import br.net.brjdevs.natan.gabrielbot.lang.runtime.Interpreter;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.OpcodeImplementation;

public class StoreImpl implements OpcodeImplementation {
	@Override
	public void run(Interpreter interpreter, int[] args) {
		interpreter.locals.put(args[0], interpreter.operandStack.pop());
	}
}
