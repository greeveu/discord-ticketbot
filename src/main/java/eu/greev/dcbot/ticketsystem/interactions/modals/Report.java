package eu.greev.dcbot.ticketsystem.interactions.modals;

import eu.greev.dcbot.ticketsystem.service.TicketData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public class Report extends AbstractModal {

    public Report(TicketService ticketService, TicketData ticketData) {
        super(ticketService, ticketData);
    }

    @Override
    String getTicketInfo(ModalInteractionEvent event) {
        String name = event.getValue("member").getAsString();
        String report = event.getValue("hacker").getAsString();
        String reason = event.getValue("reason").getAsString();

        return name + " wants to report " + report + "\nReason\n" + reason;
    }

    @Override
    String getTicketTopic(ModalInteractionEvent event) {
        String name = event.getValue("member").getAsString();
        String report = event.getValue("hacker").getAsString();

        return name + " wants to report " + report;
    }
}
