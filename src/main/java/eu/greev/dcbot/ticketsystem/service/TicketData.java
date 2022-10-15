package eu.greev.dcbot.ticketsystem.service;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import org.apache.logging.log4j.util.Strings;
import org.jdbi.v3.core.Jdbi;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class TicketData {
    private final JDA jda;
    private final Jdbi jdbi;

    protected Ticket loadTicket(int ticketID) {
        Ticket.TicketBuilder ticket = Ticket.builder().ticketData(this).id(ticketID);

        jdbi.withHandle(handle -> handle.createQuery("SELECT * FROM tickets WHERE ticketID = ?")
                .bind(0, ticketID)
                .map((resultSet, index, ctx) -> {
                    jda.retrieveUserById(resultSet.getString("owner")).complete();
                    ticket.channel(jda.getTextChannelById(resultSet.getString("channelID")))
                            .owner(jda.getUserById(resultSet.getString("owner")))
                            .topic(resultSet.getString("topic"))
                            .info(resultSet.getString("info"))
                            .involved(new ArrayList<>(List.of(resultSet.getString("involved").split(", "))));

                    if (!resultSet.getString("closer").equals(Strings.EMPTY))
                        ticket.closer(jda.retrieveUserById(resultSet.getString("closer")).complete());

                    if (!resultSet.getString("supporter").equals(Strings.EMPTY))
                        ticket.supporter(jda.retrieveUserById(resultSet.getString("supporter")).complete());

                    return null;
                })
                .findFirst());
        log.debug(ticket.build().toString());
        return ticket.build();
    }

    protected Ticket loadTicket(long ticketChannelID) {
        return this.loadTicket(getTicketIdByChannelId(ticketChannelID));
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
        jdbi.withHandle(handle -> handle.createUpdate("UPDATE tickets SET channelID=?, topic=?, info=?, owner=?, supporter=?, involved=? WHERE ticketID =?")
                .bind(0, ticket.getChannel() != null ? ticket.getChannel().getId() : "")
                .bind(1, ticket.getTopic() != null ? ticket.getTopic() : "No topic given")
                .bind(2, ticket.getInfo())
                .bind(3, ticket.getOwner().getId())
                .bind(4, ticket.getSupporter() != null ? ticket.getSupporter().getId() : "")
                .bind(5, ticket.getInvolved()  == null || ticket.getInvolved().isEmpty() ?
                        "" : ticket.getInvolved().toString().replace("[", "").replace("]", ""))
                .bind(6, ticket.getId())
                .execute());
    }
}