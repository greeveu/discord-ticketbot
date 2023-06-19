package eu.greev.dcbot.ticketsystem.service;

import eu.greev.dcbot.ticketsystem.entities.Edit;
import eu.greev.dcbot.ticketsystem.entities.Message;
import eu.greev.dcbot.ticketsystem.entities.Ticket;
import org.jdbi.v3.core.Jdbi;

import java.util.ArrayList;
import java.util.List;

public class TranscriptData {
    private final Jdbi jdbi;

    protected TranscriptData(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public void loadTranscript(Ticket ticket) {
        ticket.setTranscript(new Transcript(loadMessages(ticket.getId())));
    }

    public void addNewMessage(Message message) {
        jdbi.withHandle(handle -> handle.createUpdate("INSERT INTO messages(messageID, content, author, timeCreated) VALUES(?, ?, ?, ?)")
                .bind(0, message.getId())
                .bind(1, message.getOriginalContent())
                .bind(2, message.getAuthor())
                .bind(3, message.getTimestamp())
                .execute());
    }

    public void addEditToMessage(Edit edit, long messageId) {
        jdbi.withHandle(handle -> handle.createUpdate("INSERT INTO edits(messageID, content, timeEdited) VALUES(?, ?, ?)")
                .bind(0, messageId)
                .bind(1, edit.getEdit())
                .bind(2, edit.getTimeEdited())
                .execute());
    }

    public void deleteMessage(long messageId) {
        jdbi.withHandle(handle -> handle.createUpdate("UPDATE messages SET isDeleted=true WHERE messageID=?")
                .bind(0, messageId)
                .execute());
    }

    private List<Message> loadMessages(int ticketId) {
        List<Message> messages = new ArrayList<>();

        jdbi.withHandle(handle -> handle.createQuery("SELECT * FROM messages WHERE ticketID = ?")
                .bind(0, ticketId)
                .map((r, columnNumber, ctx) -> {
                    Message message = new Message(
                            r.getLong("messageID"),
                            r.getString("content"),
                            r.getString("author"),
                            r.getLong("timeCreated"));
                    boolean isDeleted = r.getBoolean("isDeleted");
                    boolean isEdited = r.getBoolean("isEdited");

                    message.setDeleted(isDeleted);

                    if (isEdited) {
                        message.setEdits(loadEdits(message.getId()));
                    }
                    return null;
                })
                .findFirst());
        return messages;
    }

    private List<Edit> loadEdits(long messageId) {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT content, timeEdited FROM edits WHERE messageID = ?")
                .bind(0, messageId)
                .mapTo(Edit.class)
                .list());
    }

    public void deleteTranscript(Ticket ticket) {
        List<Message> messages = ticket.getTranscript().getMessages();
        List<Message> messagesWithEdits = messages.stream().filter(m -> !m.getEdits().isEmpty()).toList();

        if (!messagesWithEdits.isEmpty()) {
            for (Message message : messagesWithEdits) {
                jdbi.withHandle(handle -> handle.createUpdate("DELETE FROM edits WHERE messageID=?")
                        .bind(0, message.getId())
                        .execute());
            }
        }

        jdbi.withHandle(handle -> handle.createUpdate("DELETE FROM messages WHERE ticketID=?")
                .bind(0, ticket.getId())
                .execute());
    }
}