package eu.greev.dcbot.ticketsystem.interactions.modals;

import eu.greev.dcbot.ticketsystem.service.TicketData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public class Bug extends AbstractModal {

    public Bug(TicketService ticketService, TicketData ticketData) {
        super(ticketService, ticketData);
    }

    @Override
    String getTicketInfo(ModalInteractionEvent event) {
        return event.getValue("bug").getAsString();
    }

    @Override
    String getTicketTopic(ModalInteractionEvent event) {
        return "Bugreport";
    }
}
