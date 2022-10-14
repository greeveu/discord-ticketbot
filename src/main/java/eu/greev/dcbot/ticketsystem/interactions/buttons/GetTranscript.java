package eu.greev.dcbot.ticketsystem.interactions.buttons;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.ticketsystem.service.Transcript;
import eu.greev.dcbot.utils.Constants;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

@AllArgsConstructor
public class GetTranscript extends AbstractButton {
    private final Role staff;
    private final EmbedBuilder missingPerm;
    private final TicketService ticketService;

    @Override
    public void execute(Event evt) {
        ButtonInteractionEvent event = (ButtonInteractionEvent) evt;
        if (!event.getMember().getRoles().contains(staff)) {
            event.replyEmbeds(missingPerm.build()).setEphemeral(true).queue();
            return;
        }
        int ticketID = Integer.parseInt(event.getMessage().getEmbeds().get(0).getTitle().replace("Ticket #", ""));
        Ticket ticket = ticketService.getTicketByTicketId(ticketID);

        event.getUser().openPrivateChannel()
                .flatMap(channel -> channel.sendFiles(FileUpload.fromData(new Transcript(ticket).getTranscript())))
                .queue();

        EmbedBuilder builder = new EmbedBuilder().setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO)
                .setAuthor(event.getMember().getEffectiveName(), null, event.getMember().getEffectiveAvatarUrl())
                .setDescription("Sent transcript of Ticket #" + ticketID + " via DM");
        event.replyEmbeds(builder.build()).setEphemeral(true).queue();
    }
}