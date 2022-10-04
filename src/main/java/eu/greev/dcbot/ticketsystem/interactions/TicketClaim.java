package eu.greev.dcbot.ticketsystem.interactions;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Constants;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

@AllArgsConstructor
@Slf4j
public class TicketClaim implements Interaction {
    private final EmbedBuilder wrongChannel;
    private final EmbedBuilder missingPerm;
    private final Role staff;
    private final TicketService ticketService;

    @Override
    public void execute(Event evt) {
        ButtonInteractionEvent event = (ButtonInteractionEvent) evt;
        if (event.getMember().getRoles().contains(staff)) {
            if (event.getMessageChannel().getName().contains("ticket-")) {
                Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
                if (ticketService.claim(ticket, event.getUser())) {
                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO);
                    builder.setColor(Constants.GREEV_GREEN);
                    builder.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl());
                    builder.addField("✅ **Ticket claimed**", "Your ticket will be handled by " + event.getUser().getAsMention(), false);

                    File transcript = new File("./GreevTickets/transcripts/" + ticket.getId() + ".txt");
                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(transcript));
                        List<String> lines = reader.lines().toList();
                        reader.close();
                        EmbedBuilder builder1 = new EmbedBuilder();
                        builder1.setColor(new Color(63, 226, 69, 255));
                        builder1.setDescription("Hello there, " + ticket.getOwner().getAsMention() + "! " + """
                                A member of staff will assist you shortly.
                                In the mean time, please describe your issue in as much detail as possible! :)
                                """);
                        builder1.addField("Topic", ticket.getTopic(), false);
                        builder1.setAuthor(ticket.getOwner().getName(), null, ticket.getOwner().getEffectiveAvatarUrl());
                        builder1.setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO);

                        ticket.getChannel().editMessageEmbedsById(lines.get(0), builder1.build()).setActionRow(Button.danger("close", "Close")).queue();
                    } catch (IOException e) {
                        log.error("Failed reading File", e);
                    }
                    event.replyEmbeds(builder.build()).queue();
                } else {
                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setColor(Color.RED);
                    builder.addField("❌ **Failed claiming**", "You can not claim this ticket!", false);
                    builder.setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO);

                    event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                }
            } else {
                event.replyEmbeds(wrongChannel.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl()).build()).setEphemeral(true).queue();
            }
        } else {
            event.replyEmbeds(missingPerm.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl()).build()).setEphemeral(true).queue();
        }
    }
}
