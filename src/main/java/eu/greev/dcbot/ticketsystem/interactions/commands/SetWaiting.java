package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Constants;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

@AllArgsConstructor
public class SetWaiting extends AbstractCommand {
    private final Role staff;
    private final TicketService ticketService;
    private final EmbedBuilder wrongChannel;
    private final EmbedBuilder missingPerm;

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        Member member = event.getMember();
        if (!member.getRoles().contains(staff)) {
            event.replyEmbeds(missingPerm.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl()).build()).setEphemeral(true).queue();
            return;
        }
        if (!event.getMessageChannel().getName().contains("ticket-")) {
            event.replyEmbeds(wrongChannel.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl()).build()).setEphemeral(true).queue();
            return;
        }

        Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
        if (!ticket.getChannel().getName().contains("\uD83D\uDD50")) {
            ticketService.toggleWaiting(ticket, true);
            EmbedBuilder builder = new EmbedBuilder().setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO)
                    .setAuthor(member.getEffectiveName(), null, member.getEffectiveAvatarUrl())
                    .setDescription("Waiting for response.")
                    .setColor(Constants.GREEV_GREEN);
            event.replyEmbeds(builder.build()).queue();
        } else {
            EmbedBuilder builder = new EmbedBuilder().setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO)
                    .setColor(Color.RED)
                    .addField("❌ **Changing waiting mode failed**", "This ticket is already in waiting mode!", false);

            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
        }
    }
}
