package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.categories.ICategory;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class GetTickets extends AbstractCommand {
    public static final List<ScrollEntity> PAGE_SCROLL_CACHE = new ArrayList<>();
    public static final int PAGE_SIZE = 25; // this value shouldn't be greater than 25 because of Discord limitations

    public GetTickets(Config config, TicketService ticketService, EmbedBuilder missingPerm, JDA jda) {
        super(config, ticketService, missingPerm, jda);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                PAGE_SCROLL_CACHE.removeIf(entity -> Instant.now()
                        .minus(12, ChronoUnit.HOURS)
                        .isAfter(Instant.ofEpochMilli(entity.getTimeCreated()))
                );
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

        for (int i = 0; i < PAGE_SIZE; i++) {
            if (tickets.size() == i) break;
            Ticket ticket = ticketService.getTicketByTicketId(tickets.get(i));
            if (ticket == null) {
                EmbedBuilder error = new EmbedBuilder()
                        .setColor(Color.RED)
                        .setDescription("❌ **Something went wrong, please report this to the Bot creator!**");
                event.replyEmbeds(error.build()).setEphemeral(true).queue();
                return;
            }
            builder.addField(generateName(ticket.getCategory(), tickets.get(i)), "", true);
        }

        int maxPage = tickets.size() / PAGE_SIZE + (tickets.size() % PAGE_SIZE == 0 ? 0 : 1);
        PAGE_SCROLL_CACHE.removeIf(e -> e.getHandlerId() == event.getMember().getIdLong());
        ScrollEntity scrollEntity = new ScrollEntity(event.getMember().getIdLong(), user.getIdLong(), maxPage, Instant.now().toEpochMilli());

        event.replyEmbeds(builder.setDescription("Page 1/%d".formatted(maxPage)).build()).setActionRow(
                Button.primary("tickets-backwards", Emoji.fromUnicode("◀️")),
                Button.primary("tickets-forwards", Emoji.fromUnicode("▶️"))
        ).setEphemeral(true).queue(s -> PAGE_SCROLL_CACHE.add(scrollEntity));
    }

    public static String generateName(ICategory category, int ticketId) {

        return category.getLabel() + " # " + ticketId;
    }
}