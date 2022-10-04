package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Constants;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

@AllArgsConstructor
@Slf4j
public class SetTopic extends AbstractCommand {
    private final Role STAFF;
    private final TicketService ticketService;
    private final EmbedBuilder wrongChannel;
    private final EmbedBuilder missingPerm;

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        Member member = event.getMember();

        if (member.getRoles().contains(STAFF)) {
            if (event.getMessageChannel().getName().contains("ticket-")) {
                Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
                ticketService.setTopic(ticket, event.getOption("topic").getAsString());

                EmbedBuilder builder = new EmbedBuilder();
                builder.setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO);
                builder.setColor(Constants.GREEV_GREEN);
                builder.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl());
                builder.addField("✅ **New Topic**", "Changed topic to '" + event.getOption("topic").getAsString() + "'", false);

                File transcript = new File("./GreevTickets/transcripts/" + ticket.getId() + ".txt");
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(transcript));
                    List<String> lines = reader.lines().toList();
                    reader.close();
                    EmbedBuilder builder1 = new EmbedBuilder();
                    builder1.setColor(Constants.GREEV_GREEN);
                    builder1.setDescription("Hello there, " + ticket.getOwner().getAsMention() + "! " + """
                                        A member of staff will assist you shortly.
                                        In the mean time, please describe your issue in as much detail as possible! :)
                                        """);
                    builder1.addField("Topic", ticket.getTopic(), false);
                    builder1.setAuthor(ticket.getOwner().getName(),null, ticket.getOwner().getEffectiveAvatarUrl());
                    builder.setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO);

                    if (ticket.getChannel().getTopic().split(" \\| ").length > 2) {
                        ticket.getChannel().editMessageEmbedsById(lines.get(1), builder1.build()).setActionRow(net.dv8tion.jda.api.interactions.components.buttons.Button.danger("ticket-close", "Close")).queue();
                    }else {
                        ticket.getChannel().editMessageEmbedsById(lines.get(1), builder1.build()).setActionRow(net.dv8tion.jda.api.interactions.components.buttons.Button.primary("ticket-claim", "Claim"),
                                Button.danger("ticket-close", "Close")).queue();
                    }
                } catch (IOException e) {
                    log.error("Failed reading File", e);
                }
                event.replyEmbeds(builder.build()).queue();
            } else {
                event.replyEmbeds(wrongChannel.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl()).build()).setEphemeral(true).queue();
            }
        } else {
            event.replyEmbeds(missingPerm.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl()).build()).setEphemeral(true).queue();
        }
    }
}
