package eu.greev.dcbot.ticketsystem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TicketData {
    private final Connection connection;

    public TicketData(Connection connection) {
        this.connection = connection;
    }

    public void setSupporter(String ticketId, String supporter) {
        try (Connection conn = connection; PreparedStatement statement = conn.prepareStatement(
                "UPSERT INTO tickets(ticketID, supporter) VALUES(?,?);"
        )) {
            statement.setString(1, ticketId);
            statement.setString(2, supporter);
            statement.execute();
        } catch (SQLException e) {
            System.out.println("Could not set supporter: " + e);
        }
    }

    public void setCreator(String ticketId, String creator) {
        try (Connection conn = connection; PreparedStatement statement = conn.prepareStatement(
                "UPSERT INTO tickets(ticketID, creator) VALUES(?,?);"
        )) {
            statement.setString(1, ticketId);
            statement.setString(2, creator);
            statement.execute();
        } catch (SQLException e) {
            System.out.println("Could not set Creator: " + e);
        }
    }

    public long getCreator(String ticketID) {
        try (Connection conn = connection; PreparedStatement statement = conn.prepareStatement(
                "SELECT creator FROM tickets WHERE ticketID = ?;"
        )) {
            statement.setString(1, ticketID);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong("creator");
            }
        } catch (SQLException e) {
            System.out.println("Could not get creator: " + e);
        }
        return 0;
    }

    public long getSupporter(String ticketID) {
        try (Connection conn = connection; PreparedStatement statement = conn.prepareStatement(
                "SELECT supporter FROM tickets WHERE ticketID = ?;"
        )) {
            statement.setString(1, ticketID);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong("supporter");
            }
        } catch (SQLException e) {
            System.out.println("Could not get supporter: " + e);
        }
        return 0;
    }

    public List<String> getInvolved(String ticketID) {
        List<String> involved = new ArrayList<>();
        try (Connection conn = connection; PreparedStatement statement = conn.prepareStatement(
                "SELECT involved FROM tickets WHERE ticketID = ?;"
        )) {
            statement.setString(1, ticketID);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                involved.add(Arrays.toString(resultSet.getString("involved").split(", ")));
                return involved;
            }
        } catch (SQLException e) {
            System.out.println("Could not get involved members: " + e);
        }
        return involved;
    }

    public void addInvolved(String ticketID) {
        List<String> involved = getInvolved(ticketID);
        final String[] value = {""};
        involved.forEach(s -> {
            value[0] = value[0] + ", " + s;
        });
        try (Connection conn = connection; PreparedStatement statement = conn.prepareStatement(
                "UPSERT INTO tickets(ticketID, involved) VALUES(?,?);"
        )){
            statement.setString(1, ticketID);
            statement.setString(2, value[0]);
        } catch (SQLException e) {
            System.out.println("Could not add involved member: " + e);
        }
    }

    public void removeInvolved(String ticketID, String involved) {
        final String[] strings = {""};
        getInvolved(ticketID).forEach(s -> {
            strings[0] = strings[0] + ", " + s;
        });
        String value = strings[0].replaceAll(involved + ", ", "");
        try (Connection conn = connection; PreparedStatement statement = conn.prepareStatement(
                "UPSERT INTO tickets(ticketID, involved) VALUES(?,?);"
        )){
            statement.setString(1, ticketID);
            statement.setString(2, value);
        } catch (SQLException e) {
            System.out.println("Could not remove involved member: " + e);
        }
    }
}
