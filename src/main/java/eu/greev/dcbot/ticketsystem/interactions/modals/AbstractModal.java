package eu.greev.dcbot.ticketsystem.interactions.modals;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.service.TicketData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

import java.awt.*;

@AllArgsConstructor
@Getter
public abstract class AbstractModal implements Interaction {
    private final TicketService ticketService;
    private final TicketData ticketData;

    @Override
    public void execute(Event evt) {
        ModalInteractionEvent event = (ModalInteractionEvent) evt;

        if (ticketService.createNewTicket(getTicketInfo(event), getTicketTopic(event), event.getUser())) {
            Ticket ticket = ticketService.getTicketByTicketId(String.valueOf(ticketData.getLastTicketId()));
            EmbedBuilder builder = new EmbedBuilder().setAuthor(event.getMember().getEffectiveName(), null, event.getMember().getEffectiveAvatarUrl())
                    .addField("✅ **Ticket created**", "Successfully created a ticket for you " + ticket.getChannel().getAsMention(), false)
                    .setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO);
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
        } else {
            EmbedBuilder builder = new EmbedBuilder();
            event.getGuild().getTextChannels().forEach(channel -> {
                PermissionOverride override = channel.getPermissionOverride(event.getMember());
                if (override == null) return;
                if (!channel.getName().contains("ticket-") || !channel.getPermissionOverride(event.getMember()).getAllowed().contains(Permission.VIEW_CHANNEL)) return;

                builder.setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO)
                        .setColor(Color.RED)
                        .addField("❌ **Creating ticket failed**", "There is already an opened ticket for you. Please use this instead first or close it -> " + channel.getAsMention(), false);
            });
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
        }
    }

    abstract String getTicketInfo(ModalInteractionEvent event);

    abstract String getTicketTopic(ModalInteractionEvent event);
}
