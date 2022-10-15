package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.apache.logging.log4j.util.Strings;
import org.simpleyaml.configuration.file.YamlFile;

import java.awt.*;

@AllArgsConstructor
public class LoadTicket extends AbstractCommand{
    private final JDA jda;
    private final YamlFile config;
    private final EmbedBuilder missingPerm;
    private final TicketService ticketService;

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        if (!event.getMember().getRoles().contains(jda.getRoleById(config.getLong("data.staffId")))) {
            event.replyEmbeds(missingPerm.setFooter(config.getString("data.serverName"), config.getString("data.serverLogo")).build()).setEphemeral(true).queue();
            return;
        }
        if (config.getString("data.serverName") == null) {
            EmbedBuilder error = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setDescription("❌ **Ticketsystem wasn't setup, please tell an Admin to use </ticket setup:0>!**");
            event.replyEmbeds(error.build()).setEphemeral(true).queue();
            return;
        }
        int ticketID = event.getOption("ticket-id").getAsInt();
        Ticket ticket = ticketService.getTicketByTicketId(ticketID);
        EmbedBuilder builder = new EmbedBuilder()
                .setColor(Color.RED)
                .setFooter(config.getString("data.serverName"), config.getString("data.serverLogo"));
        if (ticket == null) {
            builder.setDescription("❌ **Invalid ticket id**");
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            return;
        } else if (ticket.getChannel() != null && event.getGuild().getGuildChannelById(ticket.getChannel().getIdLong()) != null) {
            builder.setDescription("❌ **Ticket is still open**");
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            return;
        }

        builder.setColor(getColor(config.getString("data.color")))
                .setTitle("Ticket #" + ticketID)
                .setAuthor(event.getMember().getEffectiveName(), null, event.getMember().getEffectiveAvatarUrl())
                .addField("Topic", ticket.getTopic(), false)
                .addField("Owner", ticket.getOwner().getAsMention(), false)
                .addField("Closer", ticket.getCloser().getAsMention(), false);

        if (ticket.getSupporter() != null)
            builder.addField("Supporter", ticket.getSupporter().getAsMention(), false);
        if (!ticket.getInfo().equals(Strings.EMPTY))
            builder.addField("Information", ticket.getInfo(), false);
        if (ticket.getInvolved().isEmpty())
            builder.addField("Involved", ticket.getInvolved().toString(), false);

        event.replyEmbeds(builder.build())
                .setActionRow(Button.secondary("transcript", "Get transcript"))
                .setEphemeral(true)
                .queue();
    }
}