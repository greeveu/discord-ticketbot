package eu.greev.dcbot.ticketsystem.interactions.selections;

import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

public class TicketApplication extends AbstractSelection {
    @Override
    public void execute(Event evt) {
        StringSelectInteractionEvent event = (StringSelectInteractionEvent) evt;
        TextInput member = TextInput.create("member", "Name", TextInputStyle.SHORT)
                .setPlaceholder("Your Minecraft name")
                .setMaxLength(16)
                .setRequired(true)
                .build();

        TextInput info = TextInput.create("info", "Information", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Give us more information. Note: You don't have to write your application in here.")
                .setMaxLength(500)
                .setRequired(true)
                .build();

        Modal modal = Modal.create("application", "Give us more information!")
                .addComponents(ActionRow.of(member), ActionRow.of(info))
                .build();
        event.replyModal(modal).queue();
    }
}