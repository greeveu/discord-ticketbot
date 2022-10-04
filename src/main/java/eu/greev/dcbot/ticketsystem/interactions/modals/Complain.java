package eu.greev.dcbot.ticketsystem.interactions.modals;

import eu.greev.dcbot.ticketsystem.service.TicketData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public class Complain extends AbstractModal {

    public Complain(TicketService ticketService, TicketData ticketData) {
        super(ticketService, ticketData);
    }

    @Override
    String getTicketInfo(ModalInteractionEvent event) {
        return event.getValue("complain").getAsString();
    }

    @Override
    String getTicketTopic(ModalInteractionEvent event) {
        return "Complain";
    }
}
