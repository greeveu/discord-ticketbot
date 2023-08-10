package eu.greev.dcbot.ticketsystem.service;

import eu.greev.dcbot.ticketsystem.entities.Edit;
import eu.greev.dcbot.ticketsystem.entities.Message;
import eu.greev.dcbot.ticketsystem.entities.Ticket;
import org.jdbi.v3.core.Jdbi;

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
                .bind(0, edit.messageId())
                .bind(1, edit.edit())
                .bind(2, edit.timeEdited())
                .execute());

        jdbi.withHandle(handle -> handle.createUpdate("UPDATE messages SET isEdited = true WHERE messageID = ?")
                .bind(0, edit.messageId())
                .execute());
    }

    public void deleteMessage(long messageId) {
        jdbi.withHandle(handle -> handle.createUpdate("UPDATE messages SET isDeleted=true WHERE messageID=?")
                .bind(0, messageId)
                .execute());
    }

    public void addLogMessage(Message log) {
        jdbi.withHandle(handle -> handle.createUpdate("INSERT INTO logs(log, timeCreated, ticketID) VALUES(?, ?, ?)")
                .bind(0, log.getOriginalContent())
                .bind(1, log.getTimestamp())
                .bind(2, log.getTicketId())
                .execute());
    }

    private List<Message> loadMessages(int ticketId) {
        List<Message> messages = jdbi.withHandle(handle -> handle.createQuery("SELECT * FROM messages WHERE ticketID = ?")
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
                    return message;
                })
                .list());

        messages.addAll(jdbi.withHandle(handle -> handle.createQuery("SELECT * FROM logs WHERE ticketID=?")
                .bind(0, ticketId)
                .map((r, columnNumber, ctx) -> new Message(0, r.getString("log"), "", r.getLong("timeCreated"), ticketId))
                .list()));

        return messages;
    }

    private List<Edit> loadEdits(long messageId) {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT content, timeEdited FROM edits WHERE messageID = ? ORDER BY timeEdited ASC")
                .bind(0, messageId)
                .map((r, columnNumber, ctx) -> new Edit(r.getString("content"), r.getLong("timeEdited"), messageId))
                .list());
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

        jdbi.withHandle(handle -> handle.createUpdate("DELETE FROM logs WHERE ticketID=?")
                .bind(0, ticket.getId())
                .execute());
    }
}