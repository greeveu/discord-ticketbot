package eu.greev.dcbot.ticketsystem.interactions.modals;

import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

@AllArgsConstructor
public class TicketConfirmMessageModal implements Interaction {
    TicketService ticketService;

    @Override
    public void execute(Event evt) {
        ModalInteractionEvent event = (ModalInteractionEvent) evt;
        event.deferEdit().queue();
        ticketService.closeTicket(ticketService.getTicketByChannelId(event.getChannel().getIdLong()), false, event.getMember(), event.getValue("message").getAsString());
    }
}