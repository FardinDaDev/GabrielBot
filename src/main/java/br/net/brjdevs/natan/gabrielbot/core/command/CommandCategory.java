package br.net.brjdevs.natan.gabrielbot.core.command;

import br.net.brjdevs.natan.gabrielbot.utils.commands.EmoteReference;

public enum CommandCategory {
    MUSIC(CommandPermission.USER, EmoteReference.HEADPHONE),
    MISC(CommandPermission.USER, EmoteReference.BLACK_JOKER),
    INFO(CommandPermission.USER, EmoteReference.BALLOT_BOX),
    IMAGE(CommandPermission.USER, EmoteReference.FRAME_PHOTO),
    FUN(CommandPermission.USER, EmoteReference.POPPER),
    MODERATION(CommandPermission.ADMIN, EmoteReference.HAMMER),
    OWNER(CommandPermission.OWNER, EmoteReference.GEAR);

    public final CommandPermission permissionRequired;
    public final EmoteReference emote;

    CommandCategory(CommandPermission permissionRequired, EmoteReference emote) {
        this.permissionRequired = permissionRequired;
        this.emote = emote;
    }
}
