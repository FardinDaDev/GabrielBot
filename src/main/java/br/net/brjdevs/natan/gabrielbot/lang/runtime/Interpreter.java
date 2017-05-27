package br.net.brjdevs.natan.gabrielbot.lang.runtime;

import br.net.brjdevs.natan.gabrielbot.lang.common.Opcodes;
import br.net.brjdevs.natan.gabrielbot.lang.compiler.MethodWriter;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.ALoadImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.AStoreImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.AddImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.DConstImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.DivImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.EqImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.FConstImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.GotoImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.GotoLImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.GtImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.IConstImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.InvokeStaticImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.InvokeVirtualImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.LConstImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.LabelImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.LoadConstantImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.LoadGlobalImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.LoadImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.ModImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.MulImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.NeqImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.NewImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.PowImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.StImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.StoreGlobalImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.StoreImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.opcodes.SubImpl;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.invoke.InterpretedMethod;
import br.net.brjdevs.natan.gabrielbot.lang.runtime.invoke.Method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class Interpreter implements Runnable, Callable<Object>, Supplier<Object> {
	public static final int MAX_STACK_SIZE = 50;
	public static final int DEBUG_OPCODES = 1;
	public static final int DEBUG_MEMORY = 2;
	
	private final OpcodeImplementation[] impl = new OpcodeImplementation[Opcodes.values().length];
	
	public final List<String> contantPool = new ArrayList<>();
	public final Stack<Object> operandStack = new Stack<>();
	public final Map<Integer, Object> locals = new HashMap<>();
	public final Map<String, Integer> labels = new HashMap<>();
	public final Map<String, Object> globals;
	
	private final Map<String, ClassReader.Method> methods;
	private final ClassReader.OpcodeAndArgs[] main;
	private final boolean debugOps;
	private final boolean debugMem;
	private final int stackDepth;
	private int current = 0;
	private Object ret;
	
	private Interpreter(Interpreter other, ClassReader.Method method, Object... args) {
		if((this.stackDepth = other.stackDepth + 1) > MAX_STACK_SIZE) {
			throw new StackOverflowError("User code");
		}
		for(int i = 0; i < impl.length; i++) {
			impl[i] = other.impl[i];
		}
		this.contantPool.addAll(other.contantPool);
		for(int i = 0; i < args.length; i++) {
			this.locals.put(i, args[i]);
		}
		this.globals = other.globals;
		this.methods = null; //already loaded on the invoke opcodes in the impl table
		this.main = method.code;
		this.debugOps = other.debugOps;
		this.debugMem = other.debugMem;
	}
	
	public Interpreter forMethod(ClassReader.Method method, Object... args) {
		return new Interpreter(this, method, args);
	}
	
	public Interpreter(ClassReader reader, String[] args, int debug) {
		this.globals = new HashMap<>();
		this.stackDepth = 1;
		this.methods = reader.methods();
		ClassReader.Method m = methods.get("main");
		this.main = m == null ? null : (m.flags & MethodWriter.ACC_STATIC) != 0 ? m.argCount == 1 ? m.code : null : null;
		this.locals.put(0, Array.fromArray(args));
		this.contantPool.addAll(reader.getConstants().asList());
		this.debugOps = (debug & DEBUG_OPCODES) != 0;
		this.debugMem = (debug & DEBUG_MEMORY) != 0;
		for(Opcodes o : Opcodes.values()) {
			switch(o) {
				case LOADCONSTANT:
					impl[o.code] = new LoadConstantImpl();
					break;
				case LOAD:
					impl[o.code] = new LoadImpl();
					break;
				case STORE:
					impl[o.code] = new StoreImpl();
					break;
				case INVOKESTATIC:
					InvokeStaticImpl isi = new InvokeStaticImpl();
					impl[o.code] = isi;
					for(Map.Entry<String, ClassReader.Method> entry : methods.entrySet()) {
                        ClassReader.Method mtd = entry.getValue();
                        if(!mtd.isStatic() || mtd.isExtern()) continue;
                        isi.register(entry.getKey(), new InterpretedMethod(mtd));
					}
					break;
				case INVOKEVIRTUAL:
					InvokeVirtualImpl ivi = new InvokeVirtualImpl();
					impl[o.code] = ivi;
					for(Map.Entry<String, ClassReader.Method> entry : methods.entrySet()) {
                        ClassReader.Method mtd = entry.getValue();
                        if(mtd.isStatic() || mtd.isExtern()) continue;
						ivi.register(entry.getKey(), new InterpretedMethod(mtd));
					}
					break;
				case ICONST:
					impl[o.code] = new IConstImpl();
					break;
				case LCONST:
					impl[o.code] = new LConstImpl();
					break;
				case FCONST:
					impl[o.code] = new FConstImpl();
					break;
				case DCONST:
					impl[o.code] = new DConstImpl();
					break;
				case GOTO:
					impl[o.code] = new GotoImpl();
					break;
				case GOTOL:
					impl[o.code] = new GotoLImpl();
					break;
				case LABEL:
					impl[o.code] = new LabelImpl(); //noop
					break;
				case EQ:
					impl[o.code] = new EqImpl();
					break;
				case NEQ:
					impl[o.code] = new NeqImpl();
					break;
				case GT:
					impl[o.code] = new GtImpl();
					break;
				case ST:
					impl[o.code] = new StImpl();
					break;
				case STOREGLOBAL:
					impl[o.code] = new StoreGlobalImpl();
					break;
				case LOADGLOBAL:
					impl[o.code] = new LoadGlobalImpl();
					break;
				case ADD:
					impl[o.code] = new AddImpl();
					break;
				case SUB:
					impl[o.code] = new SubImpl();
					break;
				case MUL:
					impl[o.code] = new MulImpl();
					break;
				case DIV:
					impl[o.code] = new DivImpl();
					break;
				case MOD:
					impl[o.code] = new ModImpl();
					break;
				case POW:
					impl[o.code] = new PowImpl();
					break;
				case NEW:
					impl[o.code] = new NewImpl();
					break;
				case ALOAD:
					impl[o.code] = new ALoadImpl();
					break;
				case ASTORE:
					impl[o.code] = new AStoreImpl();
					break;
			}
		}
		if(main != null) {
			for(int i = 0; i < main.length; i++) {
				Opcodes c = main[i].code;
				if(c == Opcodes.LABEL) {
					labels.put(contantPool.get(main[i].args[0]), i+1);
				}
			}
		}
	}
	
	public Interpreter(ClassReader reader, String[] args) {
		this(reader, args, 0);
	}
	
	public Interpreter(byte[] data, String[] args, int debug) {
		this(new ClassReader(data), args, debug);
	}
	
	public Interpreter(byte[] data, String[] args) {
		this(new ClassReader(data), args, 0);
	}
	
	public void gotoInstruction(int index) {
		current = index;
	}
	
	public boolean step() {
		if(current >= main.length) throw new IllegalStateException();
		ClassReader.OpcodeAndArgs op = main[current++];
		if(debugOps) System.out.println(current-1 + " -> " + op.code + Arrays.toString(op.args).replace('[', '(').replace(']', ')'));
		switch(op.code) {
			case VRETURN:
				current = main.length;
				ret = Method.VOID;
				return false;
			default:
				OpcodeImplementation o = impl[op.code.code];
				if(o == null) throw new UnsupportedOperationException("Uninplemented opcode: " + op.code);
				o.run(this, op.args);
				if(debugMem) {
					System.out.println("O: " + operandStack);
					System.out.println("L: " + locals);
					System.out.println("C: " + contantPool);
					System.out.println("G: " + globals);
				}
				break;
		}
		return current < main.length;
	}
	
	public boolean reset() {
		return (current = 0) < main.length;
	}
	
	public OpcodeImplementation getOpcodeImplementationByOpcode(Opcodes code) {
		if(code == null) return null;
		return impl[code.code];
	}
	
	public OpcodeImplementation getOpcodeImplementaionById(int id) {
		return getOpcodeImplementationByOpcode(Opcodes.fromCode(id));
	}
	
	public OpcodeImplementation getOpcodeImplementationByName(String name) {
		return getOpcodeImplementationByOpcode(Opcodes.fromName(name));
	}
	
	@Override
	public void run() {
	    call();
	}
	
	@Override
	public Object call() {
		if(main == null) throw new IllegalStateException("No main function defined");
		if(!reset()) return Method.VOID;
		while(step());
		return ret;
	}

    @Override
    public Object get() {
        return call();
    }

    public static boolean isVoid(Object o) {
		return o == Method.VOID;
	}
}
