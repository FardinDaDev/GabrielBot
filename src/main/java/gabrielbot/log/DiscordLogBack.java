package gabrielbot.log;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import gabrielbot.GabrielBot;
import gabrielbot.utils.Utils;

public class DiscordLogBack extends AppenderBase<ILoggingEvent> {
    private static boolean enabled = false;

    public static void enable() {
        enabled = true;
    }

    private PatternLayout patternLayout;

    @Override
    protected void append(ILoggingEvent event) {
        if(!enabled) return;
        String toSend = patternLayout.doLayout(event);
        if(toSend.contains("INFO") && toSend.contains("RemoteNodeProcessor")) return;
        if(toSend.length() > 1920)
            toSend = ":warning: Received a message but it was too long, Hastebin: " + Utils.paste(toSend);
        GabrielBot.getInstance().log(toSend);
    }

    @Override
    public void start() {
        patternLayout = new PatternLayout();
        patternLayout.setContext(getContext());
        patternLayout.setPattern("[%d{HH:mm:ss}] [%t/%level] [%logger{0}]: %msg");
        patternLayout.start();

        super.start();
    }
}
