package eu.greev.dcbot.ticketsystem.interactions.buttons;

import eu.greev.dcbot.Main;
import eu.greev.dcbot.ticketsystem.entities.ScrollEntity;
import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.interactions.commands.GetTickets;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.TicketEmojis;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.List;

import static eu.greev.dcbot.ticketsystem.interactions.commands.GetTickets.PAGE_SIZE;

@AllArgsConstructor
public class TicketsForward extends AbstractButton{
    private final TicketService ticketService;

    @Override
    public void execute(Event evt) {
        ButtonInteractionEvent event = (ButtonInteractionEvent) evt;
        EmbedBuilder builder = new EmbedBuilder(event.getMessage().getEmbeds().get(0));

        ScrollEntity entity = GetTickets.PAGE_SCROLL_CACHE.stream()
                .filter(e -> e.getHandlerId() == event.getUser().getIdLong())
                .findFirst()
                .orElse(null);

        if (entity == null) {
            EmbedBuilder error = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setDescription("❌ **This button expired, please use </ticket get-tickets:%s> again**".formatted(Main.getGetTicketCommandId()));
            event.replyEmbeds(error.build()).setEphemeral(true).queue();
            return;
        }

        int maxPage = entity.getMaxPage();
        int currentPage = entity.getCurrentPage();

        builder.clearFields().setDescription("Page %s/%s".formatted(currentPage + 1, maxPage));

        if (currentPage == maxPage) {
            builder.setDescription("You already are on the last page").setAuthor(null).setTitle(null);
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            return;
        }

        List<Integer> tickets = ticketService.getTicketIdsByOwner(entity.getUserId());

        for (int i = currentPage * PAGE_SIZE; i < (currentPage + 1) * PAGE_SIZE; i++) {
            if (tickets.size() == i) break;
            Ticket ticket = ticketService.getTicketByTicketId(tickets.get(i));
            if (ticket == null) {
                EmbedBuilder error = new EmbedBuilder()
                        .setColor(Color.RED)
                        .setDescription("❌ **Something went wrong, please report this to the Bot creator!**");
                event.replyEmbeds(error.build()).setEphemeral(true).queue();
                return;
            }
            builder.addField(GetTickets.generateName(ticket.getCategory(), tickets.get(i)), "", true);
        }

        entity.setCurrentPage(currentPage + 1);

        event.replyEmbeds(builder.build()).setActionRow(
                Button.primary("tickets-backwards", TicketEmojis.BACKWARDS.getEmoji()),
                Button.primary("tickets-forwards", TicketEmojis.FORWARDS.getEmoji())
        ).setEphemeral(true).queue();
    }
}