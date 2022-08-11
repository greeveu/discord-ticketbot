package eu.greev.dcbot.ticketsystem;

import eu.greev.dcbot.utils.data.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class TicketListener extends ListenerAdapter {
    private final long serverID = new Data().testID;
    private final JDA jda;

    public TicketListener(JDA jda) {
        this.jda = jda;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("ticket")) {
            if (event.getSubcommandName() != null && event.getSubcommandName().equals("help")) {
                EmbedBuilder builder = new EmbedBuilder().setTitle("Commands").setColor(new Color(0, 153, 153, 255));
                builder.setFooter("Greev.eu", "https://cdn.discordapp.com/emojis/355825850152779786.webp?size=96&quality=lossless");
                builder.setDescription("The commands you have access to are listed below");
                builder.addField("", "**/ticket add <Member> ·** Add a member to a ticket channel", false);
                builder.addField("", "**/ticket remove <Member> ·** Remove a member from a ticket channel", false);
                builder.addField("", "**/ticket claim ·** Claim the ticket", false);
                builder.addField("", "**/ticket close ·** Close the ticket", false);
                builder.addField("", "**/ticket supporter <Staff> ·** Set the new supporter", false);
                builder.addField("", "**/ticket topic ·** Set the topic of the ticket", false);
                builder.addField("", "Please contact a member of staff if you require assistance.", false);

                event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            }
        }
    }
}