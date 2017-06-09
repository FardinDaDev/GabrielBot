package br.net.brjdevs.natan.gabrielbot.utils.lua;

import net.dv8tion.jda.core.Region;
import net.dv8tion.jda.core.entities.Guild;

import java.util.List;
import java.util.stream.Collectors;

class SafeGuild extends SafeISnowflake {
    private final Guild guild;
    private final SafeChannel channel;

    SafeGuild(Guild guild, SafeChannel channel) {
        super(guild);
        this.guild = guild;
        this.channel = channel;
    }

    public String getName() {
        return guild.getName();
    }

    public Guild.ExplicitContentLevel getExplicitContentLevel() {
        return guild.getExplicitContentLevel();
    }

    public Region getRegion() {
        return guild.getRegion();
    }

    public List<SafeChannel> getTextChannels() {
        return guild.getTextChannels().stream().map(c->c.getIdLong() == channel.getIdLong() ? channel : new SafeChannel(c, 0)).collect(Collectors.toList());
    }

    public List<SafeRole> getRoles() {
        return guild.getRoles().stream().map(SafeRole::new).collect(Collectors.toList());
    }

    public List<SafeMember> getMembers() {
        return guild.getMembers().stream().map(SafeMember::new).collect(Collectors.toList());
    }

    public SafeMember getOwner() {
        return new SafeMember(guild.getOwner());
    }

    public List<SafeMember> getMembersByName(String name, boolean ignoreCase) {
        return guild.getMembersByName(name, ignoreCase).stream().map(SafeMember::new).collect(Collectors.toList());
    }

    public List<SafeMember> getMembersByNickname(String name, boolean ignoreCase) {
        return guild.getMembersByNickname(name, ignoreCase).stream().map(SafeMember::new).collect(Collectors.toList());
    }

    public List<SafeMember> getMembersByEffectiveName(String name, boolean ignoreCase) {
        return guild.getMembersByEffectiveName(name, ignoreCase).stream().map(SafeMember::new).collect(Collectors.toList());
    }

    public String getIconUrl() {
        return guild.getIconUrl();
    }
}
