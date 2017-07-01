package gabrielbot.lang.runtime.opcodes;

import gabrielbot.lang.runtime.Interpreter;
import gabrielbot.lang.runtime.OpcodeImplementation;
import gabrielbot.lang.runtime.invoke.Method;

import java.util.HashMap;
import java.util.Map;

public class InvokeVirtualImpl implements OpcodeImplementation {
	private static final Map<String, Method> javaMethods = new HashMap<>();
	private final Map<String, Method> methods = new HashMap<>();
	
	public static void registerJava(String id, Method m) {
		javaMethods.put(id, m);
	}
	
	public void register(String id, Method m) {
		methods.put(id, m);
	}
	
	@Override
	public void run(Interpreter interpreter, int[] args) {
		String id = interpreter.contantPool.get(args[0]);
		Method method = methods.get(id);
		if(method == null) {
            method = javaMethods.get(id);
			if(method == null) {
				throw new NoSuchMethodError(id);
			}
		}
		if(method.isStatic()) {
			throw new IllegalAccessError("Method is static: " + id);
		}
		int argCount = method.args();
		if(argCount != args[1]) {
			throw new IllegalArgumentException("Method " + id + " takes " + argCount + " args, but " + args[1] + " were given");
		}
		Object[] methodArgs = new Object[argCount];
		for(int i = argCount-1; i > -1; i--) {
			methodArgs[i] = interpreter.operandStack.pop();
		}
		Object o = method.run(interpreter, interpreter.operandStack.pop(), methodArgs);
		if(o != Method.VOID) interpreter.operandStack.push(o);
	}
}
