package eu.greev.dcbot.ticketsystem.interactions;

import eu.greev.dcbot.ticketsystem.service.TicketService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;

@AllArgsConstructor
@Slf4j
public class TicketClose implements Interaction{
    private final EmbedBuilder wrongChannel;
    private final EmbedBuilder missingPerm;
    private final TicketService ticketService;
    private final Role staff;

    @Override
    public void execute(Event evt) {
        if (evt instanceof ButtonInteractionEvent event) {
            if (!event.getMember().getRoles().contains(staff)) {
                event.replyEmbeds(missingPerm.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl()).build()).setEphemeral(true).queue();
                return;
            }
            if (ticketService.getTicketByChannelId(event.getChannel().getIdLong()) == null) {
                event.replyEmbeds(wrongChannel.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl()).build()).setEphemeral(true).queue();
                return;
            }
            EmbedBuilder builder = new EmbedBuilder().setColor(Color.WHITE)
                    .addField("Close Confirmation", "Do you really want to close this ticket?", true);
            event.replyEmbeds(builder.build())
                    .addActionRow(Button.primary("ticket-confirm", "✔️ Close"))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        if (evt instanceof SlashCommandInteractionEvent event) {
            if (!event.getMember().getRoles().contains(staff)) {
                event.replyEmbeds(missingPerm.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl()).build()).setEphemeral(true).queue();
                return;
            }
            if (!event.getMessageChannel().getName().contains("ticket-")) {
                event.replyEmbeds(wrongChannel.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl()).build()).setEphemeral(true).queue();
                return;
            }
            EmbedBuilder builder = new EmbedBuilder().setColor(Color.WHITE)
                    .addField("Close Confirmation", "Do you really want to close this ticket?", true);
            event.replyEmbeds(builder.build())
                    .addActionRow(Button.primary("ticket-confirm", "✔️ Close"))
                    .setEphemeral(true)
                    .queue();
        }
    }
}