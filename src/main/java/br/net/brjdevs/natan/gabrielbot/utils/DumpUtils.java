package br.net.brjdevs.natan.gabrielbot.utils;

import javax.management.MBeanServer;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.nio.charset.Charset;

public class DumpUtils {
    private static volatile Object hotspotMBean;

    private DumpUtils(){}

    public static void dumpThreads(String file) throws IOException {
        StringBuilder dump = new StringBuilder();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 100);
        for (ThreadInfo threadInfo : threadInfos) {
            dump.append('"');
            dump.append(threadInfo.getThreadName());
            dump.append("\" ");
            final Thread.State state = threadInfo.getThreadState();
            dump.append("\n   java.lang.Thread.State: ");
            dump.append(state);
            final StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();
            for (final StackTraceElement stackTraceElement : stackTraceElements) {
                dump.append("\n        at ");
                dump.append(stackTraceElement);
            }
            dump.append("\n\n");
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(dump.toString().getBytes(Charset.defaultCharset()));
        try(FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            int read;
            while((read = bais.read(buffer)) > -1) {
                fos.write(buffer, 0, read);
            }
        }
    }

    public static void dumpHeap(String file) {
        initHotspotMBean();
        try {
            Class<?> clazz = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
            Method m = clazz.getMethod("dumpHeap", String.class, boolean.class);
            m.invoke(hotspotMBean, file, true);
        } catch(RuntimeException re) {
            throw re;
        } catch(Exception exp) {
            throw new RuntimeException(exp);
        }
    }

    // initialize the hotspot diagnostic MBean field
    private static void initHotspotMBean() {
        if(hotspotMBean == null) {
            synchronized(DumpUtils.class) {
                if(hotspotMBean == null) {
                    try {
                        Class<?> clazz = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
                        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                        hotspotMBean = ManagementFactory.newPlatformMXBeanProxy(server, "com.sun.management:type=HotSpotDiagnostic", clazz);
                    } catch(RuntimeException re) {
                        throw re;
                    } catch(Exception exp) {
                        throw new RuntimeException(exp);
                    }
                }
            }
        }
    }
}
