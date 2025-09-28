package eu.greev.dcbot.ticketsystem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.greev.dcbot.Main;
import eu.greev.dcbot.ticketsystem.entities.Ticket;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import org.apache.logging.log4j.util.Strings;
import org.jdbi.v3.core.Jdbi;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TicketData {
    private final JDA jda;
    private final Jdbi jdbi;
    @Getter private final TranscriptData transcriptData;

    public TicketData(JDA jda, Jdbi jdbi) {
        this.jda = jda;
        this.jdbi = jdbi;
        this.transcriptData = new TranscriptData(jdbi);
    }

    protected Ticket loadTicket(int ticketID) {
        Ticket.TicketBuilder builder = jdbi.withHandle(handle -> handle.createQuery("SELECT * FROM tickets WHERE ticketID = ?")
                .bind(0, ticketID)
                .map((resultSet, index, ctx) -> {
                    if (resultSet.getString("owner").equals(Strings.EMPTY)) {
                        return null;
                    }

                    String category = resultSet.getString("category");

                    ObjectMapper mapper = new ObjectMapper();

                    Ticket.TicketBuilder ticketBuilder = null;
                    try {
                        ticketBuilder = Ticket.builder()
                                .ticketData(this)
                                .id(ticketID)
                                .textChannel(jda.getTextChannelById(resultSet.getString("channelID")))
                                .threadChannel(!resultSet.getString("threadID").equals(Strings.EMPTY)
                                        ? jda.getThreadChannelById(resultSet.getString("threadID")) : null)
                                .owner(jda.retrieveUserById(resultSet.getString("owner")).complete())
                                .category(Main.CATEGORIES.stream().filter(c -> c.getId().equals(category)).findFirst().orElse(null))
                                .info(mapper.readValue(resultSet.getString("info"), new TypeReference<>() {}))
                                .isOpen(resultSet.getBoolean("isOpen"))
                                .isWaiting(resultSet.getBoolean("isWaiting"))
                                .remindersSent(resultSet.getInt("remindersSent"))
                                .supporterRemindersSent(resultSet.getInt("supporterRemindersSent"))
                                .closeMessage(resultSet.getString("closeMessage"))
                                .waitingSince(resultSet.getString("waitingSince") != null ? Instant.parse(resultSet.getString("waitingSince")) : null)
                                .baseMessage(resultSet.getString("baseMessage"))
                                .involved(new ArrayList<>(List.of(resultSet.getString("involved").split(", "))));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }

                    if (!resultSet.getString("closer").equals(Strings.EMPTY)) {
                        ticketBuilder.closer(jda.retrieveUserById(resultSet.getString("closer")).complete());
                    }

                    if (!resultSet.getString("supporter").equals(Strings.EMPTY)) {
                        ticketBuilder.supporter(jda.retrieveUserById(resultSet.getString("supporter")).complete());
                    }

                    return ticketBuilder;
                })
                .findFirst()).orElse(null);

        if (builder == null) {
            return null;
        }

        return builder.transcript(transcriptData.loadTranscript(ticketID)).build();
    }

    protected Ticket loadTicket(long ticketChannelID) {
        return this.loadTicket(getTicketIdByChannelId(ticketChannelID));
    }

    protected List<Integer> getTicketIdsByUser(String user) {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT ticketID FROM tickets WHERE owner=?")
                .bind(0, user)
                .mapTo(Integer.class)
                .list());
    }

    public List<Integer> getOpenTicketsIds() {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT ticketID FROM tickets WHERE isOpen=true")
                .mapTo(Integer.class)
                .list());
    }

    public List<Integer> getOpenTicketsOfUser(String user) {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT ticketID FROM tickets WHERE owner=? AND isOpen=true")
                .bind(0, user)
                .mapTo(Integer.class)
                .list());
    }

    public Integer getLastTicketId() {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT ticketID FROM tickets ORDER BY ticketID DESC LIMIT 1")
                .mapTo(Integer.class).
                findFirst()
                .orElse(0));
    }

    public Integer getTicketIdByChannelId(long channelID) {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT ticketID FROM tickets WHERE channelID = ?")
                .bind(0, channelID)
                .mapTo(Integer.class)
                .findFirst()
                .orElse(0));
    }

    public void saveTicket(Ticket ticket) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            jdbi.withHandle(handle -> handle.createUpdate("UPDATE tickets SET channelID=?, threadID=?, category=?, info=?, isWaiting=?, owner=?, supporter=?, involved=?, baseMessage=?, isOpen=?, waitingSince=?, remindersSent=?, supporterRemindersSent=?, closeMessage=? WHERE ticketID =?")
                    .bind(0, ticket.getTextChannel() != null ? ticket.getTextChannel().getId() : "")
                    .bind(1, ticket.getThreadChannel() != null ? ticket.getThreadChannel().getId() : "")
                    .bind(2, ticket.getCategory().getId())
                    .bind(3, mapper.writeValueAsString(ticket.getInfo()))
                    .bind(4, ticket.isWaiting())
                    .bind(5, ticket.getOwner().getId())
                    .bind(6, ticket.getSupporter() != null ? ticket.getSupporter().getId() : "")
                    .bind(7, ticket.getInvolved()  == null || ticket.getInvolved().isEmpty() ?
                            "" : ticket.getInvolved().toString().replace("[", "").replace("]", ""))
                    .bind(8, ticket.getBaseMessage())
                    .bind(9, ticket.isOpen())
                    .bind(10, ticket.getWaitingSince() == null ? null : ticket.getWaitingSince().toString())
                    .bind(11, ticket.getRemindersSent())
                    .bind(12, ticket.getSupporterRemindersSent())
                    .bind(13, ticket.getCloseMessage())
                    .bind(14, ticket.getId())
                    .execute());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    // Stats queries
    public int countTotalTickets() {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT COUNT(*) FROM tickets")
                .mapTo(Integer.class)
                .findOne()
                .orElse(0));
    }

    public int countOpenTickets() {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT COUNT(*) FROM tickets WHERE isOpen = true")
                .mapTo(Integer.class)
                .findOne()
                .orElse(0));
    }

    public int countWaitingTickets() {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT COUNT(*) FROM tickets WHERE isWaiting = true")
                .mapTo(Integer.class)
                .findOne()
                .orElse(0));
    }

    public java.util.LinkedHashMap<String, Integer> topClosers(int limit) {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT closer, COUNT(*) as c FROM tickets WHERE isOpen = false AND closer != '' GROUP BY closer ORDER BY c DESC LIMIT :limit")
                .bind("limit", limit)
                .reduceRows(new java.util.LinkedHashMap<String, Integer>(), (map, row) -> {
                    map.put(row.getColumn("closer", String.class), row.getColumn("c", Integer.class));
                    return map;
                }));
    }

    public java.util.LinkedHashMap<String, Integer> topOwnersWithOpenTickets(int limit) {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT owner, COUNT(*) as c FROM tickets WHERE isOpen = true AND owner != '' GROUP BY owner ORDER BY c DESC LIMIT :limit")
                .bind("limit", limit)
                .reduceRows(new java.util.LinkedHashMap<String, Integer>(), (map, row) -> {
                    map.put(row.getColumn("owner", String.class), row.getColumn("c", Integer.class));
                    return map;
                }));
    }
}