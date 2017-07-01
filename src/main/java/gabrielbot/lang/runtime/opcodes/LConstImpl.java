package gabrielbot.lang.runtime.opcodes;

import gabrielbot.lang.runtime.Interpreter;
import gabrielbot.lang.runtime.OpcodeImplementation;

public class LConstImpl implements OpcodeImplementation {
	@Override
	public void run(Interpreter interpreter, int[] args) {
		interpreter.operandStack.push(((long)args[0])<<32 | args[1]);
	}
}
