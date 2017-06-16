package br.net.brjdevs.natan.gabrielbot.core.command;

import br.net.brjdevs.natan.gabrielbot.utils.commands.EmoteReference;

public enum CommandCategory {
    MUSIC(EmoteReference.HEADPHONE),
    MISC(EmoteReference.BLACK_JOKER),
    INFO(EmoteReference.BALLOT_BOX),
    IMAGE(EmoteReference.FRAME_PHOTO),
    FUN(EmoteReference.POPPER),
    GAME(EmoteReference.VIDEO_GAME),
    UTIL(EmoteReference.WRENCH),
    CODE(EmoteReference.KEYBOARD),
    LARS(EmoteReference.LARS_X_KODE),
    MODERATION(EmoteReference.HAMMER),
    OWNER(EmoteReference.GEAR);

    public final EmoteReference emote;

    CommandCategory(EmoteReference emote) {
        this.emote = emote;
    }
}
