package eu.greev.dcbot.ticketsystem;

import eu.greev.dcbot.Main;
import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.ticketsystem.service.Transcript;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class TicketListener extends ListenerAdapter {
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
    public void onModalInteraction(ModalInteractionEvent event) {
        Main.INTERACTIONS.get(event.getModalId()).execute(event);
    }

    @Override
    public void onSelectMenuInteraction(@NotNull SelectMenuInteractionEvent event) {
        if (event.getSelectMenu().getId() == null || !event.getSelectMenu().getId().equals("ticket-create-topic")) return;
        Main.INTERACTIONS.get(event.getSelectedOptions().get(0).getValue()).execute(event);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("ticket")) return;
        Main.INTERACTIONS.get(event.getSubcommandName()).execute(event);
    }

    /*
     *Listeners to handle the transcript
     */
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (isValid(event) || event.getAuthor().isBot()) return;

        Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
        if (ticket.getChannel().getName().contains("\uD83D\uDD50")) {
            ticketService.toggleWaiting(ticket, false);
        }
        String content = event.getMessageId() + "} "
                + new SimpleDateFormat("[hh:mm:ss a '|' dd'th' MMM yyyy] ").format(new Date(System.currentTimeMillis()))
                + "[" + event.getMember().getEffectiveName() + "#" + event.getMember().getUser().getDiscriminator() + "]"
                + ":>>> " + event.getMessage().getContentDisplay();
        new Transcript(ticket)
                .addMessage(content);
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        if (!event.isFromGuild() || !event.getChannelType().equals(ChannelType.TEXT) || ticketService.getTicketByChannelId(event.getChannel().getIdLong()) == null) return;
        Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
        if (event.getMessageId().equals(ticket.getTempMsgId())) return;

        new Transcript(ticket).deleteMessage(event.getMessageId());
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        if (isValid(event) || event.getAuthor().isBot()) return;
        new Transcript(ticketService.getTicketByChannelId(event.getChannel().getIdLong()))
                .editMessage(event.getMessageId(), event.getMessage().getContentDisplay());
    }

    private boolean isValid(GenericMessageEvent event) {
        return !event.isFromGuild() || !event.getChannelType().equals(ChannelType.TEXT) || ticketService.getTicketByChannelId(event.getChannel().getIdLong()) == null;
    }
}