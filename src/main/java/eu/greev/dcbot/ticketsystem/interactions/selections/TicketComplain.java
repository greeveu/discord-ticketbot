package eu.greev.dcbot.ticketsystem.interactions.selections;

import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

public class TicketComplain extends AbstractSelection {
    @Override
    public void execute(Event evt) {
        SelectMenuInteractionEvent event = (SelectMenuInteractionEvent) evt;
        TextInput complain = TextInput.create("complain", "Complain", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Give us more information about your problem")
                .setMinLength(5)
                .build();

        Modal modal = Modal.create("complain", "Give us more information!")
                .addActionRows(ActionRow.of(complain))
                .build();
        event.replyModal(modal).queue();
    }
}
