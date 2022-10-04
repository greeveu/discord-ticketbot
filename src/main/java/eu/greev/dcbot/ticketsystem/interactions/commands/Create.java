package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Constants;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

@AllArgsConstructor
public class Create extends AbstractCommand {
    private final TicketService ticketService;
    private final TicketData ticketData;

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;

        String topic;
        if (event.getOption("topic") == null) {
            topic = "";
        }else {
            topic = event.getOption("topic").getAsString();
        }
        if (ticketService.createNewTicket("", topic, event.getUser())) {
            Ticket ticket = ticketService.getTicketByTicketId((ticketData.getLastTicketId()) + "");
            EmbedBuilder builder = new EmbedBuilder();
            builder.setAuthor(event.getMember().getEffectiveName(), null, event.getMember().getEffectiveAvatarUrl());
            builder.addField("✅ **Ticket created**", "Successfully created a ticket for you " + ticket.getChannel().getAsMention(), false);
            builder.setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO);

            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
        } else {
            EmbedBuilder builder = new EmbedBuilder();
            event.getGuild().getTextChannels().forEach(channel -> {
                if (channel.getName().contains("ticket-") && channel.getPermissionOverride(event.getMember()).getAllowed().contains(Permission.VIEW_CHANNEL)) {
                    builder.setColor(Color.RED);
                    builder.addField("❌ **Creating ticket failed**", "There is already an opened ticket for you. Please use this instead first or close it -> " + channel.getAsMention(), false);
                    builder.setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO);
                }
            });
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
        }
    }
}
