package eu.greev.dcbot.ticketsystem.service;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
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

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@AllArgsConstructor
public class TicketService {
    private final JDA jda;
    private final Config config;
    private final Jdbi jdbi;
    private final TicketData ticketData;
    private final Set<Ticket> allCurrentTickets = new HashSet<>();

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
                .owner(owner)
                .topic(topic)
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
            if (member != null) thread.addThreadMember(member).queue();
        });
        return true;
    }

    public void closeTicket(Ticket ticket, boolean wasAccident, Member closer) {
        Transcript transcript = new Transcript(ticket);
        ticket.setCloser(closer.getUser());
        if (wasAccident) {
            ticket.getTextChannel().delete().queue();
            jdbi.withHandle(handle -> handle.createUpdate("DELETE FROM tickets WHERE ticketID=?").bind(0, ticket.getId()).execute());
            allCurrentTickets.remove(ticket);

            transcript.getTranscript().delete();
        } else {
            jdbi.withHandle(handle -> handle.createUpdate("UPDATE tickets SET closer=? WHERE ticketID=?")
                    .bind(0, closer.getId())
                    .bind(1, ticket.getId())
                    .execute());
            String content = new SimpleDateFormat("[hh:mm:ss a '|' dd'th' MMM yyyy] ").format(new Date(System.currentTimeMillis()))
                    + "> [" + closer.getEffectiveName() + "#" + closer.getUser().getDiscriminator() + "] closed the ticket.";
            transcript.addMessage(content);
            EmbedBuilder builder = new EmbedBuilder().setTitle("Ticket " + ticket.getId())
                    .addField("Text Transcript⠀⠀⠀⠀⠀⠀⠀⠀", "See attachment", false)
                    .setColor(Color.decode(config.getColor()))
                    .setFooter(config.getServerName(), config.getServerLogo());
            try {
                ticket.getOwner().openPrivateChannel()
                        .flatMap(channel -> channel.sendMessageEmbeds(builder.build()).setFiles(FileUpload.fromData(transcript.clean())))
                        .complete();
            } catch (ErrorResponseException e) {
                log.warn("Couldn't send [" + ticket.getOwner().getName() + "#" + ticket.getOwner().getDiscriminator() + "] their transcript since an error occurred:\nMeaning:"
                        + e.getMeaning() + " | Message:" + e.getMessage() + " | Response:" + e.getErrorResponse());
            }
            ticket.getTextChannel().delete().queue();
        }
    }

    public boolean claim(Ticket ticket, User supporter) {
        if (supporter == ticket.getOwner()) return false;

        ticket.setSupporter(supporter);
        updateChannelTopic(ticket);
        ticket.getTextChannel().getManager().setName("✓-" + ticket.getTextChannel().getName()).queue();
        EmbedBuilder builder = new EmbedBuilder().setColor(Color.decode(config.getColor()))
                .setDescription("Hello there, " + ticket.getOwner().getAsMention() + "! " + """
                           A member of staff will assist you shortly.
                           In the mean time, please describe your issue in as much detail as possible! :)
                           """)
                .addField("Topic", ticket.getTopic(), false)
                .setAuthor(ticket.getOwner().getName(), null, ticket.getOwner().getEffectiveAvatarUrl())
                .setFooter(config.getServerName(), config.getServerLogo());
        if (!ticket.getInfo().equals(Strings.EMPTY))
            builder.addField("Information", ticket.getInfo(), false);

        ticket.getThreadChannel().addThreadMember(supporter).queue();

        String content = new SimpleDateFormat("[hh:mm:ss a '|' dd'th' MMM yyyy] ").format(new Date(System.currentTimeMillis()))
                + "> [" + supporter.getName() + "#" + supporter.getDiscriminator() + "] claimed the ticket.";
        new Transcript(ticket).addMessage(content);
        ticket.getTextChannel().editMessageEmbedsById(ticket.getBaseMessage(), builder.build())
                .setActionRow(Button.danger("close", "Close"))
                .queue();
        return true;
    }

    public void toggleWaiting(Ticket ticket, boolean waiting) {
        String name = ticket.getTextChannel().getName();
        String waitingEmote = "\uD83D\uDD50";
        TextChannelManager manager = ticket.getTextChannel().getManager();
        if (waiting) {
            name = name.contains("✓") ? name.replace("✓", waitingEmote) : waitingEmote + name;
            manager.setName(name).queue();
        }else {
            if (ticket.getSupporter() != null) {
                manager.setName("✓-" + name.replace(waitingEmote, "").replace("✓", "")).queue();
            } else {
                manager.setName(name.replace(waitingEmote, "").replace("✓", "")).queue();
            }
        }
    }

    public boolean addUser(Ticket ticket, User user) {
        Guild guild = ticket.getTextChannel().getGuild();
        PermissionOverride permissionOverride = ticket.getTextChannel().getPermissionOverride(guild.getMember(user));
        if ((permissionOverride != null && permissionOverride.getAllowed().contains(Permission.VIEW_CHANNEL)) || guild.getMember(user).getPermissions().contains(Permission.ADMINISTRATOR)) {
            return false;
        }
        String content = new SimpleDateFormat("[hh:mm:ss a '|' dd'th' MMM yyyy] ").format(new Date(System.currentTimeMillis()))
                + "> [" + user.getName() + "#" + user.getDiscriminator() + "] got added to the ticket.";
        new Transcript(ticket).addMessage(content);
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
        String content = new SimpleDateFormat("[hh:mm:ss a '|' dd'th' MMM yyyy] ").format(new Date(System.currentTimeMillis()))
                + "> [" + user.getName() + "#" + user.getDiscriminator() + "] got removed from the ticket.";
        new Transcript(ticket).addMessage(content);
        ticket.getTextChannel().upsertPermissionOverride(guild.getMember(user)).setDenied(Permission.VIEW_CHANNEL).queue();
        ticket.removeInvolved(user.getId());
        return true;
    }

    public boolean setOwner(Ticket ticket, Member owner) {
        if (ticket.getTextChannel().getPermissionOverride(owner) == null) {
            return false;
        }
        if (!ticket.getTextChannel().getPermissionOverride(owner).getAllowed().contains(Permission.VIEW_CHANNEL)) {
            return false;
        }
        String content = new SimpleDateFormat("[hh:mm:ss a '|' dd'th' MMM yyyy] ").format(new Date(System.currentTimeMillis()))
                + "> [" + owner.getEffectiveName() + "#" + owner.getUser().getDiscriminator() + "] is the new ticket owner.";
        new Transcript(ticket).addMessage(content);
        ticket.setOwner(owner.getUser());
        updateChannelTopic(ticket);
        return true;
    }

    public void setTopic(Ticket ticket, String topic) {
        String content = new SimpleDateFormat("[hh:mm:ss a '|' dd'th' MMM yyyy] ").format(new Date(System.currentTimeMillis()))
                + "> Set new topic to '" + topic + "'";
        new Transcript(ticket).addMessage(content);
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
            if (loadedTicket.getOwner() == null) return null;
            allCurrentTickets.add(loadedTicket);
            return loadedTicket;
        });
    }

    public Ticket getTicketByTicketId(int ticketID) {
        Optional<Ticket> optionalTicket = allCurrentTickets.stream()
                .filter(ticket -> ticket.getId() == (ticketID))
                .findAny();

        return optionalTicket.orElseGet(() -> {
            Ticket loadedTicket = ticketData.loadTicket(ticketID);
            if (loadedTicket.getOwner() == null) return null;
            allCurrentTickets.add(loadedTicket);
            return loadedTicket;
        });
    }

    public List<Integer> getTicketIdsByOwner(User owner) {
        return ticketData.getTicketIdsByUser(owner);
    }

    public void updateChannelTopic(Ticket ticket) {
        if (ticket.getSupporter() == null) {
            ticket.getTextChannel().getManager().setTopic(ticket.getOwner().getAsMention() + " | " + ticket.getTopic()).queue();
        }else {
            ticket.getTextChannel().getManager().setTopic(ticket.getOwner().getAsMention() + " | " + ticket.getTopic() + " | " + ticket.getSupporter().getAsMention()).queue();
        }
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