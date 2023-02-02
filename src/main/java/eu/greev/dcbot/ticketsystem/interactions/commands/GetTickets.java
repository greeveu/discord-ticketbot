package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.util.List;

public class GetTickets extends AbstractCommand {

    public GetTickets(Config config, TicketService ticketService, EmbedBuilder missingPerm, JDA jda) {
        super(config, ticketService, missingPerm, jda);
    }


    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        if (!fromGuild(event)) return;
        if (config.getServerName() == null) {
            EmbedBuilder error = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setDescription("❌ **Ticketsystem wasn't setup, please tell an Admin to use </ticket setup:0>!**");
            event.replyEmbeds(error.build()).setEphemeral(true).queue();
            return;
        }
        if (!event.getMember().getRoles().contains(jda.getRoleById(config.getStaffId()))) {
            event.replyEmbeds(missingPerm.setFooter(config.getServerName(), config.getServerLogo()).build()).setEphemeral(true).queue();
            return;
        }
        List<Integer> tickets = ticketService.getTicketIdsByOwner(event.getOption("member").getAsUser());
        EmbedBuilder builder = new EmbedBuilder()
                .setFooter(config.getServerName(), config.getServerLogo())
                .setColor(Color.decode(config.getColor()));

        if (tickets.isEmpty()) {
            builder.setColor(Color.RED)
                    .setDescription("This user never opened a ticket");
        } else {
            builder.setTitle("This user opened following tickets:");
            for (Integer id : tickets) {
                Ticket ticket = ticketService.getTicketByTicketId(id);
                builder.addField(generateName(ticket.getTopic(), id), ticket.getTopic(), false);
            }
        }
        event.replyEmbeds(builder.build()).setEphemeral(true).queue();
    }

    private String generateName(String topic, int ticketId) {
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