package br.net.brjdevs.natan.gabrielbot.lang.runtime;

import br.net.brjdevs.natan.gabrielbot.lang.common.ConstantPool;
import br.net.brjdevs.natan.gabrielbot.lang.common.Constants;
import br.net.brjdevs.natan.gabrielbot.lang.common.Opcodes;
import br.net.brjdevs.natan.gabrielbot.lang.compiler.MethodWriter;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ClassReader {
	private final DataInputStream dis;
	private Map<String, Method> methods;
	private ConstantPool pool;
	
	public ClassReader(DataInputStream dis) {
		this.dis = dis;
		try {
			if(dis.readInt() != Constants.IDENTIFIER) {
				throw new IllegalArgumentException("Invalid identifier");
			}
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public ClassReader(InputStream is) {
		this(new DataInputStream(is));
	}
	
	public ClassReader(byte[] data) {
		this(new ByteArrayInputStream(data));
	}
	
	private void load() {
		boolean cp = false;
		Map<String, Method> map = new HashMap<>();
		ConstantPool constants = null;
		try {
			while(dis.available() > 0) {
				int c = dis.readInt();
				if(c == Constants.METHOD) {
					if(!cp) throw new IllegalArgumentException("Method declaration before constant pool");
					int flags = dis.readInt();
					int argCount = dis.readInt();
					String name = dis.readUTF();
					int ops = dis.readInt();
					OpcodeAndArgs[] code = new OpcodeAndArgs[ops];
					for(int i = 0; i < ops; i++) {
						int opcode = dis.readInt();
						Opcodes op = Opcodes.fromCode(opcode);
						if(op == null) throw new IllegalArgumentException("Unknown opcode " + opcode);
						int[] args = new int[op.narg];
						for(int j = 0; j < args.length; j++) {
							args[j] = dis.readInt();
						}
						code[i] = new OpcodeAndArgs(op, args);
					}
					map.put(name, new Method(flags, argCount, code));
				} else if(c == Constants.CONSTANT_POOL_START) {
					if(cp) throw new IllegalArgumentException("Multiple constant pools");
					cp = true;
					constants = ConstantPool.readFrom(dis);
				}
			}
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
		methods = map;
		pool = constants;
	}
	
	public Map<String, Method> methods() {
		if(methods == null) load();
		return new HashMap<>(methods);
	}
	
	public ConstantPool getConstants() {
		if(pool == null) load();
		return pool;
	}
	
	public static class Method {
		public final int flags;
		public final int argCount;
		public final OpcodeAndArgs[] code;
		
		Method(int flags, int argCount, OpcodeAndArgs[] code) {
			this.flags = flags;
			this.argCount = argCount;
			this.code = code;
		}

		public boolean isStatic() {
		    return (flags & MethodWriter.ACC_STATIC) != 0;
        }

        public boolean isExtern() {
		    return (flags & MethodWriter.EXTERN) != 0;
        }
	}
	
	public static class OpcodeAndArgs {
		public final Opcodes code;
		public final int[] args;
		
		OpcodeAndArgs(Opcodes code, int[] args) {
			this.code = code;
			this.args = args;
		}
	}
}
