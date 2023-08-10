package eu.greev.dcbot.ticketsystem.entities;

public record Edit(String edit, long timeEdited, long messageId) implements TranscriptEntity {}