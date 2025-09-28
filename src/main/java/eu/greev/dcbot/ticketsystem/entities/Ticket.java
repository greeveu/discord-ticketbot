package eu.greev.dcbot.ticketsystem.entities;

import eu.greev.dcbot.ticketsystem.categories.ICategory;
import eu.greev.dcbot.ticketsystem.service.TicketData;
import eu.greev.dcbot.ticketsystem.service.Transcript;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Builder
public class Ticket {
    @Getter private User owner;
    @Getter private User supporter;
    @Getter private User closer;
    @Getter private ICategory category;
    @Getter private Map<String, String> info;
    @Getter @Builder.Default private ArrayList<String> involved = new ArrayList<>();
    @Getter boolean isWaiting;
    @Getter private Instant waitingSince;
    @Getter private int remindersSent;
    @Getter private int supporterRemindersSent;
    @Getter private String closeMessage;
    @Getter @Setter String tempMsgId;
    @Getter @Setter Transcript transcript;
    @Getter private String baseMessage;
    @Getter private final int id;
    @Getter private TextChannel textChannel;
    @Getter private ThreadChannel threadChannel;
    @Getter private boolean isOpen;
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

    public Ticket setOpen(boolean isOpen) {
        this.isOpen = isOpen;
        this.save();
        return this;
    }

    public Ticket setInfo(Map<String, String> info) {
        this.info = info;
        this.save();
        return this;
    }

    public Ticket setWaiting(boolean isWaiting) {
        this.isWaiting = isWaiting;
        this.save();
        return this;
    }

    public Ticket setWaitingSince(Instant waitingSince) {
        this.waitingSince = waitingSince;
        this.save();
        return this;
    }

    public Ticket setRemindersSent(int remindersSent) {
        this.remindersSent = remindersSent;
        this.save();
        return this;
    }

    public Ticket setSupporterRemindersSent(int supporterRemindersSent) {
        this.supporterRemindersSent = supporterRemindersSent;
        this.save();
        return this;
    }

    public Ticket setCloseMessage(String closeMessage) {
        this.closeMessage = closeMessage;
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
}