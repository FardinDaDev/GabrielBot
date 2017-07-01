package gabrielbot.lang.runtime.opcodes;

import gabrielbot.lang.runtime.Array;
import gabrielbot.lang.runtime.Interpreter;
import gabrielbot.lang.runtime.OpcodeImplementation;

public class AStoreImpl implements OpcodeImplementation {
	@Override
	public void run(Interpreter interpreter, int[] args) {
		if(args[1] == 1) {
			((Array)interpreter.operandStack.pop()).set(args[0], interpreter.operandStack.pop());
		} else if(args[1] == 0) {
			Object v = interpreter.operandStack.pop();
			((Array)interpreter.operandStack.pop()).set(args[0], v);
		} else if(args[1] == 2) {
			Object o = interpreter.operandStack.pop();
			Array a = (Array)interpreter.operandStack.pop();
			a.set(args[0], o);
			interpreter.operandStack.push(a);
		}
	}
}
