package eu.greev.dcbot.ticketsystem.interactions.selections;

import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

public class TicketReport extends AbstractSelection {
    @Override
    public void execute(Event evt) {
        SelectMenuInteractionEvent event = (SelectMenuInteractionEvent) evt;

        TextInput member = TextInput.create("member", "Name", TextInputStyle.SHORT)
                .setPlaceholder("Your Minecraft name")
                .setMaxLength(16)
                .setRequired(true)
                .build();
        TextInput hacker = TextInput.create("hacker", "Name", TextInputStyle.SHORT)
                .setPlaceholder("Who do you wanna report?")
                .setMaxLength(16)
                .setRequired(true)
                .build();
        TextInput reason = TextInput.create("reason", "Reason", TextInputStyle.PARAGRAPH)
                .setPlaceholder("The reason for reporting")
                .setMaxLength(500)
                .setRequired(true)
                .build();
        Modal modal = Modal.create("report", "Give us more information!")
                .addActionRows(ActionRow.of(member), ActionRow.of(hacker), ActionRow.of(reason))
                .build();
        event.replyModal(modal).queue();
    }
}
