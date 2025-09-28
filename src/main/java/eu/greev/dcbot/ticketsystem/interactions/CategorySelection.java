package eu.greev.dcbot.ticketsystem.interactions;

import eu.greev.dcbot.ticketsystem.categories.ICategory;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

@AllArgsConstructor
public class CategorySelection implements Interaction {
    private ICategory category;

    @Override
    public void execute(Event evt) {
        StringSelectInteractionEvent event = (StringSelectInteractionEvent) evt;
        event.replyModal(category.getModal()).queue();
    }
}