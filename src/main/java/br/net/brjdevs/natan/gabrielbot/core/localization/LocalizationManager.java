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

    public static class Music {
        public static final String
            SONG_SELECTION = "music_song_select",
            SONG_SELECTION_TIMEOUT = "music_song_select_timeout",
            NO_MATCHES = "music_no_matches",
            UNABLE_TO_LOAD_COMMON = "music_unable_to_load_common",
            UNABLE_TO_LOAD_REPORTED = "music_unable_to_load_reported",
            DIFFERENT_VC = "music_different_vc",
            NOT_CONNECTED = "music_not_connected",
            NOT_PLAYING = "music_not_playing",
            SKIPPING = "music_skipping",
            VOTE_REMOVED = "music_vote_removed",
            VOTE_ADDED = "music_vote_added",
            DJ_SKIP = "music_dj_skip",
            DJ_STOP = "music_dj_stop",
            NOT_DJ = "music_not_dj",
            TRACK_STUCK = "music_track_stuck",
            UNABLE_TO_PLAY_COMMON = "music_unable_to_play_common",
            UNABLE_TO_PLAY_REPORTED = "music_unable_to_play_reported",
            NOW_PLAYING = "music_now_playing",
            FINISHED_PLAYING = "music_finished_playing",
            NP = "music_np";

        private Music(){}
    }

    public static class Fun {
        public static final String
            BRAINFUCK_CYCLE_LIMIT = "brainfuck_cycle_limit",
            BRAINFUCK_DATA_POINTER_OUT_OF_BOUNDS = "brainfuck_dp_oob",
            BRAINFUCK_INPUT_OUT_OF_BOUNDS = "brainfuck_input_oob",
            BRAINFUCK_NO_RETURNS = "brainfuck_no_returns";

        private Fun(){}
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
