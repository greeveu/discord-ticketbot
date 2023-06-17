package eu.greev.dcbot.ticketsystem.entities;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Message {
    private final long id;
    private final String originalContent;
    private final String author;
    private final long timestamp;
    private List<Edit> edits = new ArrayList<>();
    private boolean isDeleted;
}