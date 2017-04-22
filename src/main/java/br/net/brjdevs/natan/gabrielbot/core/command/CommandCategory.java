package br.net.brjdevs.natan.gabrielbot.core.command;

public enum CommandCategory {
    MISC(CommandPermission.USER),
    INFO(CommandPermission.USER),
    IMAGE(CommandPermission.USER),
    FUN(CommandPermission.USER),
    MODERATION(CommandPermission.ADMIN),
    OWNER(CommandPermission.OWNER);

    public final CommandPermission permissionRequired;

    CommandCategory(CommandPermission permissionRequired) {
        this.permissionRequired = permissionRequired;
    }
}
