package eu.greev.dcbot.ticketsystem.interactions.selections;

import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

public class TicketBug extends AbstractSelection {

    @Override
    public void execute(Event evt) {
        StringSelectInteractionEvent event = (StringSelectInteractionEvent) evt;
        TextInput bug = TextInput.create("bug", "Bug", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Give us more information about the bug. Note: You don't have to write the bug report in here.")
                .setMaxLength(500)
                .setRequired(true)
                .build();

        Modal modal = Modal.create("bug", "Give us more information!")
                .addComponents(ActionRow.of(bug))
                .build();

        event.replyModal(modal).queue();
    }
}