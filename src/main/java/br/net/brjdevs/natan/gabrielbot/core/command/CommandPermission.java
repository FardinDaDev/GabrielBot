package br.net.brjdevs.natan.gabrielbot.core.command;

import br.net.brjdevs.natan.gabrielbot.core.data.GabrielData;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;

import java.util.Arrays;


public enum CommandPermission {
    USER() {
        @Override
        public boolean test(Member member) {
            return true;
        }
    },
    ADMIN() {
        @Override
        public boolean test(Member member) {
            return member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR) || member.hasPermission(Permission.MANAGE_SERVER) || OWNER.test(member);
        }
    },
    OWNER() {
        @Override
        public boolean test(Member member) {
            return Arrays.stream(GabrielData.config().owners).filter(id->member.getUser().getIdLong() == id).count() > 0;
        }
    };

    public abstract boolean test(Member member);
}
