package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

public class SetWaiting extends AbstractCommand {
    private final EmbedBuilder wrongChannel;

    public SetWaiting(Config config, TicketService ticketService, EmbedBuilder missingPerm, EmbedBuilder wrongChannel, JDA jda) {
        super(config, ticketService, missingPerm, jda);
        this.wrongChannel = wrongChannel;
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
        Member member = event.getMember();
        if (!event.getMember().getRoles().contains(jda.getRoleById(config.getStaffId()))) {
            event.replyEmbeds(missingPerm.setFooter(config.getServerName(), config.getServerLogo()).build()).setEphemeral(true).queue();
            return;
        }
        if (ticketService.getTicketByChannelId(event.getChannel().getIdLong()) == null) {
            event.replyEmbeds(wrongChannel
                            .setFooter(config.getServerName(), config.getServerLogo())
                            .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                            .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
        EmbedBuilder builder = new EmbedBuilder()
                .setFooter(config.getServerName(), config.getServerLogo())
                .setColor(Color.RED);
        if (!ticket.getChannel().getName().contains("\uD83D\uDD50")) {
            ticketService.toggleWaiting(ticket, true);
            builder.setAuthor(member.getEffectiveName(), null, member.getEffectiveAvatarUrl())
                    .setDescription("Waiting for response.")
                    .setColor(Color.decode(config.getColor()));
            event.replyEmbeds(builder.build()).queue();
        } else {
            builder.addField("❌ **Changing waiting mode failed**", "This ticket is already in waiting mode!", false);
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
        }
    }
}