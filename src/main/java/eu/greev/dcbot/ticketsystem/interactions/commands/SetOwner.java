package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.simpleyaml.configuration.file.YamlFile;

import java.awt.*;

@AllArgsConstructor
public class SetOwner extends AbstractCommand {
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
                    .setFooter(config.getString("data.serverName"), config.getString("data.serverLogo"))
                    .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                    .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
        EmbedBuilder builder = new EmbedBuilder()
                .setFooter(config.getString("data.serverName"), config.getString("data.serverLogo"))
                .setColor(Color.RED);

        Member member = event.getOption("staff").getAsMember();
        if (!member.getUser().equals(ticket.getOwner())) {
            if (ticketService.setOwner(ticket, member)) {
                builder.setColor(getColor(config.getString("data.color")))
                        .setAuthor(event.getUser().getName(), event.getUser().getEffectiveAvatarUrl())
                        .addField("✅ **New owner**", member.getAsMention() + " is now the new owner of the ticket", false);
                event.replyEmbeds(builder.build()).queue();
            }else {
                builder.addField("❌ **Setting new owner failed**", "This user has not access to this channel, please add them first", false);
                event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            }
        }else {
            builder.addField("❌ **Setting new owner failed**", "This member is already the creator", false);
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
        }
    }
}