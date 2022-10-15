package eu.greev.dcbot.ticketsystem.interactions;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.simpleyaml.configuration.file.YamlFile;

import java.awt.*;

@AllArgsConstructor
@Slf4j
public class TicketClaim implements Interaction {
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

            Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
            if (ticketService.claim(ticket, event.getUser())) {
                EmbedBuilder builder = new EmbedBuilder().setFooter(config.getString("data.serverName"), config.getString("data.serverLogo"))
                        .setColor(getColor(config.getString("data.color")))
                        .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                        .addField("✅ **Ticket claimed**", "Your ticket will be handled by " + event.getUser().getAsMention(), false);
                event.replyEmbeds(builder.build()).queue();
            } else {
                EmbedBuilder builder = new EmbedBuilder().setColor(Color.RED)
                        .addField("❌ **Failed claiming**", "You can not claim this ticket!", false)
                        .setFooter(config.getString("data.serverName"), config.getString("data.serverLogo"));
                event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            }
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

            Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
            if (ticketService.claim(ticket, event.getUser())) {
                EmbedBuilder builder = new EmbedBuilder().setFooter(config.getString("data.serverName"), config.getString("data.serverLogo"))
                        .setColor(getColor(config.getString("data.color")))
                        .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                        .addField("✅ **Ticket claimed**", "Your ticket will be handled by " + event.getUser().getAsMention(), false);
                event.replyEmbeds(builder.build()).queue();
            } else {
                EmbedBuilder builder = new EmbedBuilder().setColor(Color.RED)
                        .addField("❌ **Failed claiming**", "You can not claim this ticket!", false)
                        .setFooter(config.getString("data.serverName"), config.getString("data.serverLogo"));
                event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            }
        }
    }

    Color getColor(String colorFormat) {
        Color color = new Color(63, 226, 69, 255);
        switch (colorFormat) {
            case "BLACK" -> color = Color.BLACK;
            case "BLUE" -> color = Color.BLUE;
            case "CYAN" -> color = Color.CYAN;
            case "DARK_GRAY" -> color = Color.DARK_GRAY;
            case "GRAY" -> color = Color.GRAY;
            case "GREEN" -> color = Color.GREEN;
            case "LIGHT_GRAY" -> color = Color.LIGHT_GRAY;
            case "MAGENTA" -> color = Color.MAGENTA;
            case "ORANGE" -> color = Color.ORANGE;
            case "PINK" -> color = Color.PINK;
            case "RED" -> color = Color.RED;
            case "WHITE" -> color = Color.WHITE;
            case "YELLOW" -> color = Color.YELLOW;
        }
        return color;
    }
}