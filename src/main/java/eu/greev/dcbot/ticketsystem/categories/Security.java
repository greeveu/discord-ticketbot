package eu.greev.dcbot.ticketsystem.categories;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.LinkedHashMap;
import java.util.Map;

public class Security implements ICategory {
    @Override
    public String getId() {
        return "security";
    }

    @Override
    public String getLabel() {
        return "Security Report";
    }

    @Override
    public String getDescription() {
        return "Here you can report security vulnerabilities";
    }

    @Override
    public Modal getModal() {
        TextInput vulnerability = TextInput.create("vulnerability", "Vulnerability", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Describe the security vulnerability you have discovered.")
                .setRequired(true)
                .setMaxLength(2000)
                .build();

        Modal modal = Modal.create(getId(), getModalTitle())
                .addActionRow(vulnerability)
                .build();

        return modal;
    }

    @Override
    public Map<String, String> getInfo(ModalInteractionEvent event) {
        Map<String, String> map = new LinkedHashMap<>();

        map.put("Vulnerability", event.getValue("vulnerability").getAsString());

        return map;
    }
}
