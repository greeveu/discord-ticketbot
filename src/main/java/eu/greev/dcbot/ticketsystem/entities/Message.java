package eu.greev.dcbot.ticketsystem.entities;

import lombok.Data;

import java.util.List;

@Data
public class Message {
    private final long id;
    private final String originalContent;
    private final long timestamp;
    private List<String> editedContent;
    private boolean isDeleted;
}