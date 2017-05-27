package br.net.brjdevs.natan.gabrielbot.lang.compiler;

import java.io.DataOutputStream;
import java.io.IOException;

import br.net.brjdevs.natan.gabrielbot.lang.common.Constants;
import br.net.brjdevs.natan.gabrielbot.lang.common.Opcodes;

import java.io.ByteArrayOutputStream;

public class MethodWriter {
	public static final int ACC_STATIC = 1;
	public static final int EXTERN = 2;
	
	private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private final DataOutputStream dos = new DataOutputStream(baos);
	private final ClassWriter cw;
	private final String name;
	private final int flags;
	private final int args;
	private int ops;
	
	MethodWriter(ClassWriter cw, String name, int flags, int args) {
		this.cw = cw;
		this.name = name;
		this.flags = flags;
		this.args = args;
	}
	
	public MethodWriter writeOpcode(Opcodes code, int... args) {
		if(args == null) args = new int[0];
		if(args.length != code.narg) throw new IllegalArgumentException("Number of args does not match opcode requirements");
		ops++;
		writeInt(code.code);
		for(int i : args) writeInt(i);
		return this;
	}
	
	public ClassWriter writeToClass() {
		cw.checkMethod();
		cw.writeInt(Constants.METHOD);
		cw.writeInt(flags);
		cw.writeInt(args);
		cw.writeUTF(name);
		cw.writeInt(ops);
		cw.writeBytes(baos.toByteArray());
		return cw;
	}
	
	private void writeInt(int i) {
		try {
			dos.writeInt(i);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
}
