package br.net.brjdevs.natan.gabrielbot.core.localization;

import net.dv8tion.jda.core.entities.Guild;

public class LocalizationManager {
    public static class CustomCommands {
        public static final String
            ADDED_SUCCESSFULLY = "cc_add_success",
            COMMAND_EXISTS = "cc_cmd_exists",
            NAME_CONFLICT_DEFAULT = "cc_cmd_conflicts_default",
            NAME_CONFLICT = "cc_cmd_conflicts",
            NO_COMMANDS = "cc_no_cmds",
            REMOVED_SUCCESSFULLY = "cc_remove_success",
            COMMAND_NOT_FOUND = "cc_cmd_notfound",
            RENAME_SUCCESS = "cc_rename_success";

        private CustomCommands(){}
    }

    public static final String
        IMAGE_NOT_FOUND = "imgnotfound",
        NOT_NSFW = "notnsfw",
        COMMAND_NOT_FOUND = "cmdnotfound",
        HELP_LIST_DESCRIPTION = "helpdesc",
        PRUNE_SUCCESS = "prunedone",
        MISSING_PERMISSION = "missingperm";

    private LocalizationManager(){}

    public static String getString(Guild guild, String code, String defValue) {
        return defValue; //TODO implement
    }
}
