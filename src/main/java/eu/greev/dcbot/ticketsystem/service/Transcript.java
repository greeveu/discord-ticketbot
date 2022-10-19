package eu.greev.dcbot.ticketsystem.service;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.List;

@Slf4j
public class Transcript {
    private final int id;
    @Getter private final File transcript;

    public Transcript(Ticket ticket) {
        id = ticket.getId();
        new File("./Tickets/transcripts").mkdirs();
        transcript = new File("./Tickets/transcripts/" + id + ".txt");
        try {
            if (transcript.createNewFile()) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(transcript, true))) {
                    writer.write("Transcript of ticket #" + id);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            log.error("Could not create transcript", e);
        }
    }

    public void addMessage(String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(transcript, true))) {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            log.error("Could not create transcript of ticket #" + id, e);
        }
    }

    public void editMessage(String messageId, String content) {
        File temp = new File("./Tickets/transcripts/" + id + ".temp");
        try {
            temp.createNewFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(temp, true));
                 BufferedReader reader = new BufferedReader(new FileReader(transcript))) {
                List<String> lines = reader.lines().toList();
                int edits = 1;

                StringBuilder contentBuilder = new StringBuilder(content);
                for (String line : lines) {
                    if (line.split("}")[0].equals(messageId)) {
                        if (line.split("~edit-").length > 1) {
                            edits += Integer.parseInt(line.split("~edit-")[line.split("~edit-").length - 1].substring(0, 1));
                        }
                        if (edits == 1) {
                            contentBuilder.insert(0, "~original~: " + line.split(":>>> ")[1] + " | ~edit-1~:>> ");
                        } else {
                            contentBuilder.insert(0, line.split("~edit-" + edits)[0].split(":>>> ")[1] + "  ~edit-" + edits + "~:>> ");
                        }
                        writer.write(line.split(":>>> ")[0] + ":>>> " + contentBuilder);
                        writer.newLine();
                    } else {
                        writer.write(line);
                        writer.newLine();
                    }
                }
                transcript.delete();
                temp.renameTo(new File("./Tickets/transcripts/" + id + ".txt"));
            }
        } catch (IOException e) {
            log.error("Could not read transcript of ticket #" + id, e);
        }
    }

    public void deleteMessage(String messageId) {
        try {
            File temp = new File("./Tickets/transcripts/" + id + ".temp");
            temp.createNewFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(temp, true));
                 BufferedReader reader = new BufferedReader(new FileReader(transcript))) {
                List<String> lines = reader.lines().toList();
                for (String line : lines) {
                    if (line.contains(messageId)) {
                        String log;
                        try {
                            log = line.replace(":>>> " + line.split(":>>> ")[1], ":>>> ~~" + line.split(":>>> ")[1] + "~~");
                        } catch (ArrayIndexOutOfBoundsException e) { return; }
                        writer.write(log);
                        writer.newLine();
                    } else {
                        writer.write(line);
                        writer.newLine();
                    }
                    transcript.delete();
                    temp.renameTo(new File("./Tickets/transcripts/" + id + ".txt"));
                }
            }
        } catch (IOException e) {
            log.error("Could not read transcript of ticket #" + id, e);
        }
    }

    public File clean() {
        File temp = new File("./Tickets/transcripts/" + id + ".temp");
        try {
            temp.createNewFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(temp, true));
                 BufferedReader reader = new BufferedReader(new FileReader(transcript));) {
                List<String> lines = reader.lines().toList();

                for (String line : lines) {
                    if (lines.get(1).equals(line)) continue;
                    if (lines.get(0).equals(line)) {
                        writer.write("Transcript of ticket: #" + id);
                        writer.newLine();
                        continue;
                    }
                    if (!line.contains("} ")) {
                        writer.write(line);
                        writer.newLine();
                        continue;
                    }
                    String content = line.split("} ")[1];
                    writer.write(content);
                    writer.newLine();
                }
            }
            transcript.delete();
            temp.renameTo(transcript);
        } catch (IOException e) {
            log.error("Could not clean transcript of ticket #" + id, e);
        }
        return transcript;
    }
}