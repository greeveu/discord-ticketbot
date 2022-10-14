package eu.greev.dcbot.ticketsystem.interactions.selections;

import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

public class TicketApplication extends AbstractSelection {
    @Override
    public void execute(Event evt) {
        SelectMenuInteractionEvent event = (SelectMenuInteractionEvent) evt;
        TextInput member = TextInput.create("member", "Name", TextInputStyle.SHORT)
                .setPlaceholder("Your Minecraft name")
                .setMaxLength(12)
                .setRequired(true)
                .build();

        TextInput info = TextInput.create("info", "Information", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Give us more information")
                .setMaxLength(500)
                .setRequired(true)
                .build();

        Modal modal = Modal.create("application", "Give us more information!")
                .addActionRows(ActionRow.of(member), ActionRow.of(info))
                .build();
        event.replyModal(modal).queue();
    }
}
