package eu.greev.dcbot.ticketsystem.interactions.modals;

import eu.greev.dcbot.ticketsystem.service.TicketData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.apache.logging.log4j.util.Strings;

public class Custom extends AbstractModal {

    public Custom(TicketService ticketService, TicketData ticketData, Config config) {
        super(config, ticketService, ticketData);
    }

    @Override
    String getTicketInfo(ModalInteractionEvent event) {
        return Strings.EMPTY;
    }

    @Override
    String getTicketTopic(ModalInteractionEvent event) {
        return event.getValue("custom").getAsString();
    }
}
