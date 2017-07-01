package gabrielbot.core.listeners;

import br.com.brjdevs.highhacks.eventbus.Listener;
import gabrielbot.utils.commands.EmoteReference;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class ReactListener implements EventListener {
    @Override
    @Listener
    public void onEvent(Event event) {
        if(event instanceof GuildMessageReceivedEvent) {
            GuildMessageReceivedEvent gmre = (GuildMessageReceivedEvent)event;
            if(!gmre.getGuild().getSelfMember().hasPermission(gmre.getChannel(), Permission.MESSAGE_ADD_REACTION)) return;
            long id = gmre.getAuthor().getIdLong();
            Message message = gmre.getMessage();
            String contentLower = gmre.getMessage().getContent().toLowerCase();
            String rawContent = gmre.getMessage().getRawContent();

            if(nep(id) && (contentLower.equals("nep") || contentLower.equals("nepnep"))) {
                message.addReaction(EmoteReference.NEP.getUnicode()).queue();
            }

            if(ayaya(id) && contentLower.equals("ayaya")) {
                message.addReaction(EmoteReference.AYAYA.getUnicode()).queue();
            }

            if(all(id)) {
                if(contentLower.equals("ion") || rawContent.equals("<@251260900252712962>") || rawContent.equals("<@!251260900252712962>")) {
                    message.addReaction(EmoteReference.ION.getUnicode()).queue();
                }
                if(contentLower.equals("cute") || rawContent.equals("<@132584525296435200>") || rawContent.equals("<@!132584525296435200>")) {
                    message.addReaction(EmoteReference.LARS.getUnicode()).queue();
                }
            }
        }
    }

    private static boolean nep(long id) {
        return all(id) || id == 200100128009355264L; //NepNep#7478
    }

    private static boolean ayaya(long id) {
        return all(id) || id == 214393232342122506L; //Aya Komichi#7541
    }

    private static boolean all(long id) {
        return id == 182245310024777728L || //Me
                id == 251260900252712962L || //Jakuri#8127
                id == 155867458203287552L || //Kodehawa#3457
                id == 132584525296435200L || //MrLar#8117
                id == 232542027550556160L || //Unavoidable Baka#0670
                id == 267207628965281792L; //Desiree#3658
    }
}
