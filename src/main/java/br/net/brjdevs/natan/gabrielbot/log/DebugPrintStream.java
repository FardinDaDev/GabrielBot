package br.net.brjdevs.natan.gabrielbot.log;

import java.io.OutputStream;
import java.io.PrintStream;

public class DebugPrintStream extends PrintStream {
    public DebugPrintStream(OutputStream out) {
        super(out);
    }

    @Override
    public void println(String s) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String current = stackTrace[2].toString();
        int i = 3;
        while ((current.startsWith("sun.") || current.startsWith("java.")) && i < stackTrace.length)
            current = stackTrace[i++].toString();
        super.println("[" + current + "]: " + s);
    }
}
