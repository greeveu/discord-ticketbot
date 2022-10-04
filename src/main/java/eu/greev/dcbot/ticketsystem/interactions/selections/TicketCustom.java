package eu.greev.dcbot.ticketsystem.interactions.selections;

import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

public class TicketCustom extends AbstractSelection {
    @Override
    public void execute(Event evt) {
        SelectMenuInteractionEvent event = (SelectMenuInteractionEvent) evt;
        TextInput custom = TextInput.create("custom", "Custom", TextInputStyle.SHORT)
                .setPlaceholder("Tell us which topic your ticket should have")
                .setMinLength(5)
                .build();

        Modal modal = Modal.create("custom", "Your custom ticket topic!")
                .addActionRows(ActionRow.of(custom))
                .build();
        event.replyModal(modal).queue();
    }
}
