package eu.greev.dcbot.ticketsystem.entities;

import eu.greev.dcbot.ticketsystem.service.TicketData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.ArrayList;

@Slf4j
public class Ticket {
    @Getter private User owner;
    @Getter private User supporter;
    @Getter private String topic = "No topic given";
    @Getter private ArrayList<String> involved = new ArrayList<>();
    @Getter private final String id;
    @Getter private TextChannel channel;
    private final TicketData ticketData;

    public Ticket(String id, TicketData ticketData) {
        this.id = id;
        this.ticketData = ticketData;
    }

    public void setOwner(User owner) {
        this.owner = owner;
        ticketData.saveTicket(this);
    }

    public void setSupporter(User supporter) {
        this.supporter = supporter;
        ticketData.saveTicket(this);
    }

    public void setTopic(String topic) {
        this.topic = topic;
        ticketData.saveTicket(this);
    }

    public void setInvolved(ArrayList<String> involved) {
        this.involved = involved;
        ticketData.saveTicket(this);
    }

    public void setChannel(TextChannel channel) { this.channel = channel; }

    public void addInvolved(String involved) {
        if (!this.involved.contains(involved)) {
            this.involved.add(involved);
            ticketData.saveTicket(this);
        }
    }

    public void removeInvolved(String involved) {
        if (this.involved.remove(involved)) {
            ticketData.saveTicket(this);
        }
    }
}