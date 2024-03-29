package eu.greev.dcbot.ticketsystem.service;

import eu.greev.dcbot.ticketsystem.entities.Edit;
import eu.greev.dcbot.ticketsystem.entities.Message;
import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.entities.TranscriptEntity;
import eu.greev.dcbot.utils.Config;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.logging.log4j.util.Strings;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TicketService {
    private final JDA jda;
    private final Config config;
    private final Jdbi jdbi;
    private final TicketData ticketData;
    private final Set<Ticket> allCurrentTickets = new HashSet<>();
    public static final String WAITING_EMOTE = "\uD83D\uDD50";

    public TicketService(JDA jda, Config config, Jdbi jdbi, TicketData ticketData) {
        this.jda = jda;
        this.config = config;
        this.jdbi = jdbi;
        this.ticketData = ticketData;

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                getOpenCachedTickets().stream()
                        .map(Ticket::getTranscript)
                        .map(Transcript::getRecentChanges)
                        .filter(changes -> !changes.isEmpty())
                        .forEach(TicketService.this::saveTranscriptChanges);
            }
        }, 0, TimeUnit.MINUTES.toMillis(3));
    }

    public boolean createNewTicket(String info, String topic, User owner) {
        Guild guild = jda.getGuildById(config.getServerId());
        for (TextChannel textChannel : guild.getTextChannels()) {
            Ticket tckt = getTicketByChannelId(textChannel.getIdLong());
            if (tckt != null && tckt.getOwner().equals(owner)) {
                return false;
            }
        }
        Ticket ticket = Ticket.builder()
                .id(ticketData.getLastTicketId() + 1)
                .ticketData(ticketData)
                .transcript(new Transcript(new ArrayList<>()))
                .owner(owner)
                .topic(topic)
                .isOpen(true)
                .info(info)
                .build();

        TextChannel ticketChannel = guild.createTextChannel(generateChannelName(topic, ticketData.getLastTicketId() + 1), jda.getCategoryById(config.getSupportCategory()))
                .addRolePermissionOverride(guild.getPublicRole().getIdLong(), null, List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY))
                .addRolePermissionOverride(config.getStaffId(), List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY), null)
                .addMemberPermissionOverride(owner.getIdLong(), List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY), null)
                .setTopic(owner.getAsMention() + " | " + topic)
                .complete();
        ThreadChannel thread = ticketChannel.createThreadChannel("Discussion-" + ticket.getId(), true).complete();

        jdbi.withHandle(handle -> handle.createUpdate("INSERT INTO tickets(ticketID, channelID, threadID, topic, info, owner) VALUES(?, ?, ?, ?, ?, ?)")
                .bind(0, ticket.getId())
                .bind(1, ticketChannel.getId())
                .bind(2, thread.getId())
                .bind(3, topic)
                .bind(4, info)
                .bind(5, owner.getId())
                .execute());

        EmbedBuilder builder = new EmbedBuilder().setColor(Color.decode(config.getColor()))
                .setDescription("Hello there, " + owner.getAsMention() + "! " + """
                            A member of staff will assist you shortly.
                            In the meantime, please describe your issue in as much detail as possible! :)
                            """)
                .addField("Topic", ticket.getTopic(), false)
                .setAuthor(owner.getName(),null, owner.getEffectiveAvatarUrl());

        if (!info.equals(Strings.EMPTY)) {
            builder.addField("Information", info, false);
        }
        ticketChannel.sendMessage(owner.getAsMention() + " has created a new ticket").complete();

        String msgId = ticketChannel.sendMessageEmbeds(builder.build())
                .setActionRow(Button.primary("claim", "Claim"),
                        Button.danger("close", "Close")).complete().getId();
        ticket.setTextChannel(ticketChannel)
                .setThreadChannel(thread)
                .setBaseMessage(msgId);
        allCurrentTickets.add(ticket);

        EmbedBuilder builder1 = new EmbedBuilder().setColor(Color.decode(config.getColor()))
                .setFooter(config.getServerName(), config.getServerLogo())
                .setDescription("""
                    If you opened this ticket accidentally, you have now the opportunity to close it again for 1 minute! Just click `Nevermind!` below.
                    This message will delete itself after this minute.
                    """);

        ticketChannel.sendMessageEmbeds(builder1.build())
                .setActionRow(Button.danger("nevermind", "Nevermind!"))
                .queue(suc -> {
                    suc.delete().queueAfter(1, TimeUnit.MINUTES, msg -> {}, err -> {});
                    ticket.setTempMsgId(suc.getId());
                });

        config.getAddToTicketThread().forEach(id -> {
            Role role = guild.getRoleById(id);
            if (role != null) {
                guild.findMembersWithRoles(role).onSuccess(list -> list.forEach(member -> thread.addThreadMember(member).queue()));
                return;
            }
            Member member = guild.retrieveMemberById(id).complete();
            if (member != null) {
                thread.addThreadMember(member).queue();
            }
        });
        return true;
    }

    public void closeTicket(Ticket ticket, boolean wasAccident, Member closer) {
        Transcript transcript = ticket.getTranscript();
        int ticketId = ticket.getId();
        ticket.setCloser(closer.getUser()).setOpen(false);
        if (wasAccident) {
            ticket.getTextChannel().delete().queue();
            jdbi.withHandle(handle -> handle.createUpdate("DELETE FROM tickets WHERE ticketID=?").bind(0, ticketId).execute());
            allCurrentTickets.remove(ticket);

            ticketData.getTranscriptData().deleteTranscript(ticket);
            return;
        }

        jdbi.withHandle(handle -> handle.createUpdate("UPDATE tickets SET closer=? WHERE ticketID=?")
                .bind(0, closer.getId())
                .bind(1, ticketId)
                .execute());

        transcript.addLogMessage("[" + closer.getUser().getName() + "] closed the ticket.", Instant.now().getEpochSecond(), ticketId);

        EmbedBuilder builder = new EmbedBuilder().setTitle("Ticket " + ticketId)
                .addField("Text Transcript⠀⠀⠀⠀⠀⠀⠀⠀", "See attachment", false)
                .setColor(Color.decode(config.getColor()))
                .setFooter(config.getServerName(), config.getServerLogo());

        if (ticket.getOwner().getMutualGuilds().contains(jda.getGuildById(config.getServerId()))) {
            try {
                ticket.getOwner().openPrivateChannel()
                        .flatMap(channel -> channel.sendMessageEmbeds(builder.build()).setFiles(FileUpload.fromData(transcript.toFile(ticketId))))
                        .complete();
            } catch (ErrorResponseException e) {
                log.warn("Couldn't send [" + ticket.getOwner().getName() + "] their transcript since an error occurred:\nMeaning:"
                        + e.getMeaning() + " | Message:" + e.getMessage() + " | Response:" + e.getErrorResponse());
            }
        }
        saveTranscriptChanges(ticket.getTranscript().getRecentChanges());
        ticket.getTextChannel().delete().queue();
    }

    public boolean claim(Ticket ticket, User supporter) {
        if (supporter == ticket.getOwner()) return false;

        ticket.setSupporter(supporter);
        updateChannelTopic(ticket);
        ticket.getTextChannel().getManager().setName("✓-" + generateChannelName(ticket.getTopic(), ticket.getId())).queue();
        EmbedBuilder builder = new EmbedBuilder().setColor(Color.decode(config.getColor()))
                .setDescription("Hello there, " + ticket.getOwner().getAsMention() + "! " + """
                           A member of staff will assist you shortly.
                           In the mean time, please describe your issue in as much detail as possible! :)
                           """)
                .addField("Topic", ticket.getTopic(), false)
                .setAuthor(ticket.getOwner().getName(), null, ticket.getOwner().getEffectiveAvatarUrl())
                .setFooter(config.getServerName(), config.getServerLogo());
        if (!ticket.getInfo().equals(Strings.EMPTY)) {
            builder.addField("Information", ticket.getInfo(), false);
        }

        ticket.getThreadChannel().addThreadMember(supporter).queue();

        ticket.getTranscript().addLogMessage("[" + supporter.getName() + "] claimed the ticket.", Instant.now().getEpochSecond(), ticket.getId());
        ticket.getTextChannel().editMessageEmbedsById(ticket.getBaseMessage(), builder.build())
                .setActionRow(Button.danger("close", "Close"))
                .queue();
        return true;
    }

    public void toggleWaiting(Ticket ticket, boolean waiting) {
        TextChannelManager manager = ticket.getTextChannel().getManager();
        String channelName = generateChannelName(ticket.getTopic(), ticket.getId());
        ticket.setWaiting(waiting);
        if (waiting) {
            manager.setName(WAITING_EMOTE + "-" + channelName).queue();
        } else {
            if (ticket.getSupporter() == null) {
                manager.setName(channelName).queue();
                return;
            }
            manager.setName("✓-" + channelName).queue();
        }
    }

    public boolean addUser(Ticket ticket, User user) {
        Guild guild = ticket.getTextChannel().getGuild();
        PermissionOverride permissionOverride = ticket.getTextChannel().getPermissionOverride(guild.getMember(user));
        if ((permissionOverride != null && permissionOverride.getAllowed().contains(Permission.VIEW_CHANNEL))
                || guild.getMember(user).getPermissions().contains(Permission.ADMINISTRATOR)) {
            return false;
        }

        ticket.getTranscript().addLogMessage("[" + user.getName() + "] got added to the ticket.", Instant.now().getEpochSecond(), ticket.getId());

        ticket.getTextChannel().upsertPermissionOverride(guild.getMember(user)).setAllowed(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND).queue();
        ticket.addInvolved(user.getId());
        return true;
    }

    public boolean removeUser(Ticket ticket, User user) {
        Guild guild = ticket.getTextChannel().getGuild();
        PermissionOverride permissionOverride = ticket.getTextChannel().getPermissionOverride(guild.getMember(user));
        if (permissionOverride == null || !permissionOverride.getAllowed().contains(Permission.VIEW_CHANNEL)) {
            return false;
        }
        ticket.getTranscript().addLogMessage("[" + user.getName() + "] got removed from the ticket.", Instant.now().getEpochSecond(), ticket.getId());
        ticket.getTextChannel().upsertPermissionOverride(guild.getMember(user)).setDenied(Permission.VIEW_CHANNEL).queue();
        ticket.removeInvolved(user.getId());
        return true;
    }

    public boolean setOwner(Ticket ticket, Member owner) {
        if (ticket.getTextChannel().getPermissionOverride(owner) == null
                || !ticket.getTextChannel().getPermissionOverride(owner).getAllowed().contains(Permission.VIEW_CHANNEL)) {
            return false;
        }

        ticket.getTranscript().addLogMessage("[" + owner.getUser().getName() + "] is the new ticket owner.", Instant.now().getEpochSecond(), ticket.getId());
        ticket.setOwner(owner.getUser());
        updateChannelTopic(ticket);
        return true;
    }

    public void setTopic(Ticket ticket, String topic) {
        ticket.getTranscript().addLogMessage("Set new topic to '" + topic + "'", Instant.now().getEpochSecond(), ticket.getId());

        ticket.setTopic(topic);
        updateChannelTopic(ticket);
    }

    public Ticket getTicketByChannelId(long idLong) {
        Optional<Ticket> optionalTicket = allCurrentTickets.stream()
                .filter(ticket -> ticket.getTextChannel() != null)
                .filter(ticket -> ticket.getTextChannel().getIdLong() == idLong)
                .findAny();

        return optionalTicket.orElseGet(() -> {
            Ticket loadedTicket = ticketData.loadTicket(idLong);
            if (loadedTicket != null) {
                allCurrentTickets.add(loadedTicket);
            }
            return loadedTicket;
        });
    }

    public Ticket getTicketByTicketId(int ticketID) {
        Optional<Ticket> optionalTicket = allCurrentTickets.stream()
                .filter(ticket -> ticket.getId() == (ticketID))
                .findAny();

        return optionalTicket.orElseGet(() -> {
            Ticket loadedTicket = ticketData.loadTicket(ticketID);
            if (loadedTicket != null) {
                allCurrentTickets.add(loadedTicket);
            }
            return loadedTicket;
        });
    }

    public List<Ticket> getOpenCachedTickets() {
        return allCurrentTickets.stream().filter(Ticket::isOpen).toList();
    }

    public List<Integer> getTicketIdsByOwner(long owner) {
        return ticketData.getTicketIdsByUser(String.valueOf(owner));
    }

    public @Nullable Ticket getOpenTicket(User owner) {
        Integer ticketId = ticketData.getOpenTicketOfUser(owner.getId());
        if (ticketId == null) {
            return null;
        }
        return this.getTicketByTicketId(ticketId);
    }

    public void updateChannelTopic(Ticket ticket) {
        TextChannelManager channelManager = ticket.getTextChannel().getManager();
        if (ticket.getSupporter() == null) {
            channelManager.setTopic(ticket.getOwner().getAsMention() + " | " + ticket.getTopic()).queue();
        } else {
            channelManager.setTopic(ticket.getOwner().getAsMention() + " | " + ticket.getTopic() + " | " + ticket.getSupporter().getAsMention()).queue();
        }
    }

    private void saveTranscriptChanges(List<TranscriptEntity> changes) {
        TranscriptData transcriptData = ticketData.getTranscriptData();
        for (TranscriptEntity entity : changes) {
            if (entity instanceof Edit edit) {
                transcriptData.addEditToMessage(edit);
                continue;
            }
            Message message = (Message) entity;

            if (message.getId() == 0 && message.getAuthor().equals(Strings.EMPTY)) {
                transcriptData.addLogMessage(message);
                continue;
            }

            if (message.isDeleted()) {
                transcriptData.deleteMessage(message.getId());
            } else {
                transcriptData.addNewMessage(message);
            }
        }
        changes.clear();
    }

    private String generateChannelName(String topic, int ticketId) {
        String name;
        if (topic.equals("Bugreport")) {
            name = "bugreport-" + ticketId;
        } else if (topic.equals("Complain")) {
            name = "complain-" + ticketId;
        }  else if (topic.contains(" wants pardon ")) {
            name = "pardon-" + ticketId;
        } else if (topic.contains(" apply ")) {
            name = "application-" + ticketId;
        } else {
            name = "ticket-" + ticketId;
        }
        return name;
    }
}