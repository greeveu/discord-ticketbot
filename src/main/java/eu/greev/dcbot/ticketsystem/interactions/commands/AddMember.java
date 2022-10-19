package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

@AllArgsConstructor
public class AddMember extends AbstractCommand {
    private final JDA jda;
    private final Config config;
    private final TicketService ticketService;
    private final EmbedBuilder wrongChannel;
    private final EmbedBuilder missingPerm;

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