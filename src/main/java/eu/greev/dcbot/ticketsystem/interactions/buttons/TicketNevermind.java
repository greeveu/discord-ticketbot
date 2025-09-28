package eu.greev.dcbot.ticketsystem.interactions.buttons;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.awt.*;

@AllArgsConstructor
public class TicketNevermind extends AbstractButton {
    private final TicketService ticketService;
    private final Config config;

    @Override
    public void execute(Event evt) {
        ButtonInteractionEvent event = (ButtonInteractionEvent) evt;
        if (config.getServerName() == null) {
            EmbedBuilder error = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setDescription("❌ **Ticketsystem wasn't setup, please tell an Admin to use </ticket setup:0>!**");
            event.replyEmbeds(error.build()).setEphemeral(true).queue();
            return;
        }
        Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
        if (ticket.getOwner().equals(event.getUser())) {
            ticketService.closeTicket(ticket, true, event.getMember(), null);
        }else {
            EmbedBuilder builder = new EmbedBuilder().setColor(Color.RED)
                    .addField("❌ **Missing access**", "You can not click this button", false)
                    .setFooter(config.getServerName(), config.getServerLogo());
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
        }
    }
}