package br.net.brjdevs.natan.gabrielbot.utils.lua;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public class LuaLambda {
    @SuppressWarnings("unchecked")
    public static <T> T toLambda(ClassLoader classLoader, Class<T> lambdaClass, LuaFunction function) {
        if(!lambdaClass.isInterface() || lambdaClass.getAnnotation(FunctionalInterface.class) == null) throw new UnsupportedOperationException(lambdaClass + " is not a FunctionalInterface");
        Method lambdaMethod = findLambdaMethod(lambdaClass);

        return (T)Proxy.newProxyInstance(classLoader, new Class[]{lambdaClass}, new LambdaInvocationHandler(lambdaMethod, function));
    }

    public static boolean isLambda(Class<?> clazz) {
        return clazz.isInterface() && clazz.getAnnotation(FunctionalInterface.class) != null;
    }

    private static Method findLambdaMethod(Class<?> lambdaClass) {
        for(Method method : lambdaClass.getMethods()) {
            if(method.isDefault() || Modifier.isStatic(method.getModifiers())) continue;
            return method;
        }
        throw new AssertionError();
    }

    private static class LambdaInvocationHandler implements InvocationHandler {
        private final String methodName;
        private final Class<?>[] methodArgs;
        private final LuaFunction callback;

        LambdaInvocationHandler(Method method, LuaFunction callback) {
            this.methodName = method.getName();
            this.methodArgs = method.getParameterTypes();
            this.callback = callback;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if(method.getName().equals(methodName) && Arrays.equals(method.getParameterTypes(), methodArgs)) {
                LuaValue[] luaArgs = new LuaValue[args.length];
                for(int i = 0; i < luaArgs.length; i++) {
                    luaArgs[i] = LuaHelper.toLua(args[i]);
                }
                LuaValue obj = callback.invoke(luaArgs).arg1();
                Class<?> ret = method.getReturnType();
                if(ret == void.class) return null;
                switch(ret.getName()) {
                    case "boolean":
                    case "java.lang.Boolean":
                        return obj.isboolean() ? obj.toboolean() : !obj.isnil();
                    case "char":
                    case "java.lang.Character":
                        return obj.tochar();
                    case "byte":
                    case "java.lang.Byte":
                        return obj.checknumber().tobyte();
                    case "short":
                    case "java.lang.Short":
                        return obj.checknumber().toshort();
                    case "int":
                    case "java.lang.Integer":
                        return obj.checknumber().toint();
                    case "float":
                    case "java.lang.Float":
                        return obj.checknumber().tofloat();
                    case "long":
                    case "java.lang.Long":
                        return obj.checknumber().tolong();
                    case "double":
                    case "java.lang.Double":
                        return obj.checknumber().todouble();
                }
                return LuaHelper.getInstance(obj, Object.class);
            }
            return method.invoke(proxy, args);
        }
    }
}
