package eu.greev.dcbot.ticketsystem;

import eu.greev.dcbot.Main;
import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class TicketListener extends ListenerAdapter {
    private Ticket ticket;
    private final TicketService ticketService;

    public TicketListener(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getButton().getId() == null) return;
        Main.INTERACTIONS.get(event.getButton().getId()).execute(event);
    }

    @Override
    public void onModalInteraction(@Nonnull ModalInteractionEvent event) {
        Main.INTERACTIONS.get(event.getModalId()).execute(event);
    }

    @Override
    public void onSelectMenuInteraction(@NotNull SelectMenuInteractionEvent event) {
        Main.INTERACTIONS.get(event.getSelectMenu().getId()).execute(event);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("ticket")) {
            Main.INTERACTIONS.get(event.getSubcommandName()).execute(event);
        }
    }

    /*
     *Listeners to handle the transcript
     */
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.isFromGuild() && event.getChannelType().equals(ChannelType.TEXT) && event.getChannel().getName().contains("ticket-") && !event.getAuthor().isBot()) {
            ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
            if (ticket.getChannel().getName().contains("\uD83D\uDD50")) {
                ticketService.toggleWaiting(ticket, false);
            }

            String content = event.getMessageId() + "} "
                    + new SimpleDateFormat("[hh:mm:ss a '|' dd'th' MMM yyyy] ").format(new Date(System.currentTimeMillis()))
                    + "[" + event.getMember().getEffectiveName() + "#" + event.getMember().getUser().getDiscriminator() + "]"
                    + ":>>> " + event.getMessage().getContentDisplay();

            Transcript transcript = new Transcript(ticket);
            transcript.addMessage(content);
        }
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        if (event.isFromGuild() && event.getChannelType().equals(ChannelType.TEXT) && event.getChannel().getName().contains("ticket-")) {
            ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
            Transcript transcript = new Transcript(ticket);
            transcript.deleteMessage(event.getMessageId());
        }
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        if (event.isFromGuild() && event.getChannelType().equals(ChannelType.TEXT) && event.getChannel().getName().contains("ticket-") && !event.getAuthor().isBot()) {
            ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
            Transcript transcript = new Transcript(ticket);
            transcript.editMessage(event.getMessageId(), event.getMessage().getContentDisplay());
        }
    }
}