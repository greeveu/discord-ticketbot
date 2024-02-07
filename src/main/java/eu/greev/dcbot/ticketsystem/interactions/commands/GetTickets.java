package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.ScrollEntity;
import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class GetTickets extends AbstractCommand {
    public static final List<ScrollEntity> PAGE_SCROLL_CACHE = new ArrayList<>();

    public GetTickets(Config config, TicketService ticketService, EmbedBuilder missingPerm, JDA jda) {
        super(config, ticketService, missingPerm, jda);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                PAGE_SCROLL_CACHE.removeIf(entity -> System.currentTimeMillis() - entity.getTimeCreated() > TimeUnit.HOURS.toMillis(12));
            }
        }, 0, TimeUnit.MINUTES.toMillis(5));
    }

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        if (!event.getMember().getRoles().contains(jda.getRoleById(config.getStaffId()))) {
            event.replyEmbeds(missingPerm.setFooter(config.getServerName(), config.getServerLogo()).build()).setEphemeral(true).queue();
            return;
        }
        User user = event.getOption("member").getAsUser();
        List<Integer> tickets = ticketService.getTicketIdsByOwner(user.getIdLong());
        EmbedBuilder builder = new EmbedBuilder()
                .setAuthor(user.getName(), null, user.getAvatarUrl())
                .setTitle("This user opened following tickets:")
                .setFooter(config.getServerName(), config.getServerLogo())
                .setColor(Color.decode(config.getColor()));

        if (tickets.isEmpty()) {
            builder.setColor(Color.RED).setTitle("This user never opened a ticket");
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            return;
        }

        for (int i = 0; i < 25; i++) {
            if (tickets.size() - 1 == i) break;
            Ticket ticket = ticketService.getTicketByTicketId(tickets.get(i));
            if (ticket == null) {
                EmbedBuilder error = new EmbedBuilder()
                        .setColor(Color.RED)
                        .setDescription("❌ **Something went wrong, please report this to the Bot creator!**");
                event.replyEmbeds(error.build()).setEphemeral(true).queue();
                return;
            }
            builder.addField(generateName(ticket.getTopic(), tickets.get(i)), ticket.getTopic(), false);
        }

        int maxPage = tickets.size() / 25 + (tickets.size() % 25 == 0 ? 0 : 1);
        PAGE_SCROLL_CACHE.removeIf(e -> e.getHandlerId() == event.getMember().getIdLong());
        ScrollEntity scrollEntity = new ScrollEntity(event.getMember().getIdLong(), user.getIdLong(), maxPage, System.currentTimeMillis());

        event.replyEmbeds(builder.setDescription("Page 1/%d".formatted(maxPage)).build()).setActionRow(
                Button.primary("tickets-backwards", Emoji.fromUnicode("◀️")),
                Button.primary("tickets-forwards", Emoji.fromUnicode("▶️"))
        ).setEphemeral(true).queue(s -> PAGE_SCROLL_CACHE.add(scrollEntity));
    }

    public static String generateName(String topic, int ticketId) {
        String name;
        if (topic.equals("Bugreport")) {
            name = "Bugreport #" + ticketId;
        } else if (topic.contains(" wants pardon ")) {
            name = "Pardon #" + ticketId;
        } else if (topic.contains(" apply ")) {
            name = "Application #" + ticketId;
        } else {
            name = "Ticket #" + ticketId;
        }
        return name;
    }
}