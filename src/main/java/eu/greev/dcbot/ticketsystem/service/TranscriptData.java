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

    public Transcript loadTranscript(int ticketId) {
        return new Transcript(loadMessages(ticketId));
    }

    public void addNewMessage(Message message) {
        jdbi.withHandle(handle -> handle.createUpdate("INSERT INTO messages(messageID, content, author, timeCreated, ticketID) VALUES(?, ?, ?, ?, ?) ON CONFLICT(messageId) DO UPDATE SET isEdited=true")
                .bind(0, message.getId())
                .bind(1, message.getOriginalContent())
                .bind(2, message.getAuthor())
                .bind(3, message.getTimestamp())
                .bind(4, message.getTicketId())
                .execute());
    }

    public void addEditToMessage(Edit edit) {
        jdbi.withHandle(handle -> handle.createUpdate("INSERT INTO edits(messageID, content, timeEdited) VALUES(?, ?, ?)")
                .bind(0, edit.getMessageId())
                .bind(1, edit.getEdit())
                .bind(2, edit.getTimeEdited())
                .execute());

        jdbi.withHandle(handle -> handle.createUpdate("UPDATE messages SET isEdited = true WHERE messageID = ?")
                .bind(0, edit.getMessageId())
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
                            r.getLong("timeCreated"),
                            ticketId);
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
        List<Edit> edits = jdbi.withHandle(handle -> handle.createQuery("SELECT content, timeEdited FROM edits WHERE messageID = ?")
                .bind(0, messageId)
                .map((r, columnNumber, ctx) -> new Edit(r.getString("content"), r.getLong("timeEdited"), messageId))
                .list());
        edits.sort((edit1, edit2) -> (int) (edit1.getTimeEdited() - edit2.getTimeEdited()));
        return edits;
    }

    public void deleteTranscript(Ticket ticket) {
        Transcript transcript = ticket.getTranscript();
        transcript.getRecentChanges().clear();
        List<Message> messages = transcript.getMessages();
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