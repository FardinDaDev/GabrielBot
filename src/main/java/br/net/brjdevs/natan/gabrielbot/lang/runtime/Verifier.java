package br.net.brjdevs.natan.gabrielbot.lang.runtime;

import br.net.brjdevs.natan.gabrielbot.lang.common.ConstantPool;
import br.net.brjdevs.natan.gabrielbot.lang.common.Constants;

import java.util.Map;

public class Verifier {
    private Verifier() {}

    public static void verify(ClassReader reader) {
        Map<String, ClassReader.Method> methods = reader.methods();
        ConstantPool cp = reader.getConstants();
        methods.values().forEach(method->{
            for(ClassReader.OpcodeAndArgs o : method.code) {
                switch(o.code) {
                    case INVOKESTATIC: {
                        String name = cp.get(o.args[0]);
                        if(isInternalMethod(name)) break;
                        ClassReader.Method m = methods.get(name);
                        if(m == null) throw new InvalidBytecodeException("Undeclared method " + name);
                        if(m.argCount != o.args[1]) {
                            throw new InvalidBytecodeException("Method " + name + " takes " + m.argCount + " args, but was called with " + o.args[1] + " args");
                        }
                        if(!m.isStatic()) {
                            throw new InvalidBytecodeException("Method " + name + " is not static but was called with an INVOKESTATIC opcode");
                        }
                    } break;
                    case INVOKEVIRTUAL: {
                        String name = cp.get(o.args[0]);
                        if(isInternalMethod(name)) break;
                        ClassReader.Method m = methods.get(name);
                        if(m == null) throw new InvalidBytecodeException("Undeclared method " + name);
                        if(m.argCount != o.args[1]) {
                            throw new InvalidBytecodeException("Method " + name + " takes " + m.argCount + " args, but was called with " + o.args[1] + " args");
                        }
                        if(m.isStatic()) {
                            throw new InvalidBytecodeException("Method " + name + " is static but was called with an INVOKEVIRTUAL opcode");
                        }
                    } break;
                }
            }
        });
    }

    public static void verify(byte[] bytecode) {
        verify(new ClassReader(bytecode));
    }

    private static boolean isInternalMethod(String name) {
        return
                Constants.STRING_APPEND.equals(name) ||
                Constants.NEW_ARRAY.equals(name) ||
                Constants.NEW_STRING.equals(name);
    }

    public static class InvalidBytecodeException extends RuntimeException {
        InvalidBytecodeException(String s) {
            super(s);
        }
    }
}
