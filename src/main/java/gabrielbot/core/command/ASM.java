package gabrielbot.core.command;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.BiFunction;

class ASM implements Opcodes {
    private static final BiFunction<ClassLoader, byte[], Class<?>> DEFINE_CLASS;

    static {
        try {
            Method m = ClassLoader.class.getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
            m.setAccessible(true);
            DEFINE_CLASS = (loader, bytes)->{
                try {
                    return (Class<?>)m.invoke(loader, bytes, 0, bytes.length);
                } catch(IllegalAccessException e) {
                    throw new AssertionError(e);
                } catch(InvocationTargetException e) {
                    throw new AssertionError(e.getCause());
                }
            };
        } catch(Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static CommandInvoker invoker(Method method, ClassLoader classLoader) {
        String className = ASM.class.getName() + "$0Invoker___" + method.getDeclaringClass().getName().replace(".", "_").replace("$", "__") + "___" + method.getName() + "_" + Type.getMethodDescriptor(method).replace("(", "____").replace(")", "____").replace(';', '_').replace('/', '_');
        try {
            return (CommandInvoker)classLoader.loadClass(className).newInstance();
        } catch(ClassNotFoundException e) {
            //define the class
        } catch(Exception e) {
            throw new AssertionError(e);
        }
        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        String[] args = new String[paramAnnotations.length];
        int i = 0;
        for(Annotation[] array : paramAnnotations) {
            for(Annotation annotation : array) {
                if(annotation instanceof Argument) {
                    args[i] = ((Argument) annotation).value();
                }
            }
            i++;
        }

        className = className.replace('.', '/');

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_8, ACC_PUBLIC + ACC_STATIC + ACC_SUPER, className, null, "gabrielbot/core/command/CommandInvoker", null);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "invoke", "(Ljava/util/Map;)V",  null, null);
        mv.visitCode();

        Class<?>[] argTypes = method.getParameterTypes();
        i = 0;
        for(String s : args) {
            Class<?> arg = argTypes[i++];
            if(arg.isPrimitive()) throw new UnsupportedOperationException("primitive arguments");
            if(s == null) {
                mv.visitInsn(ACONST_NULL);
            } else {
                mv.visitVarInsn(ALOAD, 1);
                mv.visitLdcInsn(s);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
                mv.visitTypeInsn(CHECKCAST, arg.getName().replace('.', '/'));
            }
        }
        mv.visitMethodInsn(INVOKESTATIC, method.getDeclaringClass().getName().replace('.', '/'), method.getName(), Type.getMethodDescriptor(method), false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(args.length, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "gabrielbot/core/command/CommandInvoker", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();

        cw.visitEnd();

        try {
            byte[] bytes = cw.toByteArray();
            return (CommandInvoker)DEFINE_CLASS.apply(classLoader, bytes).newInstance();
        } catch(Exception e) {
            throw new AssertionError(e);
        }
    }
}
