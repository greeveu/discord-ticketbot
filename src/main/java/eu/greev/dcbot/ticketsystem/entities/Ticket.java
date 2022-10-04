package eu.greev.dcbot.ticketsystem.entities;

import eu.greev.dcbot.ticketsystem.service.TicketData;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Builder
public class Ticket {
    @Getter private User owner;
    @Getter private User supporter;
    @Getter @Builder.Default private String topic = "No topic given";
    @Getter @Builder.Default private ArrayList<String> involved = new ArrayList<>();
    @Getter private final String id;
    @Getter private TextChannel channel;
    private final TicketData ticketData;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(10);

    public void setOwner(User owner) {
        this.owner = owner;
        this.save();
    }

    public void setSupporter(User supporter) {
        this.supporter = supporter;
        this.save();
    }

    public void setTopic(String topic) {
        this.topic = topic;
        this.save();
    }

    public void setChannel(TextChannel channel) { this.channel = channel; }

    public void addInvolved(String involved) {
        if (!this.involved.contains(involved)) {
            this.involved.add(involved);
            this.save();
        }
    }

    public void removeInvolved(String involved) {
        if (this.involved.remove(involved)) {
            this.save();
        }
    }

    public void save() {
        EXECUTOR.execute(() -> ticketData.saveTicket(this));
    }

    @Override
    public String toString() {
        return "Id:" + id + ",Channel:" + channel + ",Owner:" + owner + ",Topic:" + topic + ",Supporter:" + supporter + ",Involved:" + involved;
    }
}