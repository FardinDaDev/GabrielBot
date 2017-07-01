package gabrielbot.lang.scriptengine;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import java.util.Collections;
import java.util.List;

public class Factory implements ScriptEngineFactory {
    static final Factory INSTANCE = new Factory();

    @Override
    public String getEngineName() {
        return "CustomCommands";
    }

    @Override
    public String getEngineVersion() {
        return "1.0";
    }

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("command");
    }

    @Override
    public List<String> getMimeTypes() {
        return Collections.singletonList("text/*");
    }

    @Override
    public List<String> getNames() {
        return Collections.singletonList("cc");
    }

    @Override
    public String getLanguageName() {
        return "CustomCommandLang";
    }

    @Override
    public String getLanguageVersion() {
        return "1.0";
    }

    @Override
    public Object getParameter(String key) {
        switch(key) {
            case ScriptEngine.ENGINE: return getEngineName();
            case ScriptEngine.ENGINE_VERSION: return getEngineVersion();
            case ScriptEngine.LANGUAGE: return getLanguageName();
            case ScriptEngine.LANGUAGE_VERSION: return getLanguageVersion();
            case ScriptEngine.NAME: return getNames().get(0);
            case "THREADING": return "STATELESS";
            default: return null;
        }
    }

    @Override
    public String getMethodCallSyntax(String obj, String m, String... args) {
        return m + "(" + String.join(",", args) + ");";
    }

    @Override
    public String getOutputStatement(String toDisplay) {
        return "println(" + toDisplay + ");";
    }

    @Override
    public String getProgram(String... statements) {
        return String.join(";", statements) + ";";
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new Engine();
    }
}
