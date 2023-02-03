package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

public class Create extends AbstractCommand {
    private final TicketData ticketData;

    public Create(Config config, TicketService ticketService, TicketData ticketData, EmbedBuilder missingPerm, JDA jda) {
        super(config, ticketService, missingPerm, jda);
        this.ticketData = ticketData;
    }

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        String topic;
        if (event.getOption("topic") == null) {
            topic = "No topic given";
        }else {
            topic = event.getOption("topic").getAsString();
        }
        if (ticketService.createNewTicket("", topic, event.getUser())) {
            Ticket ticket = ticketService.getTicketByTicketId(ticketData.getLastTicketId());
            EmbedBuilder builder = new EmbedBuilder().setAuthor(event.getMember().getEffectiveName(), null, event.getMember().getEffectiveAvatarUrl())
                    .addField("✅ **Ticket created**", "Successfully created a ticket for you " + ticket.getChannel().getAsMention(), false)
                    .setColor(Color.decode(config.getColor()))
                    .setFooter(config.getServerName(), config.getServerLogo());

            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
        } else {
            EmbedBuilder builder = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setFooter(config.getServerName(), config.getServerLogo());

            event.getGuild().getTextChannels().forEach(channel -> {
                if (channel.getName().contains("ticket-") && channel.getPermissionOverride(event.getMember()).getAllowed().contains(Permission.VIEW_CHANNEL)) {
                    builder.addField("❌ **Creating ticket failed**", "There is already an opened ticket for you. Please use this instead first or close it -> " + channel.getAsMention(), false);
                }
            });
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
        }
    }
}