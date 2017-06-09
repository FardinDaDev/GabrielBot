package br.net.brjdevs.natan.gabrielbot.utils.lua;

import net.dv8tion.jda.core.entities.ISnowflake;

import java.time.OffsetDateTime;

class SafeISnowflake {
    private final ISnowflake snowflake;

    SafeISnowflake(ISnowflake snowflake) {
        this.snowflake = snowflake;
    }

    public String getId() {
        return snowflake.getId();
    }

    public long getIdLong() {
        return snowflake.getIdLong();
    }

    public OffsetDateTime getCreationTime() {
        return snowflake.getCreationTime();
    }
}
