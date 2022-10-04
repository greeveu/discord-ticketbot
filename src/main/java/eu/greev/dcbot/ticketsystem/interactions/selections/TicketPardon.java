package eu.greev.dcbot.ticketsystem.interactions.selections;

import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

public class TicketPardon extends AbstractSelection {
    @Override
    public void execute(Event evt) {
        SelectMenuInteractionEvent event = (SelectMenuInteractionEvent) evt;
        TextInput member = TextInput.create("member", "Name", TextInputStyle.SHORT)
                .setPlaceholder("Your Minecraft name")
                .setMinLength(2)
                .setMaxLength(12)
                .build();
        TextInput info = TextInput.create("info", "Description", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Give us more information about your problem")
                .setMinLength(5)
                .build();

        Modal modal = Modal.create("pardon", "Give us more information!")
                .addActionRows(ActionRow.of(member), ActionRow.of(info))
                .build();
        event.replyModal(modal).queue();
    }
}
