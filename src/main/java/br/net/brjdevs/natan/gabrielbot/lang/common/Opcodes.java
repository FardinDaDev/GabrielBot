package br.net.brjdevs.natan.gabrielbot.lang.common;

public enum Opcodes {
	LOADCONSTANT(0, 1), //load from constant pool
	LOAD(1, 1), //load local
	STORE(2, 1), //store local
	INVOKESTATIC(3, 2), //invoke static method
	INVOKEVIRTUAL(4, 2), //invoke non static method (basically static but pops an instance from operand stack)
	ICONST(5, 1), //int constant
	LCONST(6, 2), //long constant
	FCONST(7, 1), //float constant
	DCONST(8, 2), //double constant
	GOTO(9, 1), //goto (for if and loops)
	GOTOL(10, 1), //goto a label
	LABEL(11, 1), //label
	VRETURN(12, 0), //void return
	EQ(13, 1), //compares top 2 ints on the stack and goes to the give index if they're *not* equal
	NEQ(14, 1), //opposite of IEQ
	GT(15, 1), //greather than
	ST(16, 1), //smaller than
	STOREGLOBAL(17, 1),
	LOADGLOBAL(18, 1),
	ADD(19, 0),
	SUB(20, 0),
	MUL(21, 0),
	DIV(22, 0),
	MOD(23, 0),
	POW(24, 0),
	NEW(25, 1),
	ALOAD(26, 1),
	ASTORE(27, 2), //second arg -> 0 == normal mode (pop value then the array), 1 == inverse mode (pop array then the value), 2 == pop value, array and push array
	;
	
	public final int code;
	public final int narg;
	
	Opcodes(int code, int narg) {
		this.code = code;
		this.narg = narg;
	}
	
	public static Opcodes fromName(String name) {
		for(Opcodes o : Opcodes.values()) {
			if(o.name().equalsIgnoreCase(name)) return o;
		}
		return null;
	}
	
	public static Opcodes fromCode(int code) {
		for(Opcodes o : Opcodes.values()) {
			if(o.code == code) return o;
		}
		return null;
	}
}
