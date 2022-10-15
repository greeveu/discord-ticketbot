package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.ticketsystem.service.Transcript;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.simpleyaml.configuration.file.YamlFile;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

@AllArgsConstructor
public class SetSupporter extends AbstractCommand {
    private final JDA jda;
    private YamlFile config;
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
                            .setFooter(config.getString("data.serverName"), config.getString("data.serverLogo"))
                            .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                            .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
        Member sup = event.getOption("staff").getAsMember();
        if (sup.getRoles().contains(jda.getRoleById(config.getLong("data.staffId"))) || !sup.getUser().equals(ticket.getSupporter())) {
            ticket.setSupporter(sup.getUser());
            EmbedBuilder builder = new EmbedBuilder().setFooter(config.getString("data.serverName"), config.getString("data.serverLogo"))
                    .setColor(getColor(config.getString("data.color")))
                    .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                    .addField("✅ **New supporter**", sup.getAsMention() + " is the new supporter", false);
            String content = new SimpleDateFormat("[hh:mm:ss a '|' dd'th' MMM yyyy] ").format(new Date(System.currentTimeMillis()))
                    + "> Ticket got transferred to [" + sup.getUser().getName() + "#" + sup.getUser().getDiscriminator() + "].";
            new Transcript(ticket).addMessage(content);
            event.replyEmbeds(builder.build()).queue();
        }else {
            EmbedBuilder builder = new EmbedBuilder().setFooter(config.getString("data.serverName"), config.getString("data.serverLogo"))
                    .setColor(Color.RED)
                    .addField("❌ **Setting new supporter failed**", "This member is either already the supporter or not a staff member", false);
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
        }
    }
}
