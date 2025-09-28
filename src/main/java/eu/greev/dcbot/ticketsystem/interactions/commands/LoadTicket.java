package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.Map;

public class LoadTicket extends AbstractCommand {

    public LoadTicket(Config config, TicketService ticketService, EmbedBuilder missingPerm, JDA jda) {
        super(config, ticketService, missingPerm, jda);
    }

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        if (!event.getMember().getRoles().contains(jda.getRoleById(config.getStaffId()))) {
            event.replyEmbeds(missingPerm.setFooter(config.getServerName(), config.getServerLogo()).build()).setEphemeral(true).queue();
            return;
        }
        int ticketID = event.getOption("ticket-id").getAsInt();
        Ticket ticket = ticketService.getTicketByTicketId(ticketID);
        EmbedBuilder builder = new EmbedBuilder()
                .setColor(Color.RED)
                .setFooter(config.getServerName(), config.getServerLogo());
        if (ticket == null) {
            builder.setDescription("❌ **Invalid ticket id**");
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            return;
        } else if (ticket.getTextChannel() != null && event.getGuild().getGuildChannelById(ticket.getTextChannel().getIdLong()) != null) {
            builder.setDescription("❌ **Ticket is still open**");
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            return;
        }

        builder.setColor(Color.decode(config.getColor()))
                .setTitle("Ticket #" + ticketID)
                .setAuthor(event.getMember().getEffectiveName(), null, event.getMember().getEffectiveAvatarUrl())
                .addField("Owner", ticket.getOwner().getAsMention(), false)
                .addField("Closer", ticket.getCloser() == null ? "No closer" : ticket.getCloser().getAsMention(), false);

        if (ticket.getSupporter() != null)
            builder.addField("Supporter", ticket.getSupporter().getAsMention(), false);
        for (Map.Entry<String, String> entry : ticket.getInfo().entrySet()) {
            builder.addField(entry.getKey(), entry.getValue(), false);
        }
        if (!ticket.getInvolved().isEmpty())
            builder.addField("Involved", ticket.getInvolved().toString(), false);

        event.replyEmbeds(builder.build())
                .setActionRow(Button.secondary("transcript", "Get transcript"))
                .setEphemeral(true)
                .queue();
    }
}