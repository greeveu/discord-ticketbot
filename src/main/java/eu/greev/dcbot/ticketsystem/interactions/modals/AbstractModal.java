package eu.greev.dcbot.ticketsystem.interactions.modals;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.service.TicketData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.simpleyaml.configuration.file.YamlFile;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
@Getter
public abstract class AbstractModal implements Interaction {
    private final YamlFile config;
    private final TicketService ticketService;
    private final TicketData ticketData;
    private final List<String> discordFormattingChars = Arrays.asList("\\", "*", "~", "|", "_", "`");

    @Override
    public void execute(Event evt) {
        ModalInteractionEvent event = (ModalInteractionEvent) evt;
        if (config.getString("data.serverName") == null) {
            EmbedBuilder error = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setDescription("❌ **Ticketsystem wasn't setup, please tell an Admin to use </ticket setup:0>!**");
            event.replyEmbeds(error.build()).setEphemeral(true).queue();
            return;
        }
        EmbedBuilder builder = new EmbedBuilder().setColor(Color.RED)
                .setFooter(config.getString("data.serverName"), config.getString("data.serverLogo"));

        if (ticketService.createNewTicket(escapeFormatting(getTicketInfo(event)), escapeFormatting(getTicketTopic(event)), event.getUser())) {
            Ticket ticket = ticketService.getTicketByTicketId(ticketData.getLastTicketId());
            builder.setAuthor(event.getMember().getEffectiveName(), null, event.getMember().getEffectiveAvatarUrl())
                    .addField("✅ **Ticket created**", "Successfully created a ticket for you " + ticket.getChannel().getAsMention(), false);
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
        } else {
            event.getGuild().getTextChannels().forEach(channel -> {
                PermissionOverride override = channel.getPermissionOverride(event.getMember());
                if (override == null) return;
                if (ticketService.getTicketByChannelId(channel.getIdLong()) == null || !channel.getPermissionOverride(event.getMember()).getAllowed().contains(Permission.VIEW_CHANNEL)) return;

                builder.addField("❌ **Creating ticket failed**", "There is already an opened ticket for you. Please use this instead first or close it -> " + channel.getAsMention(), false);
            });
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
        }

    }

    abstract String getTicketInfo(ModalInteractionEvent event);

    abstract String getTicketTopic(ModalInteractionEvent event);

    private String escapeFormatting(String text) {
        for (String formatString : this.discordFormattingChars) {
            text = text.replace(formatString, "\\" + formatString);
        }
        return text;
    }
}
