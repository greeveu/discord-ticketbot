package eu.greev.dcbot.scheduler;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import java.awt.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@AllArgsConstructor
public class HourlyScheduler {
    private static final int REMIND_INTERVAL_HOURS = 24;
    private static final int REMIND_SUPPORTER_HOURS = 24;
    private static final int AUTO_CLOSE_HOURS = 96;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Config config;
    private TicketService ticketService;
    private TicketData ticketData;
    private JDA jda;

    public void start() {
        scheduler.scheduleAtFixedRate(this::run, getInitialDelay(), 60 * 60, TimeUnit.SECONDS);
    }

    private int getInitialDelay() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextHour = now.plusHours(1).withMinute(0).withSecond(0).withNano(0);
        return (int) Duration.between(now, nextHour).getSeconds();
    }

    private void run() {
        log.info("Running hourly ticket check...");

        List<Integer> ticketsIds = ticketData.getOpenTicketsIds();

        for (Integer ticketId : ticketsIds) {
            log.debug("Checking ticket ID: {}", ticketId);
            Ticket ticket = ticketService.getTicketByTicketId(ticketId);

            boolean shouldRemind = ticket.isWaiting() && ticket.getWaitingSince() != null &&
                    ticket.getWaitingSince()
                            .plus((long) REMIND_INTERVAL_HOURS * (ticket.getRemindersSent() + 1), ChronoUnit.HOURS)
                            .isBefore(Instant.now());

            boolean shouldClose = ticket.isWaiting() && ticket.getWaitingSince() != null &&
                    ticket.getWaitingSince()
                            .plus(AUTO_CLOSE_HOURS, ChronoUnit.HOURS)
                            .isBefore(Instant.now());

            AtomicBoolean shouldRemindSupporter = new AtomicBoolean(false);

            if (shouldClose) {
                ticketService.closeTicket(ticket, false, jda.getGuildById(config.getServerId()).getSelfMember(), "Automatic close due to inactivity");
            } else if (shouldRemind) {
                EmbedBuilder builder = new EmbedBuilder()
                        .setTitle(String.format("⏰ Reminder: Waiting for your response (%s/%s)", ticket.getRemindersSent() + 1, AUTO_CLOSE_HOURS / REMIND_INTERVAL_HOURS - 1))
                        .setColor(Color.decode(config.getColor()))
                        .appendDescription("**Our support team is waiting for you to respond**")
                        .appendDescription(String.format("\nIf you do not respond, the ticket will be automatically closed <t:%d:R>.",
                                ticket.getWaitingSince()
                                        .plus(Duration.ofHours(AUTO_CLOSE_HOURS + 1))
                                        .atZone(ZoneId.of("UTC"))
                                        .withMinute(0)
                                        .toInstant()
                                        .toEpochMilli() / 1000)
                        )
                        .setFooter(config.getServerName(), config.getServerLogo());

                EmbedBuilder threadMessageBuilder = new EmbedBuilder()
                        .setTitle("The reminder was sent to %s (%s/%s)".formatted(ticket.getOwner().getAsMention(), ticket.getRemindersSent() + 1, AUTO_CLOSE_HOURS / REMIND_INTERVAL_HOURS - 1))
                        .setColor(Color.decode(config.getColor()))
                        .setFooter(config.getServerName(), config.getServerLogo());

                try {
                    ticket.getOwner().openPrivateChannel()
                            .flatMap(channel -> channel.sendMessageEmbeds(builder.build()))
                            .complete();
                } catch (ErrorResponseException e) {
                    ticket.getTextChannel()
                            .sendMessage(ticket.getOwner().getAsMention())
                            .setEmbeds(builder.build())
                            .queue();
                }

                ticket.getThreadChannel().sendMessageEmbeds(threadMessageBuilder.build()).queue();

                ticket.setRemindersSent(ticket.getRemindersSent() + 1);
            } else {
                ticket.getTextChannel()
                        .retrieveMessageById(ticket.getTextChannel().getLatestMessageId())
                        .queue(message -> {
                            shouldRemindSupporter.set(!ticket.isWaiting() && ticket.getSupporter() != null &&
                                    message.getTimeCreated()
                                            .plusHours((long) REMIND_SUPPORTER_HOURS * (ticket.getSupporterRemindersSent() + 1))
                                            .isBefore(Instant.now().atZone(ZoneId.of("UTC")).toOffsetDateTime()));

                            if (shouldRemindSupporter.get()) {
                                ticket.getThreadChannel()
                                        .sendMessage(ticket.getSupporter().getAsMention())
                                        .queue();

                                ticket.setSupporterRemindersSent(ticket.getSupporterRemindersSent() + 1);
                            }
                        });
            }

            log.debug("Should remind: {}, Should close: {}", shouldRemind, shouldClose);

            try {
                TimeUnit.SECONDS.sleep(10L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
