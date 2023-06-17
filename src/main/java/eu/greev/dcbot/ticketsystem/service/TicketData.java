package eu.greev.dcbot.ticketsystem.service;

import eu.greev.dcbot.ticketsystem.entities.Edit;
import eu.greev.dcbot.ticketsystem.entities.Message;
import eu.greev.dcbot.ticketsystem.entities.Ticket;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import org.apache.logging.log4j.util.Strings;
import org.jdbi.v3.core.Jdbi;

import java.util.ArrayList;
import java.util.List;

@Log4j
@AllArgsConstructor
public class TicketData {
    private final JDA jda;
    private final Jdbi jdbi;

    protected Ticket loadTicket(int ticketID) {
        Ticket.TicketBuilder builder = Ticket.builder().ticketData(this).id(ticketID);

        jdbi.withHandle(handle -> handle.createQuery("SELECT * FROM tickets WHERE ticketID = ?")
                .bind(0, ticketID)
                .map((resultSet, index, ctx) -> {
                    jda.retrieveUserById(resultSet.getString("owner")).complete();
                    builder.textChannel(jda.getTextChannelById(resultSet.getString("channelID")))
                            .threadChannel(!resultSet.getString("threadID").equals(Strings.EMPTY)
                                    ? jda.getThreadChannelById(resultSet.getString("threadID")) : null)
                            .owner(jda.getUserById(resultSet.getString("owner")))
                            .topic(resultSet.getString("topic"))
                            .info(resultSet.getString("info"))
                            .baseMessage(resultSet.getString("baseMessage"))
                            .involved(new ArrayList<>(List.of(resultSet.getString("involved").split(", "))));

                    if (!resultSet.getString("closer").equals(Strings.EMPTY)) {
                        builder.closer(jda.retrieveUserById(resultSet.getString("closer")).complete());
                    }

                    if (!resultSet.getString("supporter").equals(Strings.EMPTY)) {
                        builder.supporter(jda.retrieveUserById(resultSet.getString("supporter")).complete());
                    }

                    return null;
                })
                .findFirst());

        Ticket ticket = builder.build();
        loadTranscript(ticket);

        return builder.build();
    }

    protected Ticket loadTicket(long ticketChannelID) {
        return this.loadTicket(getTicketIdByChannelId(ticketChannelID));
    }

    public void loadTranscript(Ticket ticket) {
        ticket.setTranscript(new Transcript(loadMessages(ticket.getId())));
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

    protected List<Integer> getTicketIdsByUser(User user) {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT ticketID FROM tickets WHERE owner=?")
                .bind(0, user.getId())
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
        jdbi.withHandle(handle -> handle.createUpdate("UPDATE tickets SET channelID=?, threadID=?, topic=?, info=?, owner=?, supporter=?, involved=?, baseMessage=? WHERE ticketID =?")
                .bind(0, ticket.getTextChannel() != null ? ticket.getTextChannel().getId() : "")
                .bind(1, ticket.getThreadChannel() != null ? ticket.getThreadChannel().getId() : "")
                .bind(2, ticket.getTopic() != null ? ticket.getTopic() : "No topic given")
                .bind(3, ticket.getInfo())
                .bind(4, ticket.getOwner().getId())
                .bind(5, ticket.getSupporter() != null ? ticket.getSupporter().getId() : "")
                .bind(6, ticket.getInvolved()  == null || ticket.getInvolved().isEmpty() ?
                        "" : ticket.getInvolved().toString().replace("[", "").replace("]", ""))
                .bind(7, ticket.getBaseMessage())
                .bind(8, ticket.getId())
                .execute());
    }
}