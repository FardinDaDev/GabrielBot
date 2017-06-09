package br.net.brjdevs.natan.gabrielbot.utils.lua;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;

import java.util.List;

class SafeRole extends SafeISnowflake {
    private final Role role;

    SafeRole(Role role) {
        super(role);
        this.role = role;
    }

    public String getName() {
        return role.getName();
    }

    public List<Permission> getPermissions() {
        return role.getPermissions();
    }

    public long getPermissionsRaw() {
        return role.getPermissionsRaw();
    }

    public int getPosition() {
        return role.getPosition();
    }

    public int getPositionRaw() {
        return role.getPositionRaw();
    }

    public boolean isManaged() {
        return role.isManaged();
    }

    public boolean isPublicRole() {
        return role.isPublicRole();
    }

    public boolean isMentionable() {
        return role.isMentionable();
    }

    public boolean isSeparate() {
        return role.isHoisted();
    }
}
