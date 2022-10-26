package eu.greev.dcbot.ticketsystem.interactions.selections;

import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

public class TicketCustom extends AbstractSelection {
    @Override
    public void execute(Event evt) {
        StringSelectInteractionEvent event = (StringSelectInteractionEvent) evt;
        TextInput custom = TextInput.create("custom", "Custom", TextInputStyle.SHORT)
                .setPlaceholder("Tell us which topic your ticket should have")
                .setMaxLength(100)
                .setRequired(true)
                .build();

        Modal modal = Modal.create("custom", "Your custom ticket topic!")
                .addActionRows(ActionRow.of(custom))
                .build();
        event.replyModal(modal).queue();
    }
}
