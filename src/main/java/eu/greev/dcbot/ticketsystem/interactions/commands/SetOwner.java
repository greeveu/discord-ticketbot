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
public class SetOwner extends AbstractCommand {
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
        if (ticketService.getTicketByChannelId(event.getChannel().getIdLong()) == null) {
            event.replyEmbeds(wrongChannel.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl()).build()).setEphemeral(true).queue();
            return;
        }

        Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
        if (!event.getOption("member").getAsUser().equals(ticket.getOwner())) {
            if (ticketService.setOwner(ticket, event.getOption("member").getAsMember())) {
                EmbedBuilder builder = new EmbedBuilder().setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO)
                        .setColor(Constants.GREEV_GREEN)
                        .setAuthor(event.getUser().getName(), event.getUser().getEffectiveAvatarUrl())
                        .addField("✅ **New owner**", event.getOption("member").getAsUser().getAsMention() + " is now the new owner of the ticket", false);
                event.replyEmbeds(builder.build()).queue();
            }else {
                EmbedBuilder builder = new EmbedBuilder().setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO)
                        .setColor(Color.RED)
                        .addField("❌ **Setting new owner failed**", "This user has not access to this channel, please add them first", false);
                event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            }
        }else {
            EmbedBuilder builder = new EmbedBuilder().setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO)
                    .setColor(Color.RED)
                    .addField("❌ **Setting new owner failed**", "This member is already the creator", false);
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
        }
    }
}
