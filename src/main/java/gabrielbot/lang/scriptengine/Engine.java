package gabrielbot.lang.scriptengine;

import gabrielbot.lang.common.Opcodes;
import gabrielbot.lang.compiler.Parser;
import gabrielbot.lang.runtime.Interpreter;
import gabrielbot.lang.runtime.Verifier;
import gabrielbot.lang.runtime.invoke.Method;
import gabrielbot.lang.runtime.opcodes.InvokeStaticImpl;
import gabrielbot.lang.runtime.opcodes.InvokeVirtualImpl;
import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Engine extends AbstractScriptEngine {
    private List<Bindings> bindings = new ArrayList<>();

    @Override
    @SuppressWarnings("unchecked")
    public Object eval(String script, ScriptContext context) throws ScriptException {
        try {
            Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
            if(bindings == null) bindings = new SimpleBindings();
            for(Bindings b : this.bindings) {
                bindings.putAll(b);
            }
            Object args = bindings == null ? null : bindings.get("args");
            Object methods = bindings == null ? null : bindings.get("methods");
            byte[] classBytes;
            try {
                classBytes = Parser.parse(script);
                Verifier.verify(classBytes);
            } catch(Exception e) {
                throw new ScriptException(e);
            }
            Interpreter interpreter = new Interpreter(classBytes, args instanceof String[] ? (String[])args : new String[0]);
            if(methods instanceof Map) {
                InvokeStaticImpl isi = (InvokeStaticImpl)interpreter.getOpcodeImplementationByOpcode(Opcodes.INVOKESTATIC);
                InvokeVirtualImpl ivi = (InvokeVirtualImpl) interpreter.getOpcodeImplementationByOpcode(Opcodes.INVOKEVIRTUAL);
                Map<String, Method> map = (Map<String, Method>)methods;
                map.forEach((name, method)->{
                    if(method.isStatic()) {
                        isi.register(name, method);
                    } else {
                        ivi.register(name, method);
                    }
                });
            }
            bindings.forEach((key, value)->{
                if("args".equals(key) || "methods".equals(key)) return;
                interpreter.globals.put(key, value);
            });
            return interpreter.call();
        } catch(Exception e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        StringBuilder sb = new StringBuilder();
        try {
            int i;
            while((i = reader.read()) != -1) {
                sb.append((char)i);
            }
        } catch(IOException e) {
            throw new ScriptException(e);
        }
        return eval(sb.toString(), context);
    }

    @Override
    public Bindings createBindings() {
        Bindings b = new SimpleBindings();
        bindings.add(b);
        return b;
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return Factory.INSTANCE;
    }
}
