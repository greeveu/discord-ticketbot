package eu.greev.dcbot.ticketsystem.entities;

import lombok.Getter;

public record Edit(@Getter String edit, @Getter long timeEdited, @Getter long messageId) implements TranscriptEntity {}