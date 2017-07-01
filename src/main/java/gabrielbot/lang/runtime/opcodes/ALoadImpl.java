package gabrielbot.lang.runtime.opcodes;

import gabrielbot.lang.runtime.Array;
import gabrielbot.lang.runtime.Interpreter;
import gabrielbot.lang.runtime.OpcodeImplementation;

public class ALoadImpl implements OpcodeImplementation {
	@Override
	public void run(Interpreter interpreter, int[] args) {
		interpreter.operandStack.push(((Array)interpreter.operandStack.pop()).get(args[0]));
	}
}
