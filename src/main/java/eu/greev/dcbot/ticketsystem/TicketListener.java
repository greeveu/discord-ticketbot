package eu.greev.dcbot.ticketsystem;

import eu.greev.dcbot.Main;
import eu.greev.dcbot.ticketsystem.categories.ICategory;
import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateArchivedEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateIconEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.util.concurrent.ExecutionException;

@Slf4j
@AllArgsConstructor
public class TicketListener extends ListenerAdapter {
    private final TicketService ticketService;
    private final Config config;

    @Override
    public void onChannelUpdateArchived(ChannelUpdateArchivedEvent event) {
        if (ticketService.getTicketByChannelId(event.getChannel().asThreadChannel().getParentMessageChannel().getIdLong()) == null
                || Boolean.FALSE.equals(event.getNewValue()) || !(event.getChannel() instanceof ThreadChannel channel)) {
            return;
        }
        channel.getManager().setArchived(false).queue();
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        for (Ticket ticket : ticketService.getOpenTickets(event.getUser())) {
            ticket.getTranscript().addLogMessage(ticket.getOwner().getName() + " has left the server.", Instant.now().getEpochSecond(), ticket.getId());
            EmbedBuilder info = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setFooter(config.getServerName(), config.getServerLogo())
                    .addField("ℹ️ **Member left**", ticket.getOwner().getAsMention() + " has left the server.", false);
            MessageCreateBuilder messageBuilder = new MessageCreateBuilder()
                    .addEmbeds(info.build());
            if (ticket.getSupporter() != null) {
                messageBuilder.addContent(ticket.getSupporter().getAsMention());
            }
            ticket.getTextChannel().sendMessage(messageBuilder.build()).queue();
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        for (Ticket ticket : ticketService.getOpenTickets(event.getUser())) {
            ticket.getTextChannel().upsertPermissionOverride(event.getMember()).setAllowed(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND).queue();
            ticket.getTranscript().addLogMessage(ticket.getOwner().getName() + " has rejoined the server.", Instant.now().getEpochSecond(), ticket.getId());

            EmbedBuilder info = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setFooter(config.getServerName(), config.getServerLogo())
                    .addField("ℹ️ **Member rejoined**", ticket.getOwner().getAsMention() + " has rejoined the server and was granted access to that ticket again.", false);
            MessageCreateBuilder messageBuilder = new MessageCreateBuilder()
                    .addEmbeds(info.build());
            if (ticket.getSupporter() != null) {
                messageBuilder.addContent(ticket.getSupporter().getAsMention());
            }
            ticket.getTextChannel().sendMessage(messageBuilder.build()).queue();
        }
    }

    @Override
    public void onChannelDelete(ChannelDeleteEvent event) {
        Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
        if (ticket == null) {
            return;
        }
        ticket.setOpen(false);
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
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (event.getSelectMenu().getId() == null || !event.getSelectMenu().getId().equals("ticket-create-topic")) return;
        Main.INTERACTIONS.get(event.getSelectedOptions().get(0).getValue()).execute(event);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("ticket") || !isValidSlashEvent(event)) return;
        Main.INTERACTIONS.get((event.getSubcommandGroup() == null ? "" : event.getSubcommandGroup() + " ") + event.getSubcommandName()).execute(event);
    }

    /*
     *Listeners to handle the transcript
     */
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getChannelType() == ChannelType.GUILD_PRIVATE_THREAD && event.isFromGuild()
                && ticketService.getTicketByChannelId(event.getGuildChannel().asThreadChannel().getParentMessageChannel().getIdLong()) != null) {

            for (Member member : event.getMessage().getMentions().getMembers()) {
                if (member.getRoles().stream().map(Role::getIdLong).toList().contains(config.getStaffId())) continue;
                event.getGuildChannel().asThreadChannel().removeThreadMember(member).queue();

                User author = event.getAuthor();
                event.getChannel().sendMessageEmbeds(new EmbedBuilder().setColor(Color.RED)
                        .addField("❌ **Failed**", author.getAsMention() + " messed up and pinged " + member.getAsMention(), false)
                        .setAuthor(author.getName(), null, author.getEffectiveAvatarUrl())
                        .build()).queue();
                break;
            }
            return;
        }

        if (isValid(event) || event.getAuthor().isBot()) return;

        Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
        if (ticket.isWaiting()) {
            ticketService.toggleWaiting(ticket, false);
            ticket.setWaitingSince(null);
            ticket.setRemindersSent(0);
        }

        ticket.setSupporterRemindersSent(0);

        ticket.getTranscript().addMessage(event.getMessage(), ticket.getId());
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        if (!event.isFromGuild() || !event.getChannelType().equals(ChannelType.TEXT) || ticketService.getTicketByChannelId(event.getChannel().getIdLong()) == null) return;
        Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
        if (event.getMessageId().equals(ticket.getBaseMessage())) return;

        ticket.getTranscript().deleteMessage(event.getMessageIdLong());
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        if (isValid(event) || event.getAuthor().isBot()) return;

        Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
        ticket.getTranscript().editMessage(event.getMessageIdLong(), event.getMessage().getContentDisplay(), event.getMessage().getTimeEdited().toInstant().getEpochSecond());
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
                        Just click the one of the buttons below.
                        We will try to handle your ticket as soon as possible.
                        """, false));

            StringSelectMenu.Builder selectionBuilder = StringSelectMenu.create("ticket-create-topic")
                    .setPlaceholder("Select your ticket topic");

            for (ICategory category : Main.CATEGORIES) {
                selectionBuilder.addOption(category.getLabel(), "select-" + category.getId(), category.getDescription());
            }

            event.getGuild().getTextChannelById(config.getBaseChannel()).sendMessageEmbeds(builder.build())
                    .setActionRow(selectionBuilder.build())
                    .complete();
        } catch (InterruptedException | ExecutionException | ErrorResponseException e) {
            log.error("An error occurred while handling message history", e);
            Thread.currentThread().interrupt();
        }
    }

    private boolean isValidSlashEvent(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.replyEmbeds(new EmbedBuilder()
                            .setColor(Color.RED)
                            .setDescription("You have to use this command in a guild!")
                            .build())
                    .setEphemeral(true)
                    .queue();
            return false;
        }
        if (config.getServerName() == null && !event.getSubcommandName().equals("setup")) {
            EmbedBuilder error = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setDescription("❌ **Ticketsystem wasn't setup, please tell an Admin to use </ticket setup:0>!**");
            event.replyEmbeds(error.build()).setEphemeral(true).queue();
            return false;
        }
        return true;
    }

    private boolean isValid(GenericMessageEvent event) {
        return !event.isFromGuild() || !event.getChannelType().equals(ChannelType.TEXT) || ticketService.getTicketByChannelId(event.getChannel().getIdLong()) == null;
    }
}