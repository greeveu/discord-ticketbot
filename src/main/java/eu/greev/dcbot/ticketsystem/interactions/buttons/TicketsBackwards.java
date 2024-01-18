package eu.greev.dcbot.ticketsystem.interactions.buttons;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.interactions.commands.GetTickets;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.List;

@AllArgsConstructor
public class TicketsBackwards extends AbstractButton{
    private final TicketService ticketService;

    @Override
    public void execute(Event evt) {
        ButtonInteractionEvent event = (ButtonInteractionEvent) evt;
        MessageEmbed embed = event.getMessage().getEmbeds().get(0);

        EmbedBuilder builder = new EmbedBuilder(embed);

        int maxPage = Integer.parseInt(embed.getDescription().split("/")[1]);
        int currentPage = Integer.parseInt(embed.getDescription().split("[/ ]")[1]);

        builder.clearFields().setDescription("Page %s/%s".formatted(currentPage - 1, maxPage));

        if (currentPage == 1) {
            builder.setDescription("You already are on the first page").setAuthor(null).setTitle(null);
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            return;
        }

        List<Integer> tickets = ticketService.getTicketIdsByOwner(embed.getAuthor().getUrl().split("https://www.discordapp.com/users/")[1]);

        for (int i = (currentPage-2) * 25; i < (currentPage-1) * 25; i++) {
            if (tickets.size() == i) break;
            Ticket ticket = ticketService.getTicketByTicketId(tickets.get(i));
            if (ticket == null) {
                EmbedBuilder error = new EmbedBuilder()
                        .setColor(Color.RED)
                        .setDescription("❌ **Something went wrong, please report this to the Bot creator!**");
                event.replyEmbeds(error.build()).setEphemeral(true).queue();
                return;
            }
            builder.addField(GetTickets.generateName(ticket.getTopic(), tickets.get(i)), ticket.getTopic(), false);
        }

        event.replyEmbeds(builder.build()).setActionRow(
                Button.primary("tickets-backwards", Emoji.fromUnicode("◀️")),
                Button.primary("tickets-forwards", Emoji.fromUnicode("▶️"))
        ).setEphemeral(true).queue();
    }
}