package eu.greev.dcbot.ticketsystem.service;

import eu.greev.dcbot.ticketsystem.entities.Message;
import eu.greev.dcbot.ticketsystem.entities.Ticket;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Slf4j
public class Transcript {
    private final Ticket ticket;
    private final List<Message> messages;
    private final File transcript;

    public Transcript(Ticket ticket, List<Message> messages) {
        this.ticket = ticket;
        this.messages = messages;
        new File("./Tickets/transcripts").mkdirs();
        transcript = new File("./Tickets/transcripts/" + ticket.getId() + ".txt");
        try {
            if (transcript.createNewFile()) {
                messages.add(0, new Message(0, "Transcript of ticket #" + ticket.getId(), "", Instant.now().getEpochSecond()));
            }
        } catch (IOException e) {
            log.error("Could not create transcript", e);
        }
    }

    public void addMessage(net.dv8tion.jda.api.entities.Message message) {
        messages.add(new Message(message.getIdLong(), message.getContentDisplay(), message.getAuthor().getAsMention(), message.getTimeCreated().toEpochSecond()));
    }

    public void addLogMessage(String log, long timestamp) {
        messages.add(new Message(0, log, "", timestamp));
    }

    public void editMessage(long messageId, String content) {
        messages.stream()
                .filter(m -> m.getId() == messageId)
                .findFirst().ifPresent(m -> m.getEditedContent().add(content));
    }

    public void deleteMessage(long messageId) {
        messages.stream()
                .filter(m -> m.getId() == messageId)
                .findFirst().ifPresent(m -> m.setDeleted(true));
    }

    public File toFile(boolean clean) {
        File temp = new File("./Tickets/transcripts/" + ticket.getId() + ".temp");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(temp, true))) {
            temp.createNewFile();

            for (Message message : messages) {
                String log =  formatTimestamp(message.getTimestamp()) + "[" + message.getAuthorMention() + "]:>>> ";
                log = clean ? log : message.getId() + "}" + log ;

                List<String> edits = message.getEditedContent();
                if (!edits.isEmpty()) {
                    StringBuilder builder = new StringBuilder(log).append(message.getOriginalContent()).append(" ~#Edits:");

                    for (int i = 0; i <= edits.size() - 1; i++) {
                        builder.append(" ").append(edits.get(i));

                        if ((edits.size() - 1) == i) {
                            builder.append(" ->");
                        }
                    }
                    log = builder.toString();
                }

                if (message.isDeleted() && message.getId() != 0) {
                    String[] split = log.split("]:>>> ");
                    log = split[0] + "]:>>> ~~~" + split[1] + "~~~";
                }

                writer.write(log);
                writer.newLine();
            }

            temp.renameTo(transcript);
        } catch (IOException e) {
            log.error("Could not clean transcript of ticket #" + ticket.getId(), e);
        }
        return transcript;
    }

    public void delete() {
        messages.clear();
        transcript.delete();
    }

    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("[hh:mm:ss a '|' dd'th' MMM yyyy] ").format(new Date(timestamp * 1000));
    }
}