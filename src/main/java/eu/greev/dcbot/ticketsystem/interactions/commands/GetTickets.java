package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Constants;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.util.List;

@AllArgsConstructor
public class GetTickets extends AbstractCommand{
    private final Role staff;
    private final EmbedBuilder missingPerm;
    private final TicketService ticketService;

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        if (!event.getMember().getRoles().contains(staff)) {
            event.replyEmbeds(missingPerm.build()).setEphemeral(true).queue();
            return;
        }
        List<Integer> tickets = ticketService.getTicketIdsByOwner(event.getOption("member").getAsUser());
        EmbedBuilder builder = new EmbedBuilder().setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO).setColor(Constants.GREEV_GREEN);

        if (tickets.isEmpty()) {
            builder.setColor(Color.RED)
                    .setDescription("This user never opened a ticket");
        } else {
            tickets.forEach(id -> builder.addField(String.valueOf(id), "", false).setAuthor("This user opened following tickets:"));
        }
        event.replyEmbeds(builder.build()).setEphemeral(true).queue();
    }
}