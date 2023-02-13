package eu.greev.dcbot.ticketsystem.entities;

import eu.greev.dcbot.ticketsystem.service.TicketData;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Builder
public class Ticket {
    @Getter private User owner;
    @Getter private User supporter;
    @Getter private User closer;
    @Getter @Builder.Default private String topic = "No topic given";
    @Getter @Builder.Default private String info = Strings.EMPTY;
    @Getter @Builder.Default private ArrayList<String> involved = new ArrayList<>();
    @Getter @Setter String tempMsgId;
    @Getter private String baseMessage;
    @Getter private final int id;
    @Getter private TextChannel textChannel;
    @Getter private ThreadChannel threadChannel;
    private final TicketData ticketData;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(10);

    public Ticket setOwner(User owner) {
        this.owner = owner;
        this.save();
        return this;
    }

    public Ticket setSupporter(User supporter) {
        this.supporter = supporter;
        this.save();
        return this;
    }

    public Ticket setCloser(User closer) {
        this.closer = closer;
        this.save();
        return this;
    }

    public Ticket setTopic(String topic) {
        this.topic = topic;
        this.save();
        return this;
    }

    public Ticket setInfo(String info) {
        this.info = info;
        this.save();
        return this;
    }

    public Ticket setTextChannel(TextChannel textChannel) {
        this.textChannel = textChannel;
        this.save();
        return this;
    }

    public Ticket setBaseMessage(String baseMessage) {
        this.baseMessage = baseMessage;
        this.save();
        return this;
    }

    public Ticket setThreadChannel(ThreadChannel threadChannel) {
        this.threadChannel = threadChannel;
        this.save();
        return this;
    }

    public Ticket addInvolved(String involved) {
        if (!this.involved.contains(involved)) {
            this.involved.add(involved);
            this.save();
        }
        return this;
    }

    public Ticket removeInvolved(String involved) {
        if (this.involved.remove(involved)) {
            this.save();
        }
        return this;
    }

    public void save() {
        EXECUTOR.execute(() -> ticketData.saveTicket(this));
    }

    @Override
    public String toString() {
        return "Id:{" + id + "},Channel:{" + textChannel + "},Owner:{" + owner + "},Topic:{" + topic + "},Info:{" + info + "},Supporter:{" + supporter + "},Involved:{" + involved + "}";
    }
}