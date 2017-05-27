package br.net.brjdevs.natan.gabrielbot.lang.compiler;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import br.net.brjdevs.natan.gabrielbot.lang.common.ConstantPool;
import br.net.brjdevs.natan.gabrielbot.lang.common.Constants;

public class ClassWriter {
	private final DataOutputStream dos;
	private boolean constantsWritten = false;
	
	public ClassWriter(DataOutputStream dos) {
		this.dos = dos;
		writeInt(Constants.IDENTIFIER);
	}
	
	public ClassWriter(OutputStream os) {
		this(new DataOutputStream(os));
	}
	
	public synchronized void writeConstants(ConstantPool pool) {
		if(constantsWritten) throw new IllegalStateException();
		constantsWritten = true;
		pool.writeTo(dos);
	}
	
	public MethodWriter method(String name, int flags, int args) {
		return new MethodWriter(this, name, flags, args);
	}
	
	public MethodWriter method(String name, int args) {
		return method(name, MethodWriter.ACC_STATIC, args);
	}
	
	void checkMethod() {
		if(!constantsWritten) throw new IllegalStateException();
	}
	
	void writeInt(int i) {
		try {
			dos.writeInt(i);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	void writeUTF(String utf) {
		try {
			dos.writeUTF(utf);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	void writeBytes(byte[] bytes) {
		try {
			dos.write(bytes);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
}
