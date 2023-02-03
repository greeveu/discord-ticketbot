package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

@Slf4j
public class SetTopic extends AbstractCommand {
    private final EmbedBuilder wrongChannel;

    public SetTopic(Config config, TicketService ticketService, EmbedBuilder missingPerm, EmbedBuilder wrongChannel, JDA jda) {
        super(config, ticketService, missingPerm, jda);
        this.wrongChannel = wrongChannel;
    }

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        if (!event.getMember().getRoles().contains(jda.getRoleById(config.getStaffId()))) {
            event.replyEmbeds(missingPerm.setFooter(config.getServerName(), config.getServerLogo()).build()).setEphemeral(true).queue();
            return;
        }
        if (ticketService.getTicketByChannelId(event.getChannel().getIdLong()) == null) {
            event.replyEmbeds(wrongChannel
                            .setFooter(config.getServerName(), config.getServerLogo())
                            .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                            .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
        ticketService.setTopic(ticket, event.getOption("topic").getAsString());
        EmbedBuilder builder = new EmbedBuilder()
                .setFooter(config.getServerName(), config.getServerLogo())
                .setColor(Color.decode(config.getColor()))
                .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                .addField("✅ **New Topic**", "Changed topic to '" + event.getOption("topic").getAsString() + "'", false);
        File transcript = new File("./Tickets/transcripts/" + ticket.getId() + ".txt");
        try {
            BufferedReader reader = new BufferedReader(new FileReader(transcript));
            List<String> lines = reader.lines().toList();
            reader.close();
            EmbedBuilder builder1 = new EmbedBuilder().setFooter(config.getServerName(), config.getServerLogo())
                    .setColor(Color.decode(config.getColor()))
                    .setDescription("Hello there, " + ticket.getOwner().getAsMention() + "! " + """
                                   A member of staff will assist you shortly.
                                   In the mean time, please describe your issue in as much detail as possible! :)
                                   """)
                    .addField("Topic", ticket.getTopic(), false)
                    .setAuthor(ticket.getOwner().getName(),null, ticket.getOwner().getEffectiveAvatarUrl());
            if (ticket.getTextChannel().getTopic().split(" \\| ").length > 2) {
                ticket.getTextChannel().editMessageEmbedsById(lines.get(1), builder1.build()).setActionRow(Button.danger("ticket-close", "Close")).queue();
            }else {
                ticket.getTextChannel().editMessageEmbedsById(lines.get(1), builder1.build()).setActionRow(Button.primary("ticket-claim", "Claim"),
                        Button.danger("ticket-close", "Close")).queue();
            }
        } catch (IOException e) {
            log.error("Failed reading File", e);
        }
        event.replyEmbeds(builder.build()).queue();
    }
}