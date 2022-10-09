package eu.greev.dcbot.ticketsystem.interactions.selections;

import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

public class TicketQuestion extends AbstractSelection {

    @Override
    public void execute(Event etv) {
        SelectMenuInteractionEvent event = (SelectMenuInteractionEvent) etv;
        TextInput question = TextInput.create("question", "Question", TextInputStyle.PARAGRAPH)
                .setPlaceholder("You have a question? Tell us more!")
                .setMaxLength(500)
                .setRequired(true)
                .build();

        Modal modal = Modal.create("question", "Give us more information!")
                .addActionRows(ActionRow.of(question))
                .build();

        event.replyModal(modal).queue();
    }
}
