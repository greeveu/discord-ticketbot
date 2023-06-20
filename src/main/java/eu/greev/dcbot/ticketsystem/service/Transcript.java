package eu.greev.dcbot.ticketsystem.service;

import eu.greev.dcbot.ticketsystem.entities.Edit;
import eu.greev.dcbot.ticketsystem.entities.Message;
import eu.greev.dcbot.ticketsystem.entities.TranscriptEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

    public void addMessage(net.dv8tion.jda.api.entities.Message message) {
        Message msg = new Message(message.getIdLong(), message.getContentDisplay(), message.getAuthor().getAsMention(), message.getTimeCreated().toEpochSecond());
        messages.add(msg);
        recentChanges.add(msg);
    }

    public void addLogMessage(String log, long timestamp) {
        Message message = new Message(0, log, "", timestamp);
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
                .filter(m -> (((Message) m).getId()) == messageId)
                .findFirst().ifPresent(m -> ((Message) m).setDeleted(true));
    }

    public File toFile(int id) {
        new File("./Tickets/transcripts").mkdirs();
        File transcript = new File("./Tickets/transcripts/" + id + ".txt");
        try {
            if (!transcript.createNewFile()) {
                throw new IllegalStateException("Transcript should already exist");
            }
            addLogMessage("Transcript of ticket #" + id, Instant.now().getEpochSecond());
        } catch (IOException e) {
            log.error("Could not create transcript", e);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(transcript, true))) {
            for (Message message : messages) {
                if (message.getId() == 0) {
                    writer.write(message.getOriginalContent());
                    writer.newLine();
                }

                String log = formatTimestamp(message.getTimestamp()) + "[" + message.getAuthor() + "] ";
                List<Edit> edits = message.getEdits();
                if (!edits.isEmpty()) {
                    StringBuilder builder = new StringBuilder(log).append(message.getOriginalContent()).append(" | Edits:");

                    for (int i = 0; i <= edits.size() - 1; i++) {
                        builder.append(" ").append(edits.get(i));

                        if ((edits.size() - 1) == i) {
                            builder.append(" ->");
                        }
                    }
                    log = builder.toString();
                }

                if (message.isDeleted() && message.getId() != 0) {
                    String[] split = log.split("]: ");
                    log = split[0] + "]: ~~" + split[1] + "~~";
                }
                writer.write(log);
                writer.newLine();
            }
        } catch (IOException e) {
            log.error("Could not clean transcript of ticket #" + id, e);
        }
        return transcript;
    }

    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("[hh:mm:ss a '|' dd'th' MMM yyyy] ").format(new Date(timestamp * 1000));
    }
}