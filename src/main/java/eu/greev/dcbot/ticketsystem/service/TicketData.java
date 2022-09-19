package eu.greev.dcbot.ticketsystem.service;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import org.jdbi.v3.core.Jdbi;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TicketData {
    private final Jdbi jdbi;
    private final JDA jda;

    public TicketData(JDA jda, Jdbi jdbi) {
        this.jdbi = jdbi;
        this.jda = jda;
    }

    protected Ticket loadTicket(String ticketID) {
        Ticket ticket = new Ticket(ticketID, this);

        jdbi.withHandle(handle -> handle.createQuery("SELECT * FROM tickets WHERE ticketID = ?")
                .bind(0, ticket.getId())
                .map((resultSet, index, ctx) -> {
                    jda.retrieveUserById(resultSet.getString("owner")).complete();
                    ticket.setChannel(jda.getGuildById(Constants.SERVER_ID).getTextChannelById(resultSet.getString("channelID")));
                    ticket.setOwner(jda.getUserById(resultSet.getString("owner")));
                    ticket.setTopic(resultSet.getString("topic"));
                    ticket.setInvolved(new  ArrayList<>(List.of(resultSet.getString("involved").split(", "))));
                    if (!resultSet.getString("supporter").equals("")) {
                        jda.retrieveUserById(resultSet.getString("supporter")).complete();
                        ticket.setSupporter(jda.getUserById(resultSet.getString("supporter")));
                    }
                    return "";
                })
                .first());
        return ticket;
    }

    protected Ticket loadTicket(long ticketChannelID) {
        return this.loadTicket(getTicketIdByChannelId(Long.toString(ticketChannelID)));
    }

    /*public List<String> getCurrentTickets() {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT ticketID FROM tickets")
                .mapTo(String.class)
                .list());
    }*/

    public Integer getLastTicketId() {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT ticketID FROM tickets ORDER BY ticketID DESC LIMIT 1")
                .mapTo(Integer.class).findFirst().orElse(0));
    }

    public String getTicketIdByChannelId(String channelID) {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT ticketID FROM tickets WHERE channelID = ?")
                .bind(0, channelID)
                .mapTo(String.class)
                .first());
    }

    public void saveTicket(Ticket ticket) {
        jdbi.withHandle(handle -> handle.createUpdate("UPDATE tickets SET channelID=?, topic=?, owner=?, supporter=?, involved=? WHERE ticketID =?")
                .bind(0, ticket.getChannel() != null ? ticket.getChannel().getId() : "")
                .bind(1, ticket.getTopic() != null ? ticket.getTopic() : "No topic given")
                .bind(2, ticket.getOwner().getId())
                .bind(3, ticket.getSupporter() != null ? ticket.getSupporter().getId() : "")
                .bind(4, ticket.getInvolved()  == null || ticket.getInvolved().isEmpty() ?
                        "" : ticket.getInvolved().toString().replace("[", "").replace("]", ""))
                .bind(5, ticket.getId())
                .execute());
    }
}