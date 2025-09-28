package eu.greev.dcbot.ticketsystem.categories;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.Map;

public interface ICategory {
    String getId();
    String getLabel();
    String getDescription();
    Modal getModal();
    Map<String, String> getInfo(ModalInteractionEvent event);
    default String getModalTitle() {
        return "Ticket: " + getLabel();
    }
}
