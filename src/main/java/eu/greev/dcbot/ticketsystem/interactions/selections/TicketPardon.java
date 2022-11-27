package eu.greev.dcbot.ticketsystem.interactions.selections;

import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

public class TicketPardon extends AbstractSelection {
    @Override
    public void execute(Event evt) {
        StringSelectInteractionEvent event = (StringSelectInteractionEvent) evt;
        TextInput member = TextInput.create("member", "Name", TextInputStyle.SHORT)
                .setPlaceholder("Your Minecraft name")
                .setMaxLength(16)
                .setRequired(true)
                .build();
        TextInput info = TextInput.create("info", "Description", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Give us more information about your problem")
                .setMaxLength(500)
                .setRequired(true)
                .build();

        TextInput banId = TextInput.create("ban-id", "The ban id", TextInputStyle.SHORT)
                .setPlaceholder("Give us your ban id, if given")
                .setRequired(false)
                .setMaxLength(15)
                .build();

        Modal modal = Modal.create("pardon", "Give us more information!")
                .addActionRows(ActionRow.of(member), ActionRow.of(info), ActionRow.of(banId))
                .build();
        event.replyModal(modal).queue();
    }
}
