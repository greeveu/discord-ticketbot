package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

public class AddMember extends AbstractCommand {
    private final EmbedBuilder wrongChannel;

    public AddMember(Config config, JDA jda, TicketService ticketService, EmbedBuilder wrongChannel, EmbedBuilder missingPerm) {
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
            event.replyEmbeds(wrongChannel.setFooter(config.getServerName(), config.getServerLogo())
                    .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                    .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }
        EmbedBuilder builder = new EmbedBuilder()
                .setColor(Color.RED)
                .setFooter(config.getServerName(), config.getServerLogo());

        if (event.getOption("member").getAsMember().getRoles().contains(jda.getRoleById(config.getStaffId()))) {
            builder.addField("❌ **Adding member failed**", event.getOption("member").getAsUser().getAsMention() + " is a staff member, they is already in the ticket.", false);

            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            return;
        }

        Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
        if (ticketService.addUser(ticket, event.getOption("member").getAsUser())) {
            builder.setColor(Color.decode(config.getColor()))
                    .setAuthor(event.getUser().getName(), event.getUser().getEffectiveAvatarUrl())
                    .addField("✅ **Member added**", event.getOption("member").getAsUser().getAsMention() + " got added to the ticket", false);
            event.replyEmbeds(builder.build()).queue();
        } else {
            builder.addField("❌ **Adding member failed**", event.getOption("member").getAsUser().getAsMention() + " is already in the ticket", false);
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
        }
    }
}