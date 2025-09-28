package eu.greev.dcbot.ticketsystem.interactions.buttons;

import eu.greev.dcbot.ticketsystem.service.TicketService;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

@AllArgsConstructor
public class TicketConfirm extends AbstractButton {
    private final TicketService ticketService;

    @Override
    public void execute(Event evt) {
        ticketService.closeTicket(ticketService.getTicketByChannelId(((ButtonInteractionEvent) evt).getChannel().getIdLong()), false, ((ButtonInteractionEvent) evt).getMember(), null);
    }
}
