package br.net.brjdevs.natan.gabrielbot.utils.lua;

import net.dv8tion.jda.core.entities.User;

class SafeUser extends SafeISnowflake {
    private final User user;

    SafeUser(User user) {
        super(user);
        this.user = user;
    }

    public String getName() {
        return user.getName();
    }

    public String getDiscriminator() {
        return user.getDiscriminator();
    }

    public String getAvatarUrl() {
        return user.getEffectiveAvatarUrl();
    }

    public boolean isBot() {
        return user.isBot();
    }

    public String getAsMention() {
        return user.getAsMention();
    }
}
