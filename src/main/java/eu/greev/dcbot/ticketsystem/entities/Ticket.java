package eu.greev.dcbot.ticketsystem.entities;

import eu.greev.dcbot.ticketsystem.service.TicketData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.List;

@Slf4j
public class Ticket {
    @Getter private User owner;
    @Getter private User supporter;
    @Getter private String topic = "No topic given";
    @Getter private List<String> involved;
    @Getter private final String id;
    @Getter private TextChannel channel;

    public Ticket(String id) {
        this.id = id;
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
        TicketData.saveTicket(this);
    }

    public void setChannel(TextChannel channel) { this.channel = channel; }

    public void addInvolved(String involved) {
        if (!this.involved.contains(involved)) {
            this.involved.add(involved);
            TicketData.saveTicket(this);
        }
    }

    public void removeInvolved(String involved) {
        this.involved.remove(involved);
        TicketData.saveTicket(this);
    }

    private void updateTopic() {
        if (supporter == null) {
            channel.getManager().setTopic(owner.getAsMention() + " | " + topic).queue();
        }else {
            channel.getManager().setTopic(owner.getAsMention() + " | " + topic + " | " + supporter.getAsMention()).queue();
        }
        TicketData.saveTicket(this);
    }
}