package eu.greev.dcbot.ticketsystem.service;

import eu.greev.dcbot.ticketsystem.entities.Edit;
import eu.greev.dcbot.ticketsystem.entities.Message;
import eu.greev.dcbot.ticketsystem.entities.TranscriptEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Getter
@RequiredArgsConstructor
public class Transcript {
    private final List<TranscriptEntity> recentChanges = new ArrayList<>();
    private final List<Message> messages;

    public void addMessage(net.dv8tion.jda.api.entities.Message message, int ticketId) {
        Message msg = new Message(message.getIdLong(), message.getContentDisplay(), message.getAuthor().getName(), message.getTimeCreated().toEpochSecond(), ticketId);
        messages.add(msg);
        recentChanges.add(msg);
    }

    public void addLogMessage(String log, long timestamp, int ticketId) {
        Message message = new Message(0, log, "", timestamp, ticketId);
        messages.add(message);
        recentChanges.add(message);
    }

    public void editMessage(long messageId, String content, long timeEdited) {
        Edit edit = new Edit(content, timeEdited, messageId);

        messages.stream()
                .filter(m -> m.getId() == messageId)
                .findFirst().ifPresent(m -> m.getEdits().add(edit));
        recentChanges.add(edit);
    }

    public void deleteMessage(long messageId) {
        messages.stream()
                .filter(m -> m.getId() == messageId)
                .findFirst().ifPresent(m -> m.setDeleted(true));
        recentChanges.stream()
                .filter(Message.class::isInstance)
                .filter(m -> (((Message) m).getId()) == messageId)
                .findFirst().ifPresentOrElse(m -> ((Message) m).setDeleted(true), () -> {
                    Message message = new Message(messageId, "", "", 0, 0);
                    message.setDeleted(true);
                    recentChanges.add(message);
                });
    }

    public File toFile(int ticketId) {
        new File("./Tickets/transcripts").mkdirs();
        File transcript = new File("./Tickets/transcripts/" + ticketId + ".txt");
        try {
            if (!transcript.createNewFile()) {
                return transcript;
            }
        } catch (IOException e) {
            log.error("Could not create transcript file", e);
        }
        messages.add(0, new Message(0, "Transcript of ticket #" + ticketId, "", Instant.now().getEpochSecond(), ticketId));

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(transcript, true))) {
            for (Message message : messages) {
                StringBuilder builder = new StringBuilder(formatTimestamp(message.getTimestamp()));

                if (message.getId() == 0 && message.getAuthor().equals(Strings.EMPTY)) {
                    writer.write(builder.append(message.getOriginalContent()).toString());
                    writer.newLine();
                    continue;
                }

                builder.append("[").append(message.getAuthor()).append("] ").append(message.getOriginalContent());
                List<Edit> edits = message.getEdits();
                if (!edits.isEmpty()) {
                    builder.append(" | Edits:");

                    for (int i = 0; i < edits.size(); i++) {
                        builder.append(" ").append(edits.get(i).edit());

                        if (edits.size() - 1 != i) {
                            builder.append(" ->");
                        }
                    }
                }

                if (message.isDeleted() && message.getId() != 0) {
                    builder.append("~~").append(message.getOriginalContent()).append("~~");
                }
                writer.write(builder.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            log.error("Could not clean transcript of ticket #{}", ticketId, e);
        }
        return transcript;
    }

    private static String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ ").format(Date.from(Instant.ofEpochSecond(timestamp)));
    }
}