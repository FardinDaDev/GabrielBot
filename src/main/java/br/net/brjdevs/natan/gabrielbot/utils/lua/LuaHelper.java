package br.net.brjdevs.natan.gabrielbot.utils.lua;

import br.net.brjdevs.natan.gabrielbot.utils.brainfuck.BrainfuckInterpreter;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.LongStream;

public class LuaHelper {
    private static final String INSTANCE_KEY = "instance";

    private static final LuaFunction __eq = new LibFunction() {
        @Override
        public LuaValue call(LuaValue first, LuaValue second) {
            if(!(first.istable() && second.istable())) return FALSE;
            LuaValue one = getInstance(first), two = getInstance(second);
            if(!(one.isuserdata(Instance.class) && two.isuserdata(Instance.class))) return FALSE;
            return valueOf(((Instance)CoerceLuaToJava.coerce(one, Instance.class)).instance.equals(((Instance)CoerceLuaToJava.coerce(two, Instance.class)).instance));
        }
    };

    public static Globals setup(int maxCycles) {
        return setup(null, maxCycles, 0, null, null);
    }

    public static Globals setup(int maxCycles, String input) {
        return setup(null, maxCycles, 0, input, null);
    }

    public static Globals setup(GuildMessageReceivedEvent event, int maxCycles, int maxMessages, String input) {
        return setup(event, maxCycles, maxMessages, input, null);
    }

    public static Globals setup(GuildMessageReceivedEvent event, int maxCycles, int maxMessages, String input, StringBuilder out) {
        Globals globals = new Globals();
        globals.load(new PackageLib());
        globals.load(new JseBaseLib());
        globals.load(new Bit32Lib());
        globals.load(new TableLib());
        globals.load(new StringLib());
        globals.load(new JseMathLib());
        LoadState.install(globals);
        LuaC.install(globals);
        CycleLimiter limiter = new CycleLimiter(maxCycles);
        globals.load(limiter);
        globals.set("package", LuaValue.NIL);
        globals.set("debug", LuaValue.NIL);
        globals.set("dofile", LuaValue.NIL);
        globals.set("loadfile", LuaValue.NIL);
        globals.set("require", LuaValue.NIL);
        if(out == null) {
            globals.set("print", new LibFunction() {});//noop
        } else {
            globals.set("print", new VarArgFunction() {
                @Override
                public Varargs onInvoke(Varargs args) {
                    for(int i = 1, n=args.narg(); i <= n; i++) {
                        if (i > 1) out.append('\t');
                        out.append(args.arg(i).tojstring());
                    }
                    out.append('\n');
                    return NONE;
                }
            });
        }

        globals.set("input", input);
        LuaValue[] args = Arrays.stream(input.split(" ")).map(LuaValue::valueOf).toArray(LuaValue[]::new);
        if(args.length != 0) globals.set("args", new LuaTable(null, args, null));
        if(event != null) {
            SafeGuildMessageReceivedEvent e = new SafeGuildMessageReceivedEvent(event, maxMessages);
            globals.set("author", coerce(e.getAuthor()));
            globals.set("channel", coerce(e.getChannel()));
            globals.set("member", coerce(e.getMember()));
            globals.set("guild", coerce(e.getGuild()));
            globals.set("message", coerce(e.getMessage()));
            globals.set("event", coerce(e));
        }

        extraFunctions(globals);

        return globals;
    }

    public static void extraFunctions(Globals globals) {
        globals.set("brainfuck", new LibFunction() {
            @Override
            public Varargs invoke(Varargs v) {
                String code = v.arg1().checkstring().tojstring();
                LuaValue i = v.arg(2);
                String input = i.isnil() ? "" : i.checkstring().tojstring();
                BrainfuckInterpreter bf = new BrainfuckInterpreter(1000, 1<<10);
                try {
                    return valueOf(bf.process(code.toCharArray(), input));
                } catch(BrainfuckInterpreter.BrainfuckException e) {
                    return varargsOf(new LuaValue[]{NIL, valueOf(e.getMessage())});
                }
            }
        });

        LuaTable stream = new LuaTable();
        stream.set("of", new VarArgFunction() {
            @Override
            public Varargs onInvoke(Varargs args) {
                LuaValue[] values = new LuaValue[args.narg()];
                for(int i = 0; i < values.length;) {
                    values[i] = args.arg(++i);
                }
                return coerce(Arrays.stream(values).map(v->getInstance(v, Object.class)));
            }
        });
        stream.set("range", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                long l1 = arg1.checklong();
                long l2 = arg2.checklong();
                if(l1 > l2) {
                    long temp = l1;
                    l1 = l2;
                    l2 = temp;
                }
                return coerce(LongStream.range(l1, l2).mapToObj(Long::valueOf));
            }
        });

        globals.set("stream", proxyTable(stream, true));
    }

    public static CycleLimiter getLimiter(Globals globals) {
        DebugLib l = globals.debuglib;
        if(l == null) return null;
        if(l instanceof CycleLimiter) {
            return (CycleLimiter)l;
        }
        throw new IllegalArgumentException("Provided globals were not created using setup()");
    }

    public static LuaTable proxyTable(LuaTable original, boolean readonly) {
        LuaTable t = new LuaTable();
        LuaTable mt = new LuaTable();
        mt.set("__index", original);
        if(readonly) mt.set("__newindex", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return error("Cannot be modified");
            }
        });
        t.setmetatable(mt);
        return t;
    }

    static LuaValue getInstance(LuaValue t) {
        return t.istable() ? t.getmetatable() != null ? t.getmetatable().istable() ? t.getmetatable().get(INSTANCE_KEY) : LuaValue.NIL : LuaValue.NIL : LuaValue.NIL;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getInstance(LuaValue lua, Class<T> clazz) {
        if(lua == null) return null;
        if(lua.isnil()) return null;
        if(lua.istable()) {
            LuaValue mt = lua.getmetatable();
            if(mt.istable()) {
                return clazz.cast(((Instance)CoerceLuaToJava.coerce(mt.get(INSTANCE_KEY), Instance.class)).instance);
            }
        }
        return (T)CoerceLuaToJava.coerce(lua, clazz);
    }

    public static LuaValue toLua(Object obj) {
        LuaValue v = CoerceJavaToLua.coerce(obj);
        return v.isuserdata() ? LuaHelper.coerce(obj) : v;
    }

    public static LuaTable coerce(Object obj) {
        return coerce(ClassLoader.getSystemClassLoader(), obj);
    }

    public static LuaTable coerce(ClassLoader lambdaLoader, Object obj) {
        LuaTable object = new LuaTable();
        Class<?> cls = obj.getClass();
        for(Method m : cls.getMethods()) {
            Class<?> declaring = m.getDeclaringClass();
            if(declaring == Object.class) {
                switch(m.getName()) {
                    //not a good idea to let these stay
                    case "notify":
                    case "notifyAll":
                    case "wait":
                        continue;
                }
            } else if(declaring == Class.class) {
                switch(m.getName()) {
                    case "forName":
                    case "newInstance":
                    case "getResource":
                    case "getResourceAsStream":
                        continue;
                }
            }
            //no reflection
            Class<?> ret = m.getReturnType();
            while(ret.isArray()) ret = ret.getComponentType();
            if(ret.getName().startsWith("java.lang.reflect.") || ret.getName().startsWith("java.lang.annotation.") || ret == ClassLoader.class) continue;
            if(m.getDeclaringClass() == net.dv8tion.jda.core.entities.MessageChannel.class && m.getName().equals("sendMessage") && m.getParameterTypes()[0] != String.class) continue;
            if(m.getDeclaringClass() == net.dv8tion.jda.core.requests.RestAction.class && m.getName().equals("queue") && m.getParameterTypes().length != 2) continue;

            m.setAccessible(true);
            object.set(m.getName(), coerce(lambdaLoader, object, obj, m));
        }

        LuaTable ret = new LuaTable();
        LuaTable retMt = new LuaTable();
        Instance i = new Instance(obj);
        retMt.set(INSTANCE_KEY, CoerceJavaToLua.coerce(i));
        retMt.set("__index", object);
        retMt.set("__metatable", LuaValue.FALSE);
        retMt.set("__newindex", new LibFunction() {
            @Override
            public LuaValue call() {
                return error("Objects cannot be modified");
            }
        });
        retMt.set("__tostring", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return valueOf(obj.toString());
            }
        });
        retMt.set("__eq", __eq);
        retMt.set("__call", new LibFunction() {
            @Override
            public LuaValue call() {
                StringBuilder sb = new StringBuilder();
                for(LuaValue key : object.keys()) {
                    sb.append(key.tojstring()).append(", ");
                }
                if(sb.length() != 0) sb.deleteCharAt(sb.length()-2);
                return valueOf("Available methods: [" + sb + "]");
            }
        });

        ret.setmetatable(retMt);
        return ret;
    }

    public static LuaFunction coerce(Object instance, Method method) {
        return coerce(null, null, instance, method);
    }

    public static LuaFunction coerce(ClassLoader lambdaLoader, Object instance, Method method) {
        return coerce(lambdaLoader, null, instance, method);
    }

    @SuppressWarnings("unchecked")
    private static LuaFunction coerce(ClassLoader lambdaLoader, LuaTable object, Object instance, Method method) {
        Class<?>[] params = method.getParameterTypes();
        return new VarArgFunction() {
            @Override
            public Varargs invoke(LuaValue[] values) {
                Object[] args = new Object[params.length];
                for(int i = 0; i < Math.min(params.length, values.length); i++) {
                    Class<?> paramClass = params[i];
                    LuaValue v = values[i];
                    if(v.istable()) {
                        LuaValue l = getInstance(v);
                        if(l.isuserdata(Instance.class)) {
                            Object o = ((Instance)CoerceLuaToJava.coerce(l, Instance.class)).instance;
                            if(o != null && !params[i].isInstance(o)) {
                                return varargsOf(new LuaValue[]{NIL, valueOf("TypeError: expected " + paramClass.getName() + ", got " + o.getClass().getName())});
                            }
                            args[i] = o;
                            continue;
                        }
                    }
                    if(LuaLambda.isLambda(paramClass)) {
                        LuaFunction function = (LuaFunction)CoerceLuaToJava.coerce(v, LuaFunction.class);
                        args[i] = LuaLambda.toLambda(lambdaLoader, paramClass, function);
                    } else {
                        args[i] = CoerceLuaToJava.coerce(v, paramClass);
                    }
                }
                try {
                    Object o = method.invoke(instance, args);
                    if(o == instance && object != null) return object;
                    if(o == null) return NIL;
                    LuaValue v = CoerceJavaToLua.coerce(o);
                    if(v.isuserdata()) {
                        return coerce(lambdaLoader, o);
                    }
                    return v;
                } catch(InvocationTargetException e) {
                    throw new LuaError(e.getCause());
                } catch(Exception e) {
                    throw new LuaError(e);
                }
            }

            @Override
            public Varargs onInvoke(Varargs args) {
                LuaValue[] v = new LuaValue[args.narg()];
                for(int i = 0; i < v.length;) {
                    v[i] = args.arg(++i);
                }
                return invoke(v);
            }
        };
    }

    private static class Instance {
        final Object instance;

        Instance(Object instance) {
            this.instance = instance;
        }
    }
}
