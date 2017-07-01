package gabrielbot.lang.runtime.opcodes;
import gabrielbot.lang.common.Opcodes;
import gabrielbot.lang.runtime.Interpreter;
import gabrielbot.lang.runtime.OpcodeImplementation;
import gabrielbot.lang.runtime.invoke.Method;

public class NewImpl implements OpcodeImplementation {
	@Override
	public void run(Interpreter interpreter, int[] args) {
		String name = interpreter.contantPool.get(args[0]);
		
		Method m = ((InvokeStaticImpl)interpreter.getOpcodeImplementationByOpcode(Opcodes.INVOKESTATIC)).methods.get(name);
		if(m == null) {
			m = InvokeStaticImpl.javaMethods.get(name);
			if(m == null) {
				throw new NoSuchMethodError("No constructor found for class " + name);
			}
		}
		if(m.args() != 0) {
			throw new NoSuchMethodError("No constructor found for class " + name);
		}
		Object v = m.run(interpreter, null);
		if(v == null || v == Method.VOID) {
			throw new AssertionError("Constructor returned null or void: " + name);
		}
		interpreter.operandStack.push(v);
	}
}
