package eu.greev.dcbot.ticketsystem.categories;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.LinkedHashMap;
import java.util.Map;

public class Creator implements ICategory {
    @Override
    public String getId() {
        return "creator";
    }

    @Override
    public String getLabel() {
        return "Creator Application";
    }

    @Override
    public String getDescription() {
        return "Apply for a creator code";
    }

    @Override
    public Modal getModal() {
        TextInput nameInput = TextInput.create("name", "What is your in-game name?", TextInputStyle.SHORT)
                .setPlaceholder("Your in-game name")
                .setRequired(true)
                .setMaxLength(16)
                .build();

        TextInput linksInput = TextInput.create("links", "Provide links to your content (YouTube, etc.)", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Links to your content")
                .setRequired(true)
                .setMaxLength(200)
                .build();

        TextInput informationInput = TextInput.create("information", "Tell us more about yourself", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Additional information")
                .setRequired(false)
                .setMaxLength(1000)
                .build();

        return Modal.create(getId(), getModalTitle())
                .addActionRow(nameInput)
                .addActionRow(linksInput)
                .addActionRow(informationInput)
                .build();
    }

    @Override
    public Map<String, String> getInfo(ModalInteractionEvent event) {
        Map<String, String> map = new LinkedHashMap<>();

        map.put("In-Game Name", event.getValue("name").getAsString());
        map.put("Content Links", event.getValue("links").getAsString());
        map.put("Additional Information", event.getValue("information") != null && !event.getValue("information").getAsString().isEmpty() ? event.getValue("information").getAsString() : "N/A");

        return map;
    }
}
