package eu.greev.dcbot.ticketsystem.service;

import eu.greev.dcbot.ticketsystem.entities.Message;
import eu.greev.dcbot.ticketsystem.entities.Ticket;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Slf4j
public class Transcript { //TODO -> rework this class: 1 transcript object per ticket, save in ticket object (dont forget loading it if not cached); hashmap for messages; file itself only as save method, not to change anything directly in it
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
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(transcript, true))) {
                    writer.write("Transcript of ticket #" + ticket.getId());
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            log.error("Could not create transcript", e);
        }
    }

    public void addMessage(net.dv8tion.jda.api.entities.Message message) {
        messages.add(new Message(message.getIdLong(), message.getContentDisplay(), message.getTimeCreated().toEpochSecond()));
    }

    public void addLogMessage(String log, long timestamp) {
        messages.add(new Message(0, log, timestamp));
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

    public File toFile() {
        File temp = new File("./Tickets/transcripts/" + ticket.getId() + ".temp");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(temp, true))) {
            temp.createNewFile();

            for (Message message : messages) {
                String log = message.getOriginalContent();


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
}