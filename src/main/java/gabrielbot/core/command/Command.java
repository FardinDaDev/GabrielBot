package gabrielbot.core.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
    String name();
    String usage();
    String description();
    CommandCategory category();
    CommandPermission permission();

    String[] nameArgs()
            default {};

    boolean advancedSplit()
            default true;
    boolean isHiddenFromHelp()
            default false;
}
