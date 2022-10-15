package eu.greev.dcbot.ticketsystem.interactions.buttons;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.ticketsystem.service.Transcript;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.simpleyaml.configuration.file.YamlFile;

import java.awt.*;

@AllArgsConstructor
public class GetTranscript extends AbstractButton {
    private final YamlFile config;
    private final TicketService ticketService;

    @Override
    public void execute(Event evt) {
        ButtonInteractionEvent event = (ButtonInteractionEvent) evt;
        if (config.getString("data.serverName") == null) {
            EmbedBuilder error = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setDescription("❌ **Ticketsystem wasn't setup, please tell an Admin to use </ticket setup:0>!**");
            event.replyEmbeds(error.build()).setEphemeral(true).queue();
            return;
        }
        int ticketID = Integer.parseInt(event.getMessage().getEmbeds().get(0).getTitle().replace("Ticket #", ""));
        Ticket ticket = ticketService.getTicketByTicketId(ticketID);

        event.getUser().openPrivateChannel()
                .flatMap(channel -> channel.sendFiles(FileUpload.fromData(new Transcript(ticket).getTranscript())))
                .queue();

        EmbedBuilder builder = new EmbedBuilder().setFooter(config.getString("data.serverName"), config.getString("data.serverLogo"))
                .setAuthor(event.getMember().getEffectiveName(), null, event.getMember().getEffectiveAvatarUrl())
                .setDescription("Sent transcript of Ticket #" + ticketID + " via DM");
        event.replyEmbeds(builder.build()).setEphemeral(true).queue();
    }
}