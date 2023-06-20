package eu.greev.dcbot.ticketsystem.entities;

import lombok.Getter;

@Getter
public record Edit(String edit, long timeEdited, long messageId) implements TranscriptEntity {}