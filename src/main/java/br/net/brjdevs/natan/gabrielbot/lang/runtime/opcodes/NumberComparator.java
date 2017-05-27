package br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes;

import br.net.brjdevs.natan.gabrielbot.lang.runtime.Interpreter;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.OpcodeImplementation;

abstract class NumberComparator implements OpcodeImplementation {
	@Override
	public void run(Interpreter interpreter, int[] args) {
		double d2 = ((Number)interpreter.operandStack.pop()).doubleValue();
		double d1 = ((Number)interpreter.operandStack.pop()).doubleValue();
		if(!apply(d1, d2)) interpreter.gotoInstruction(args[0]);
	}
	
	abstract boolean apply(double d1, double d2);
}
