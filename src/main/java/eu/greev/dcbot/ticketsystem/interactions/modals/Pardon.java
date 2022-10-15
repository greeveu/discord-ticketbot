package eu.greev.dcbot.ticketsystem.interactions.modals;

import eu.greev.dcbot.ticketsystem.service.TicketData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.simpleyaml.configuration.file.YamlFile;

public class Pardon extends AbstractModal {

    public Pardon(TicketService ticketService, TicketData ticketData, YamlFile config) {
        super(config, ticketService, ticketData);
    }

    @Override
    String getTicketInfo(ModalInteractionEvent event) {
        return  event.getValue("info").getAsString();
    }

    @Override
    String getTicketTopic(ModalInteractionEvent event) {
        String banId = event.getValue("ban-id").getAsString();
        return event.getValue("member").getAsString() + " wants pardon " + (banId.contains("#") ?  banId : "#" + banId);
    }
}
