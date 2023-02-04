package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

public class RemoveMember extends AbstractCommand {
    private final EmbedBuilder wrongChannel;

    public RemoveMember(Config config, TicketService ticketService, EmbedBuilder missingPerm, EmbedBuilder wrongChannel, JDA jda) {
        super(config, ticketService, missingPerm, jda);
        this.wrongChannel = wrongChannel;
    }

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        if (!event.getMember().getRoles().contains(jda.getRoleById(config.getStaffId()))) {
            event.replyEmbeds(missingPerm.setFooter(config.getServerName(), config.getServerLogo()).build()).setEphemeral(true).queue();
            return;
        }

        if (ticketService.getTicketByChannelId(event.getChannel().getIdLong()) == null) {
            event.replyEmbeds(wrongChannel
                    .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                    .setFooter(config.getServerName(), config.getServerLogo())
                    .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }
        EmbedBuilder builder = new EmbedBuilder()
                .setFooter(config.getServerName(), config.getServerLogo())
                .setColor(Color.RED);

        if (event.getOption("member").getAsMember().getRoles().contains(jda.getRoleById(config.getStaffId()))) {
            builder.addField("❌ **Removing member failed**", event.getOption("member").getAsUser().getAsMention() + " is a staff member, you can not remove them from this ticket.", false);
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            return;
        }

        Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
        if (ticketService.removeUser(ticket, event.getOption("member").getAsUser())) {
            builder.setAuthor(event.getUser().getName(), event.getUser().getEffectiveAvatarUrl())
                    .setColor(Color.decode(config.getColor()))
                    .addField("✅ **Member removed**", event.getOption("member").getAsUser().getAsMention() + " got removed from the ticket", false);
            event.replyEmbeds(builder.build()).queue();
        } else {
            builder.addField("❌ **Removing member failed**", event.getOption("member").getAsUser().getAsMention() + " is already not in the ticket", false);
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
        }
    }
}