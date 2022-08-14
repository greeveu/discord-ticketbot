package eu.greev.dcbot.ticketsystem;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TicketData {
    private final String ticketId;
    private final DataSource dataSource;

    public TicketData(String ticketId, DataSource dataSource) {
        this.ticketId = ticketId;
        this.dataSource = dataSource;

        if (!String.valueOf(getCurrentTickets().size()).equals(ticketId)) {
            try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement(
                    "INSERT INTO tickets(ticketID, owner, supporter, involved) VALUES(?, '', '', '')"
            )) {
                statement.setString(1, ticketId);
                statement.execute();
            } catch (SQLException e) {
                System.out.println("Could not set ticketID: " + e);
            }
        }
    }

    public void setSupporter(String supporter) {
        try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement(
                "UPDATE tickets SET supporter =? WHERE ticketID =?"
        )) {
            statement.setString(1, ticketId);
            statement.setString(2, supporter);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Could not set supporter: " + e);
        }
    }

    public void setOwner(String owner) {
        try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement(
                "UPDATE tickets SET owner = ? WHERE ticketID = ?"
        )) {
            statement.setString(1, ticketId);
            statement.setString(2, owner);
            statement.execute();
        } catch (SQLException e) {
            System.out.println("Could not set owner: " + e);
        }
    }

    public String getOwner() {
        try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement(
                "SELECT owner FROM tickets WHERE ticketID = ?"
        )) {
            statement.setString(1, ticketId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("owner");
            }
        } catch (SQLException e) {
            System.out.println("Could not get owner: " + e);
        }
        return "";
    }

    public String getSupporter() {
        try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement(
                "SELECT supporter FROM tickets WHERE ticketID = ?"
        )) {
            statement.setString(1, ticketId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("supporter");
            }
        } catch (SQLException e) {
            System.out.println("Could not get supporter: " + e);
        }
        return "";
    }

    public List<String> getInvolved() {
        List<String> involved = new ArrayList<>();
        try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement(
                "SELECT involved FROM tickets WHERE ticketID = ?"
        )) {
            statement.setString(1, ticketId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String s = Arrays.toString(resultSet.getString("involved").split(", "));
                if (!s.equals("[]")) {
                    involved.add(s);
                }
                return involved;
            }
        } catch (SQLException e) {
            System.out.println("Could not get involved members: " + e);
        }
        return involved;
    }

    public void addInvolved(String involved) {
        List<String> strings = getInvolved();
        StringBuilder value = new StringBuilder();

        if (strings.isEmpty()) {
            value = new StringBuilder(involved);
        }else {
            for (String string : strings) {
                value.append(string).append(", ").append(involved);
            }
        }

        System.out.println(value);

        try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement(
                "UPDATE tickets SET involved = ? WHERE ticketID = ?"
        )){
            statement.setString(1, ticketId);
            statement.setString(2, value.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Could not add involved member: " + e);
        }
    }

    public void removeInvolved(String involved) {
        StringBuilder value = new StringBuilder();
        List<String> strings = getInvolved();
        strings.remove(involved);
        for (int i = 0; i < strings.size(); i++) {
            if (strings.size() - 1 == i) {
                value.append(strings.get(i));
            }else {
                value.append(strings.get(i)).append(", ");
            }
        }

        try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement(
                "UPDATE tickets SET involved = ? WHERE ticketID = ?"
        )){
            statement.setString(1, ticketId);
            statement.setString(2, value.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Could not remove involved member: " + e);
        }
    }

    public List<String> getCurrentTickets() {
        List<String> tickets = new ArrayList<>();
        try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement(
                "SELECT ticketID FROM tickets")){
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) tickets.add(String.valueOf(resultSet.getRow()));
        } catch (SQLException e) {
            System.out.println("Could not get current tickets: " + e);
        }
        return tickets;
    }
}