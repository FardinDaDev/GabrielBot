package br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes;

import br.net.brjdevs.natan.gabrielbot.lang.runtime.Array;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.Interpreter;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.OpcodeImplementation;

public class ALoadImpl implements OpcodeImplementation {
	@Override
	public void run(Interpreter interpreter, int[] args) {
		interpreter.operandStack.push(((Array)interpreter.operandStack.pop()).get(args[0]));
	}
}
