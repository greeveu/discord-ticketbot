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

public class SetOwner extends AbstractCommand {
    private final EmbedBuilder wrongChannel;

    public SetOwner(Config config, TicketService ticketService, EmbedBuilder missingPerm, EmbedBuilder wrongChannel, JDA jda) {
        super(config, ticketService, missingPerm, jda);
        this.wrongChannel = wrongChannel;
    }

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
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
        if (ticketService.getTicketByChannelId(event.getChannel().getIdLong()) == null) {
            event.replyEmbeds(wrongChannel.setFooter(config.getServerName(), config.getServerLogo())
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

        Member member = event.getOption("member").getAsMember();
        if (!member.getUser().equals(ticket.getOwner())) {
            if (ticketService.setOwner(ticket, member)) {
                builder.setColor(Color.decode(config.getColor()))
                        .setAuthor(event.getUser().getName(), event.getUser().getEffectiveAvatarUrl())
                        .addField("✅ **New owner**", member.getAsMention() + " is now the new owner of the ticket", false);
                event.replyEmbeds(builder.build()).queue();
            }else {
                builder.addField("❌ **Setting new owner failed**", "This user has not access to this channel, please add them first", false);
                event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            }
        }else {
            builder.addField("❌ **Setting new owner failed**", "This member is already the creator", false);
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
        }
    }
}