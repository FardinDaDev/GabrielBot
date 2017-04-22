package br.net.brjdevs.natan.gabrielbot.log;

import br.net.brjdevs.natan.gabrielbot.GabrielBot;
import br.net.brjdevs.natan.gabrielbot.utils.Utils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class DiscordLogBack extends AppenderBase<ILoggingEvent> {
    private static boolean enabled = false;

    public static void disable() {
        enabled = false;
    }

    public static void enable() {
        enabled = true;
    }

    private PatternLayout patternLayout;
    private ILoggingEvent previousEvent;

    @Override
    protected void append(ILoggingEvent event) {
        if (!enabled) return;
        //if (!event.getLevel().isGreaterOrEqual(Level.INFO)) return;
        String toSend = patternLayout.doLayout(event);
        if (previousEvent != null && event.getMessage().equals(previousEvent.getMessage())) return;
        if (toSend.contains("INFO") && toSend.contains("RemoteNodeProcessor")) return;
        if (toSend.length() > 1920)
            toSend = ":warning: Received a message but it was too long, Hastebin: " + Utils.paste(toSend);
        GabrielBot.getInstance().log(toSend);
        previousEvent = event;
    }

    @Override
    public void start() {
        patternLayout = new PatternLayout();
        patternLayout.setContext(getContext());
        patternLayout.setPattern("```\n[%d{HH:mm:ss}] [%t/%level] [%logger{0}]: %msg```");
        patternLayout.start();

        super.start();
    }
}
