package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.simpleyaml.configuration.file.YamlFile;

import java.awt.*;

@AllArgsConstructor
public class RemoveMember extends AbstractCommand {
    private final JDA jda;
    private final YamlFile config;
    private final TicketService ticketService;
    private final EmbedBuilder wrongChannel;
    private final EmbedBuilder missingPerm;

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
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
                    .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                    .setFooter(config.getString("data.serverName"), config.getString("data.serverLogo"))
                    .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }
        EmbedBuilder builder = new EmbedBuilder()
                .setFooter(config.getString("data.serverName"), config.getString("data.serverLogo"))
                .setColor(Color.RED);

        if (event.getOption("member").getAsMember().getRoles().contains(jda.getRoleById(config.getLong("data.staffId")))) {
            builder.addField("❌ **Removing member failed**", event.getOption("member").getAsUser().getAsMention() + " is a staff member, you can not remove them from this ticket.", false);
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            return;
        }

        Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
        if (ticketService.removeUser(ticket, event.getOption("member").getAsUser())) {
            builder.setAuthor(event.getUser().getName(), event.getUser().getEffectiveAvatarUrl())
                    .addField("✅ **Member removed**", event.getOption("member").getAsUser().getAsMention() + " got removed from the ticket", false);
            event.replyEmbeds(builder.build()).queue();
        } else {
            builder.addField("❌ **Removing member failed**", event.getOption("member").getAsUser().getAsMention() + " is already not in the ticket", false);
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
        }
    }
}
