package eu.greev.dcbot.ticketsystem.entities;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Slf4j
public class Ticket {
    @Getter private User owner;
    @Getter private User supporter;
    @Getter private String topic = "No topic given";
    @Getter private List<String> involved;
    @Getter private final String id;
    @Getter private TextChannel channel;
    private final DataSource dataSource;

    public Ticket(String id, DataSource dataSource) {
        this.id = id;
        this.dataSource = dataSource;
    }

    public void setOwner(User owner) {
        this.owner = owner;
        updateTopic();
    }

    public void setSupporter(User supporter) {
        this.supporter = supporter;
        updateTopic();
    }

    public void setTopic(String topic) {
        this.topic = topic;
        updateTopic();
    }

    public void setInvolved(List<String> involved) {
        this.involved = involved;
        saveTicket();
    }

    public void setChannel(TextChannel channel) { this.channel = channel; }

    public void addInvolved(String involved) {
        if (!this.involved.contains(involved)) {
            this.involved.add(involved);
            saveTicket();
        }
    }

    public void removeInvolved(String involved) {
        this.involved.remove(involved);
        saveTicket();
    }

    private void updateTopic() {
        if (supporter == null) {
            channel.getManager().setTopic(owner.getAsMention() + " | " + topic).queue();
        }else {
            channel.getManager().setTopic(owner.getAsMention() + " | " + topic + " | " + supporter.getAsMention()).queue();
        }
        saveTicket();
    }

    private void saveTicket() {
        try(Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE tickets(ticketID, channelID, topic, owner, supporter, involved) VALUES(?, ?, ?, ?, ?, ?);")) {
            statement.setString(1, id);
            statement.setString(2, channel != null ? getChannel().getId() : "");
            statement.setString(2, topic != null ? topic : "No topic given");
            statement.setString(4, owner.getId());
            statement.setString(5, supporter != null ? supporter.getId() : "");
            statement.setString(6, involved  == null || involved.isEmpty() ?
                    "" : involved.toString().replace("[", "").replace("]", ""));
            statement.execute();
        } catch (SQLException e) {
            log.error(id + ": Could not save ticket", e);
        }
    }
}