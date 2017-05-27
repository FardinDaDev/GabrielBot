package br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes;

import br.net.brjdevs.natan.gabrielbot.lang.runtime.Interpreter;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.OpcodeImplementation;

public class LoadGlobalImpl implements OpcodeImplementation {
	@Override
	public void run(Interpreter interpreter, int[] args) {
		interpreter.operandStack.push(interpreter.globals.get(interpreter.contantPool.get(args[0])));
	}
}
