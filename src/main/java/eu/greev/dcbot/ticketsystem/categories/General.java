package eu.greev.dcbot.ticketsystem.categories;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.LinkedHashMap;
import java.util.Map;

public class General implements ICategory {
    @Override
    public String getId() {
        return "general";
    }

    @Override
    public String getLabel() {
        return "General";
    }

    @Override
    public String getDescription() {
        return "General questions regarding the client";
    }

    @Override
    public Modal getModal() {
        TextInput reason = TextInput.create("reason", "Reason", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Please describe your issue or question in detail.")
                .setRequired(true)
                .setMaxLength(1000)
                .build();

        return Modal.create(getId(), getModalTitle())
                .addActionRow(reason)
                .build();
    }

    @Override
    public Map<String, String> getInfo(ModalInteractionEvent event) {
        Map<String, String> map = new LinkedHashMap<>();

        map.put("Reason", event.getValue("reason").getAsString());

        return map;
    }
}
