package eu.greev.dcbot.ticketsystem.service;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TicketData {
    private static DataSource dataSource;
    private static JDA jda;

    public TicketData(JDA jda, DataSource dataSource) {
        TicketData.dataSource = dataSource;
        TicketData.jda = jda;
    }

    public static Ticket loadTicket(String ticketID) {
        Ticket ticket = new Ticket(ticketID, dataSource);
        try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement(
                "SELECT * FROM tickets WHERE ticketID = ?"
        )) {
            statement.setString(1, ticket.getId());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                try {
                    jda.retrieveUserById(resultSet.getString("owner")).complete();
                    jda.retrieveUserById(resultSet.getString("supporter")).complete();
                } catch (ErrorResponseException ignore) {}
                ticket.setOwner(jda.getUserById(resultSet.getString("owner")));
                ticket.setSupporter(jda.getUserById(resultSet.getString("supporter")));
                ticket.setTopic(resultSet.getString("topic"));
                ticket.setInvolved(List.of(resultSet.getString("involved").split(", ")));
                ticket.setChannel(jda.getGuildById(Constants.SERVER_ID).getTextChannelById(resultSet.getString("channelID")));
            }
        } catch (SQLException e) {
            log.error(ticketID + ": Could not load ticket", e);
        }
        return ticket;
    }

    public static Ticket loadTicket(long ticketChannelID) {
        Ticket ticket = new Ticket(getTicketIdByChannelId(Long.toString(ticketChannelID)), dataSource);
        try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement(
                "SELECT * FROM tickets WHERE ticketID = ?"
        )) {
            statement.setString(1, ticket.getId());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                try {
                    jda.retrieveUserById(resultSet.getString("owner")).complete();
                    jda.retrieveUserById(resultSet.getString("supporter")).complete();
                } catch (ErrorResponseException ignore) {}
                ticket.setOwner(jda.getUserById(resultSet.getString("owner")));
                ticket.setSupporter(jda.getUserById(resultSet.getString("supporter")));
                ticket.setTopic(resultSet.getString("topic"));
                ticket.setInvolved(List.of(resultSet.getString("involved").split(", ")));
                ticket.setChannel(jda.getGuildById(Constants.SERVER_ID).getTextChannelById(resultSet.getString("channelID")));
            }
        } catch (SQLException e) {
            log.error(ticket.getId() + ": Could not load ticket", e);
        }
        return ticket;
    }

    public static List<String> getCurrentTickets() {
        List<String> tickets = new ArrayList<>();
        try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement(
                "SELECT ticketID FROM tickets")){
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                tickets.add(resultSet.getString("ticketID"));
            }
        } catch (SQLException e) {
            log.error("Could not get current tickets", e);
        }
        return tickets;
    }

    public static String getTicketIdByChannelId(String channelID) {
        try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement(
                "SELECT ticketID FROM tickets WHERE channelID = ?"
        )){
            statement.setString(1, channelID);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("ticketID");
            }
        } catch (SQLException e) {
            log.error(channelID + ": Could not get ticketID", e);
        }
        return "";
    }
}