package gabrielbot.lang.runtime.invoke;
import gabrielbot.lang.compiler.MethodWriter;
import gabrielbot.lang.runtime.ClassReader;
import gabrielbot.lang.runtime.Interpreter;

public final class InterpretedMethod extends Method {
	private final ClassReader.Method method;
	private final boolean isStatic;
	
	public InterpretedMethod(ClassReader.Method method) {
		this.method = method;
		this.isStatic = (method.flags & MethodWriter.ACC_STATIC) != 0;
	}
	
	@Override
	public Object run(Interpreter interpreter, Object instance, Object... args) {
		if(!isStatic) {
			Object[] narg = new Object[args.length+1];
			System.arraycopy(args, 0, narg, 1, args.length);
			narg[0] = instance;
			args = narg;
		}
		Interpreter runner = interpreter.forMethod(method, args);
		try {
			return runner.call();
		} catch(RuntimeException ex) {
			throw ex;
		} catch(Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
    public int args() {
	    return method.argCount;
    }
	
	@Override
	public boolean isStatic() {
		return isStatic;
	}
}
