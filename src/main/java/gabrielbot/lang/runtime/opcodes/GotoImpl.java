package gabrielbot.lang.runtime.opcodes;

import gabrielbot.lang.runtime.Interpreter;
import gabrielbot.lang.runtime.OpcodeImplementation;

public class GotoImpl implements OpcodeImplementation {
	@Override
	public void run(Interpreter interpreter, int[] args) {
		interpreter.gotoInstruction(args[0]);
	}
}
