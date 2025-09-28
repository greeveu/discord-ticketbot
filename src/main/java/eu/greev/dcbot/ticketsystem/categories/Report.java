package eu.greev.dcbot.ticketsystem.categories;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.LinkedHashMap;
import java.util.Map;

public class Report implements ICategory {
    @Override
    public String getId() {
        return "report";
    }

    @Override
    public String getLabel() {
        return "Report";
    }

    @Override
    public String getDescription() {
        return "Here you can report a user";
    }

    @Override
    public Modal getModal() {
        TextInput user = TextInput.create("user", "User", TextInputStyle.SHORT)
                .setPlaceholder("Who do you want to report?")
                .setRequired(true)
                .setMaxLength(16)
                .build();

        TextInput reason = TextInput.create("reason", "Reason", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Why do you want to report this user?")
                .setRequired(true)
                .setMaxLength(1000)
                .build();

        TextInput evidence = TextInput.create("evidence", "Evidence", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Provide any evidence (links, screenshots, etc.) to support your report.")
                .setRequired(true)
                .setMaxLength(1000)
                .build();

        return Modal.create(getId(), getModalTitle())
                .addActionRow(user)
                .addActionRow(reason)
                .addActionRow(evidence)
                .build();
    }

    @Override
    public Map<String, String> getInfo(ModalInteractionEvent event) {
        Map<String, String> map = new LinkedHashMap<>();

        map.put("User", event.getValue("user").getAsString());
        map.put("Reason", event.getValue("reason").getAsString());
        map.put("Evidence", event.getValue("evidence").getAsString());

        return map;
    }
}
