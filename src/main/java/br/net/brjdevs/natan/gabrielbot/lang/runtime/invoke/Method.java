package br.net.brjdevs.natan.gabrielbot.lang.runtime.invoke;

import br.net.brjdevs.natan.gabrielbot.lang.runtime.Interpreter;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.InvokeStaticImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.InvokeVirtualImpl;

import java.lang.reflect.Modifier;

public abstract class Method {
	public static final Object VOID = new Object();
	public static final NameMapper DEFAULT_MAPPER = java.lang.reflect.Method::getName;
	
	public abstract Object run(Interpreter interpreter, Object instance, Object... args);
	public abstract int args();
	public boolean isStatic() {
		return false;
	}
	
	public static void register(String id, Method m) {
		if(m.isStatic()) {
			InvokeStaticImpl.registerJava(id, m);
		} else {
			InvokeVirtualImpl.registerJava(id, m);
		}
	}
	
	public static void registerAll(Class<?> cls) {
		registerAll(cls, DEFAULT_MAPPER);
	}
	
	public static void registerAll(Class<?> cls, NameMapper mapper) {
		for(java.lang.reflect.Method m : cls.getMethods()) {
			if(m.getAnnotation(BindMethod.class) == null) continue;
			register(mapper.apply(m), new ReflectionInvoker(m));
		}
	}

	@FunctionalInterface
	public interface NameMapper {
		String apply(java.lang.reflect.Method method);
	}
	
	public static class ReflectionInvoker extends Method {
		private final java.lang.reflect.Method method;
		private final boolean isVoid;
		private final int args;
		
		public ReflectionInvoker(java.lang.reflect.Method method) {
			if(!Modifier.isPublic(method.getModifiers())) {
				throw new IllegalArgumentException("Only public methods can be bound");
			}
			this.method = method;
			this.isVoid = method.getReturnType() == void.class;
			this.args = method.getParameterCount();
		}
		
		@Override
		public Object run(Interpreter interpreter, Object instance, Object... args) {
			try {
				Object o = method.invoke(instance, args);
				if(isVoid) return VOID;
				return o;
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
        public int args() {
		    return args;
        }
		
		@Override
		public boolean isStatic() {
			return Modifier.isStatic(method.getModifiers());
		}
	}
}
