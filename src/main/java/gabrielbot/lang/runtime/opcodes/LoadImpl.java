package gabrielbot.lang.runtime.opcodes;

import gabrielbot.lang.runtime.Interpreter;
import gabrielbot.lang.runtime.OpcodeImplementation;

public class LoadImpl implements OpcodeImplementation {
	@Override
	public void run(Interpreter interpreter, int[] args) {
		interpreter.operandStack.push(interpreter.locals.get(args[0]));
	}
}
