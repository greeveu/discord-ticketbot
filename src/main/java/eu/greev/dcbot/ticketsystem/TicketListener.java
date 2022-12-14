package eu.greev.dcbot.ticketsystem;

import eu.greev.dcbot.Main;
import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.ticketsystem.service.Transcript;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateIconEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.exceptions.ContextException;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

@Slf4j
@AllArgsConstructor
public class TicketListener extends ListenerAdapter {
    private final TicketService ticketService;
    private final Config config;

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
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
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

    @Override
    public void onGuildUpdateIcon(GuildUpdateIconEvent event) {
        config.setServerLogo(event.getNewIconUrl());
        config.dumpConfig("./Tickets/config.yml");
        try {
            event.getGuild().getTextChannelById(config.getBaseChannel()).getIterableHistory()
                    .takeAsync(1000)
                    .get()
                    .forEach(m -> m.delete().complete());
            EmbedBuilder builder = new EmbedBuilder().setFooter(config.getServerName(), config.getServerLogo())
                    .setColor(Color.decode(config.getColor()))
                    .addField(new MessageEmbed.Field("**Support request**", """
                        You have questions or a problem?
                        Just click the one of the buttons below or use </ticket create:1030837558994804847> somewhere else.
                        We will try to handle your ticket as soon as possible.
                        """, false));

            StringSelectMenu.Builder selectionBuilder = StringSelectMenu.create("ticket-create-topic")
                    .setPlaceholder("Select your ticket topic")
                    .addOption("Report a bug","select-bug","Bugs can be annoying. Better call the exterminator.")
                    .addOption("Application", "select-application", "The place for Applications and Questions about it.")
                    .addOption( "Write a ban- or mute appeal","select-pardon","Got muted or banned for no reason?")
                    .addOption("Your own topic","select-custom","You have another reason for opening the ticket? Specify!");

            event.getGuild().getTextChannelById(config.getBaseChannel()).sendMessageEmbeds(builder.build())
                    .setActionRow(selectionBuilder.build())
                    .complete();
        } catch (InterruptedException | ExecutionException | ErrorResponseException e) {
            log.error("An error occurred while handling message history", e);
            Thread.currentThread().interrupt();
        }
    }

    private boolean isValid(GenericMessageEvent event) {
        return !event.isFromGuild() || !event.getChannelType().equals(ChannelType.TEXT) || ticketService.getTicketByChannelId(event.getChannel().getIdLong()) == null;
    }
}