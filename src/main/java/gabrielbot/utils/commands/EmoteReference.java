package gabrielbot.utils.commands;

import gabrielbot.GabrielBot;

import java.util.Optional;

@SuppressWarnings("all")
public enum EmoteReference {
    ERROR(":heavy_multiplication_x:", "\u2716"),
    ERROR2(":x:", "\u274C"),
    DICE(":game_die:", "\uD83C\uDFB2"),
    SAD(":frowning:", "\uD83D\uDE26"),
    CORRECT(":white_check_mark:", "\u2705"),
    OK(":ok_hand:", "\uD83D\uDC4C"),
    STOP(":octagonal_sign:", "\uD83D\uDED1"),
    TALKING(":speech_balloon:", "\uD83D\uDCAC"),
    CRYING(":sob:", "\uD83D\uDE2D"),
    WARNING(":warning:", "\u26a0"),
    ZAP(":zap:", "\u26a1"),
    MEGA(":mega:", "\uD83D\uDCE3"),
    CONFUSED(":confused:", "\uD83D\uDE15"),
    WORRIED(":worried:", "\uD83D\uDE1F"),
    THINKING(":thinking:", "\uD83E\uDD14"),
    STOPWATCH(":stopwatch:", "\u23f1"),
    BUY(":inbox_tray:", "\uD83D\uDCE5"),
    SELL(":outbox_tray:", "\uD83D\uDCE4"),
    MARKET(":shopping_car:", "\uD83D\uDED2"),
    MONEY(":money_bag:", "\uD83D\uDCB0"),
    PENCIL(":pencil:", "\uD83D\uDCDD"),
    SMILE(":smile:", "\uD83D\uDE04"),
    PICK(":pick:", "\u26cf"),
    HEART(":heart:", "\u2764"),
    RUNNER(":runner:", "\uD83C\uDFC3"),
    POTION1(":milk:", "\uD83E\uDD5B"),
    POTION2(":champagne:", "\uD83C\uDF7E"),
    CREDITCARD(":credit_card:", "\uD83D\uDCB3"),
    POUCH(":pouch:", "\uD83D\uDC5D"),
    REP(":military_medal:", "\uD83C\uDF96"),
    MAGAZINE(":newspaper:", "\uD83D\uDCF0"),
    AXE(":hammer_pick:", "\u2692"),
    DOLLAR(":dollar:", "\uD83D\uDCB5"),
    WOOD(":bamboo:", "\uD83C\uDF8D"),
    EYES(":eyes:", "\uD83D\uDC40"),
    PENNY(":cd:", "\uD83D\uDCBF"),
    RING(":ring:", "\uD83D\uDC8D"),
    WIND(":wind_blowing_face:", "\uD83C\uDF2C"),
    BOOSTER(":runner:", "\uD83C\uDFC3"),
    JOY(":joy:", "\uD83D\uDE02"),
    CROSSED_SWORD(":crossed_sword:", "\u2694"),
    MAG(":mag_right:", "\uD83D\uDD0E"),
    KEY(":key:", "\uD83D\uDD11"),
    DOG(":dog:", "\uD83D\uDC36"),
    DOOR(":door:", "\uD83D\uDEAA"),
    LOVE_LETTER(":love_letter:", "\uD83D\uDC8C"),
    NECKLACE(":prayer_beads:", "\uD83D\uDCFF"),
    DIAMOND(":gem:", "\uD83D\uDC8E"),
    TUXEDO(":man_in_tuxedo:", "\uD83E\uDD35"),
    DRESS(":dress:", "\uD83D\uDC57"),
    COOKIE(":cookie:", "\uD83C\uDF6A"),
    CHOCOLATE(":chocolate_bar:", "\uD83C\uDF6B"),
    CLOTHES(":shirt:", "\uD83D\uDC55"),
    SHOES(":athletic_shoe:", "\uD83D\uDC5F"),
    ROSE(":rose:", "\uD83C\uDF39"),
    PARTY(":tada:", "\uD83C\uDF89"),
    STAR(":star:", "\u2b50"),
    GEAR(":gear:", "\u2699"),
    HAMMER(":hammer:", "\uD83D\uDD28"),
    HEADPHONE(":headphone:", "\uD83C\uDFA7"),
    POPPER(":tada:", "\uD83C\uDF89"),
    FRAME_PHOTO(":frame_photo:", "\uD83D\uDDBC"),
    BALLOT_BOX(":ballot_box:", "\uD83D\uDDF3"),
    BLACK_JOKER(":black_joker:", "\uD83C\uDCCF"),
    HEARTBEAT(":heartbeat:", "\uD83D\uDC93"),
    PING_PONG(":ping_pong:", "\uD83C\uDFD3"),
    LARS_X_KODE(GabrielBot.DEBUG ? "\u2754" : "<:LarsXKode:318417004795002880>", null),
    KEYBOARD(":keyboard:", "\u2328"),
    WRENCH(":wrench:", "\uD83D\uDD27"),
    VIDEO_GAME(":video_game:", "\uD83C\uDFAE"),
    NEP("<:nep:330775988776599554>", "nep:330775988776599554"),
    AYAYA("<:ayaya:330778938651705344>", "ayaya:330778938651705344"),
    ION("<:ion:330782832605790218>", "ion:330782832605790218"),
    LARS("<:lars:330785709365657600>", "lars:330785709365657600");


    private final String discordNotation;
    private final String unicode;

    EmoteReference(String discordNotation, String unicode) {
        this.discordNotation = discordNotation;
        this.unicode = unicode;
    }

    @Override
    public String toString() {
        return Optional.ofNullable(unicode).orElse(discordNotation) + " ";
    }

    public String getDiscordNotation() {
        return discordNotation;
    }

    public String getUnicode() {
        return unicode;
    }
}