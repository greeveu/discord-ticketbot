package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

public class ThreadJoin extends AbstractCommand {
    private final EmbedBuilder wrongChannel;

    public ThreadJoin(Config config, TicketService ticketService, EmbedBuilder wrongChannel, EmbedBuilder missingPerm, JDA jda) {
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
        Ticket ticket = ticketService.getTicketByChannelId(event.getGuildChannel().asTextChannel().getIdLong());
        if (ticket == null) {
            event.replyEmbeds(wrongChannel
                            .setFooter(config.getServerName(), config.getServerLogo())
                            .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                            .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }
        ThreadChannel thread = ticket.getThreadChannel();

        if (thread.getMembers().contains(event.getMember())) {
            event.replyEmbeds(new EmbedBuilder().setFooter(config.getServerName(), config.getServerLogo())
                    .setColor(Color.RED)
                    .addField("❌ **Joining ticket thread failed**", "You already are in the ticket thread", false)
                    .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }
        thread.addThreadMember(event.getMember()).queue();
    }
}
