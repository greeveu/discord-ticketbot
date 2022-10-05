package eu.greev.dcbot.ticketsystem.interactions.buttons;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Constants;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.awt.*;

@AllArgsConstructor
public class TicketNevermind extends AbstractButton {
    private final TicketService ticketService;

    @Override
    public void execute(Event evt) {
        ButtonInteractionEvent event = (ButtonInteractionEvent) evt;
        Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
        if (ticket.getOwner().equals(event.getUser())) {
            ticketService.closeTicket(ticket, true);
        }else {
            EmbedBuilder builder = new EmbedBuilder().setColor(Color.RED)
                    .addField("❌ **Missing access**", "You can not click this button", false)
                    .setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO);
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
        }
    }
}
