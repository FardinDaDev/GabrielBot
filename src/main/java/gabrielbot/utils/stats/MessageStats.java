package gabrielbot.utils.stats;

public class MessageStats {
    private volatile static long messages = 0;
    private volatile static long commands = 0;

    public static void message() {
        messages++;
    }

    public static long getMessages() {
        return messages;
    }

    public static void command() {
        commands++;
    }

    public static long getCommands() {
        return commands;
    }
}
