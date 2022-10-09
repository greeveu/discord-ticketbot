package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.ticketsystem.service.Transcript;
import eu.greev.dcbot.utils.Constants;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.*;

@AllArgsConstructor
public class GetTranscript extends AbstractCommand{
    private final Role staff;
    private final EmbedBuilder missingPerm;
    private final TicketService ticketService;

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        if (!event.getMember().getRoles().contains(staff)) {
            event.replyEmbeds(missingPerm.build()).setEphemeral(true).queue();
            return;
        }

        Ticket ticket = ticketService.getTicketByTicketId(event.getOption("ticket").getAsString());
        if (ticket == null) {
            EmbedBuilder builder = new EmbedBuilder().setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO)
                    .setColor(Color.RED)
                    .setDescription("❌ **Invalid ticket id**");
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            return;
        } //TODO -> check if ticket is still open

        event.getUser().openPrivateChannel()
                .flatMap(channel -> channel.sendFiles(FileUpload.fromData(new Transcript(ticket).getTranscript())))
                .queue();

        EmbedBuilder builder = new EmbedBuilder().setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO)
                .setAuthor(event.getMember().getEffectiveName(), null, event.getMember().getEffectiveAvatarUrl())
                .setDescription("Sent transcript of ticket " + event.getOption("ticket").getAsString() + " via DM");
        event.replyEmbeds(builder.build()).setEphemeral(true).queue();
    }
}
