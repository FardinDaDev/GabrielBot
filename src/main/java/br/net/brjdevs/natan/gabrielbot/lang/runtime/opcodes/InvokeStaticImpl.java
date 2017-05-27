package br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes;

import br.net.brjdevs.natan.gabrielbot.lang.common.Constants;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.Array;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.Interpreter;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.OpcodeImplementation;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.invoke.Method;

import java.util.HashMap;
import java.util.Map;

public class InvokeStaticImpl implements OpcodeImplementation {
	static final Map<String, Method> javaMethods = new HashMap<>();
	final Map<String, Method> methods = new HashMap<>();

	static {
        javaMethods.put(Constants.NEW_ARRAY, new NewArray());
        javaMethods.put(Constants.NEW_STRING, new NewString());
        javaMethods.put(Constants.STRING_APPEND, new StringAppend());
    }

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
		if(!method.isStatic()) {
			throw new IllegalAccessError("Method is not static: " + id);
		}
        int argCount = method.args();
		if(argCount != args[1]) {
			throw new IllegalArgumentException("Method " + id + " takes " + argCount + " args, but " + args[1] + " were given");
		}
		Object[] methodArgs = new Object[argCount];
		for(int i = argCount-1; i > -1; i--) {
			methodArgs[i] = interpreter.operandStack.pop();
		}
		Object o = method.run(interpreter, null, methodArgs);
		if(o != Method.VOID) interpreter.operandStack.push(o);
	}

    private static class NewArray extends Method {
        @Override
        public boolean isStatic() {
            return true;
        }

        @Override
        public Object run(Interpreter interpreter, Object instance, Object[] args) {
            return new Array();
        }

        @Override
        public int args() {
            return 0;
        }
    }

    private static class StringAppend extends Method {
        @Override
        public Object run(Interpreter interpreter, Object instance, Object[] args) {
            return String.valueOf(instance) + args[0];
        }

        @Override
        public int args() {
            return 1;
        }
    }

    private static class NewString extends Method {
        @Override
        public boolean isStatic() {
            return true;
        }

        @Override
        public Object run(Interpreter interpreter, Object instance, Object[] args) {
            return "";
        }

        @Override
        public int args() {
            return 0;
        }
    }
}
