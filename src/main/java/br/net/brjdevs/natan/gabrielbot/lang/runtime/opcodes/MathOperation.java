package br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes;

import br.net.brjdevs.natan.gabrielbot.lang.runtime.Interpreter;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.OpcodeImplementation;

public abstract class MathOperation implements OpcodeImplementation {
	@Override
	public void run(Interpreter interpreter, int[] args) {
		double d2 = ((Number)interpreter.operandStack.pop()).doubleValue();
		double d1 = ((Number)interpreter.operandStack.pop()).doubleValue();
		interpreter.operandStack.push(apply(d1, d2));
	}
	
	abstract double apply(double d1, double d2);
}
