package eu.greev.dcbot.ticketsystem.interactions.selections;

import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

public class TicketBug extends AbstractSelection {

    @Override
    public void execute(Event etv) {
        SelectMenuInteractionEvent event = (SelectMenuInteractionEvent) etv;
        TextInput bug = TextInput.create("bug", "Bug", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Give us more information about the bug")
                .setMaxLength(500)
                .setRequired(true)
                .build();

        Modal modal = Modal.create("bug", "Give us more information!")
                .addActionRows(ActionRow.of(bug))
                .build();

        event.replyModal(modal).queue();
    }
}
