package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Constants;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.apache.logging.log4j.util.Strings;

import java.awt.*;

@AllArgsConstructor
public class LoadTicket extends AbstractCommand{
    private final Role staff;
    private final EmbedBuilder missingPerm;
    private final TicketService ticketService;

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        if (!event.getMember().getRoles().contains(staff)) {
            event.replyEmbeds(missingPerm.build()).setEphemeral(true).queue();
            return;
        }
        int ticketID = event.getOption("ticket-id").getAsInt();
        Ticket ticket = ticketService.getTicketByTicketId(ticketID);
        if (ticket == null) {
            EmbedBuilder builder = new EmbedBuilder().setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO)
                    .setColor(Color.RED)
                    .setDescription("❌ **Invalid ticket id**");
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            return;
        } else if (ticket.getChannel() != null) {
            EmbedBuilder builder = new EmbedBuilder().setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO)
                    .setColor(Color.RED)
                    .setDescription("❌ **Ticket is still open**");
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            return;
        }

        EmbedBuilder builder = new EmbedBuilder().setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO)
                .setColor(Constants.GREEV_GREEN)
                .setTitle("Ticket #" + ticketID)
                .setAuthor(event.getMember().getEffectiveName(), null, event.getMember().getEffectiveAvatarUrl())
                .addField("Topic", ticket.getTopic(), false)
                .addField("Owner", ticket.getOwner().getAsMention(), false)
                .addField("Supporter", ticket.getSupporter().getAsMention(), false)
                .addField("Closer", ticket.getOwner().getAsMention(), false);

        if (!ticket.getInfo().equals(Strings.EMPTY)) {
            builder.addField("Information", ticket.getInfo(), false);
        }
        if (ticket.getInvolved().isEmpty()) {
            builder.addField("Involved", ticket.getInvolved().toString(), false);
        }

        event.replyEmbeds(builder.build())
                .setActionRow(Button.secondary("transcript", "Get transcript"))
                .setEphemeral(true)
                .queue();
    }
}
