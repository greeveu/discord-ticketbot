package eu.greev.dcbot.ticketsystem.categories;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.LinkedHashMap;
import java.util.Map;

public class Bug implements  ICategory {
    @Override
    public String getId() {
        return "bug";
    }

    @Override
    public String getLabel() {
        return "Bug";
    }

    @Override
    public String getDescription() {
        return "Errors/Bugs in the launcher, app or game";
    }

    @Override
    public Modal getModal() {
        TextInput problemInput = TextInput.create("problem", "Problem", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Please provide as much information about your problem as possible.")
                .setMaxLength(1000)
                .setRequired(true)
                .build();

        TextInput versionInput = TextInput.create("version", "Version", TextInputStyle.SHORT)
                .setPlaceholder("Please provide the version of the launcher/app/game.")
                .setMaxLength(20)
                .setRequired(false)
                .build();

        TextInput logsInput = TextInput.create("logs", "Logs", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Provide logs/errors. Use mclo.gs link or upload file after ticket creation if too long.")
                .setMaxLength(1000)
                .setRequired(false)
                .build();

        return Modal.create(getId(), getModalTitle())
                .addActionRow(problemInput)
                .addActionRow(versionInput)
                .addActionRow(logsInput)
                .build();
    }

    @Override
    public Map<String, String> getInfo(ModalInteractionEvent event) {
        Map<String, String> map = new LinkedHashMap<>();

        map.put("Problem", event.getValue("problem").getAsString());
        map.put("Version", event.getValue("version") != null && !event.getValue("version").getAsString().isEmpty() ? event.getValue("version").getAsString() : "N/A");
        map.put("Logs", event.getValue("logs") != null && !event.getValue("logs").getAsString().isEmpty() ? event.getValue("logs").getAsString() : "N/A");

        return map;
    }
}
