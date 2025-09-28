package eu.greev.dcbot.ticketsystem.interactions.buttons;

import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

public class TicketConfirmMessage extends AbstractButton {
    @Override
    public void execute(Event evt) {
        ButtonInteractionEvent event = (ButtonInteractionEvent) evt;

        TextInput message = TextInput.create("message", "Message", TextInputStyle.SHORT)
                .setPlaceholder("What closing message do you want to leave?")
                .setRequired(true)
                .setMaxLength(100)
                .build();

        Modal modal = Modal.create("ticket-confirm-message-modal", "Closing Message")
                .addActionRow(message)
                .build();

        event.replyModal(modal).queue();
    }
}
