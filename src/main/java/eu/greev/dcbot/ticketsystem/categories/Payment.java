package eu.greev.dcbot.ticketsystem.categories;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.LinkedHashMap;
import java.util.Map;

public class Payment implements ICategory {
    @Override
    public String getId() {
        return "payment";
    }

    @Override
    public String getLabel() {
        return "Payment";
    }

    @Override
    public String getDescription() {
        return "Payment failed or items not received";
    }

    @Override
    public Modal getModal() {
        TextInput nameInput = TextInput.create("name", "In-Game Name", TextInputStyle.SHORT)
                .setPlaceholder("Your in-game name")
                .setRequired(true)
                .setMaxLength(16)
                .build();

        TextInput problemInput = TextInput.create("problem", "Problem", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Describe your payment issue in detail.")
                .setRequired(true)
                .setMaxLength(1000)
                .build();

        return Modal.create(getId(), getModalTitle())
                .addActionRow(nameInput)
                .addActionRow(problemInput)
                .build();
    }

    @Override
    public Map<String, String> getInfo(ModalInteractionEvent event) {
        Map<String, String> map = new LinkedHashMap<>();

        map.put("In-Game Name", event.getValue("name").getAsString());
        map.put("Problem", event.getValue("problem").getAsString());

        return map;
    }
}
