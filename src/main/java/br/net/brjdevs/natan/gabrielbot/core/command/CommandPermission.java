package br.net.brjdevs.natan.gabrielbot.core.command;

import br.net.brjdevs.natan.gabrielbot.core.data.GabrielData;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;

import java.util.Arrays;


public enum CommandPermission {
    USER() {
        @Override
        public boolean test(Member member) {
            return true;
        }
    },
    PREMIUM() {
        @Override
        public boolean test(Member member) {
            long now = System.currentTimeMillis();
            GabrielData.UserData user = GabrielData.users().get().get(member.getUser().getId());
            GabrielData.GuildData guild = GabrielData.guilds().get().get(member.getGuild().getId());
            return
                    (user != null && user.premiumUntil > now) ||
                    (guild != null && guild.premiumUntil > now) ||
                    OWNER.test(member);
        }
    },
    LARS() {
        @Override
        public boolean test(Member member) {
            return member.getUser().getIdLong() == 132584525296435200L || OWNER.test(member);
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
