package br.net.brjdevs.natan.gabrielbot.core.listeners.operations;

import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;

@FunctionalInterface
public interface ReactionOperation {
    boolean add(MessageReactionAddEvent event);

    default boolean remove(MessageReactionRemoveEvent event) {
        return false;
    }

    default boolean removeAll(MessageReactionRemoveAllEvent event) {
        return false;
    }

    default void onCancel() {

    }

    default void onExpire() {

    }
}
