package eu.greev.dcbot.ticketsystem.interactions;

import eu.greev.dcbot.ticketsystem.service.TicketService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.simpleyaml.configuration.file.YamlFile;

import java.awt.*;

@AllArgsConstructor
@Slf4j
public class TicketClose implements Interaction{
    private final JDA jda;
    private final YamlFile config;
    private final EmbedBuilder wrongChannel;
    private final EmbedBuilder missingPerm;
    private final TicketService ticketService;

    @Override
    public void execute(Event evt) {
        if (evt instanceof ButtonInteractionEvent event) {
            if (config.getString("data.serverName") == null) {
                EmbedBuilder error = new EmbedBuilder()
                        .setColor(Color.RED)
                        .setDescription("❌ **Ticketsystem wasn't setup, please tell an Admin to use </ticket setup:0>!**");
                event.replyEmbeds(error.build()).setEphemeral(true).queue();
                return;
            }
            if (!event.getMember().getRoles().contains(jda.getRoleById(config.getLong("data.staffId")))) {
                event.replyEmbeds(missingPerm.setFooter(config.getString("data.serverName"), config.getString("data.serverLogo")).build()).setEphemeral(true).queue();
                return;
            }
            if (ticketService.getTicketByChannelId(event.getChannel().getIdLong()) == null) {
                event.replyEmbeds(wrongChannel
                                .setFooter(config.getString("data.serverName"), config.getString("data.serverLogo"))
                                .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                                .build())
                        .setEphemeral(true)
                        .queue();
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
            if (config.getString("data.serverName") == null) {
                EmbedBuilder error = new EmbedBuilder()
                        .setColor(Color.RED)
                        .setDescription("❌ **Ticketsystem wasn't setup, please tell an Admin to use </ticket setup:0>!**");
                event.replyEmbeds(error.build()).setEphemeral(true).queue();
                return;
            }
            if (!event.getMember().getRoles().contains(jda.getRoleById(config.getLong("data.staffId")))) {
                event.replyEmbeds(missingPerm.setFooter(config.getString("data.serverName"), config.getString("data.serverLogo")).build()).setEphemeral(true).queue();
                return;
            }
            if (ticketService.getTicketByChannelId(event.getChannel().getIdLong()) == null) {
                event.replyEmbeds(wrongChannel
                                .setFooter(config.getString("data.serverName"), config.getString("data.serverLogo"))
                                .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                                .build())
                        .setEphemeral(true)
                        .queue();
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