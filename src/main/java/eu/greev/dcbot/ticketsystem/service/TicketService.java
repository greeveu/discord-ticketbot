package eu.greev.dcbot.ticketsystem.service;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.logging.log4j.util.Strings;
import org.jdbi.v3.core.Jdbi;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TicketService {
    private final JDA jda;
    private final Jdbi jdbi;
    private final Guild guild;
    private final TicketData ticketData;
    private final Set<Ticket> allCurrentTickets = new HashSet<>();

    public TicketService(JDA jda, Jdbi jdbi, TicketData ticketData) {
        this.jdbi = jdbi;
        this.jda = jda;
        this.guild = jda.getGuildById(Constants.SERVER_ID);
        this.ticketData = ticketData;
    }

    public boolean createNewTicket(String info, String topic, User owner) {
        for (TextChannel textChannel : guild.getTextChannels()) {
            Ticket tckt = getTicketByChannelId(textChannel.getIdLong());
            if (tckt != null && tckt.getOwner().equals(owner)) {
                return false;
            }
        }
        Ticket ticket = Ticket.builder()
                .id(String.valueOf(ticketData.getLastTicketId() + 1)).ticketData(ticketData)
                .owner(owner)
                .topic(topic)
                .info(info)
                .build();

        TextChannel ticketChannel = guild.createTextChannel(generateChannelName(topic, ticketData.getLastTicketId() + 1), jda.getCategoryById(Constants.SUPPORT_CATEGORY))
                .addRolePermissionOverride(guild.getPublicRole().getIdLong(), null, List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY))
                .addRolePermissionOverride(Constants.TEAM_ID, List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY), null)
                .addMemberPermissionOverride(owner.getIdLong(), List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY), null)
                .setTopic(owner.getAsMention() + " | " + topic)
                .complete();

        jdbi.withHandle(handle -> handle.createUpdate("INSERT INTO tickets(ticketID, channelID, topic, info, owner) VALUES(?, ?, ?, ?, ?)")
                .bind(0, ticket.getId())
                .bind(1, ticketChannel.getId())
                .bind(2, topic)
                .bind(3, info)
                .bind(4, owner.getId())
                .execute());

        ticket.setChannel(ticketChannel);
        allCurrentTickets.add(ticket);

        EmbedBuilder builder = new EmbedBuilder().setColor(Constants.GREEV_GREEN)
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

        EmbedBuilder builder1 = new EmbedBuilder().setColor(Constants.GREEV_GREEN)
                .setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO)
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
        new Transcript(ticket)
                .addMessage(msgId);
        return true;
    }

    public void closeTicket(Ticket ticket, boolean wasAccident, Member closer) {
        Transcript transcript = new Transcript(ticket);
        allCurrentTickets.remove(ticket);
        if (wasAccident) {
            ticket.getChannel().delete().queue();
            jdbi.withHandle(handle -> handle.createUpdate("DELETE FROM tickets WHERE ticketID=?").bind(0, ticket.getId()).execute());

            transcript.getTranscript().delete();
        }else {
            jdbi.withHandle(handle -> handle.createUpdate("UPDATE tickets SET closer=? WHERE ticketID=?")
                    .bind(0, closer.getId())
                    .bind(1, ticket.getId())
                    .execute());
            String content = new SimpleDateFormat("[hh:mm:ss a '|' dd'th' MMM yyyy] ").format(new Date(System.currentTimeMillis()))
                    + "> [" + closer.getEffectiveName() + "#" + closer.getUser().getDiscriminator() + "] closed the ticket.";
            new Transcript(ticket).addMessage(content);
            EmbedBuilder builder = new EmbedBuilder().setTitle("Ticket " + ticket.getId())
                    .addField("Text Transcript⠀⠀⠀⠀⠀⠀⠀⠀", "See attachment", false)
                    .setColor(new Color(37, 150, 190))
                    .setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO);
            ticket.getOwner().openPrivateChannel()
                    .flatMap(channel -> channel.sendMessageEmbeds(builder.build()).setFiles(FileUpload.fromData(transcript.clean())))
                    .complete();
            ticket.getChannel().delete().queue();
        }
    }

    public boolean claim(Ticket ticket, User supporter) {
        if (supporter == ticket.getOwner()) {
            return false;
        }
        ticket.setSupporter(supporter);
        updateTopic(ticket);
        ticket.getChannel().getManager().setName("✓-" + ticket.getChannel().getName()).queue();
        EmbedBuilder builder = new EmbedBuilder().setColor(Constants.GREEV_GREEN)
                .setDescription("Hello there, " + ticket.getOwner().getAsMention() + "!" + """
                           A member of staff will assist you shortly.
                           In the mean time, please describe your issue in as much detail as possible! :)
                           """)
                .addField("Topic", ticket.getTopic(), false)
                .setAuthor(ticket.getOwner().getName(), null, ticket.getOwner().getEffectiveAvatarUrl())
                .setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO);
        if (!ticket.getInfo().equals(Strings.EMPTY)) { //TODO -> check again if the info gets set
            builder.addField("Information", ticket.getInfo(), false);
        }
        String content = new SimpleDateFormat("[hh:mm:ss a '|' dd'th' MMM yyyy] ").format(new Date(System.currentTimeMillis()))
                + "> [" + supporter.getName() + "#" + supporter.getDiscriminator() + "] claimed the ticket.";
        new Transcript(ticket).addMessage(content);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new Transcript(ticket).getTranscript()));
            ticket.getChannel().editMessageEmbedsById(reader.lines().toList().get(1), builder.build()).setActionRow(Button.danger("ticket-close", "Close")).queue();
            reader.close();
        } catch (IOException e) {
            log.error("Could not get Embed ID from transcript because", e);
        }
        return true;
    }

    public void toggleWaiting(Ticket ticket, boolean waiting) {
        String name = ticket.getChannel().getName();
        TextChannelManager manager = ticket.getChannel().getManager();
        if (waiting) {
            name = name.contains("✓") ? name.replace("✓", "\uD83D\uDD50") : "\uD83D\uDD50-" + name;
            manager.setName(name).queue();
        }else {
            if (ticket.getSupporter() != null) {
                manager.setName("✓-" + name.replace("\uD83D\uDD50", "").replace("✓", "")).queue();
            } else {
                manager.setName(name.replace("\uD83D\uDD50", "").replace("✓", "")).queue();
            }
        }
    }

    public boolean addUser(Ticket ticket, User user) {
        PermissionOverride permissionOverride = ticket.getChannel().getPermissionOverride(guild.getMember(user));
        if ((permissionOverride != null && permissionOverride.getAllowed().contains(Permission.VIEW_CHANNEL)) || guild.getMember(user).getPermissions().contains(Permission.ADMINISTRATOR)) {
            return false;
        }
        String content = new SimpleDateFormat("[hh:mm:ss a '|' dd'th' MMM yyyy] ").format(new Date(System.currentTimeMillis()))
                + "> [" + user.getName() + "#" + user.getDiscriminator() + "] got added to the ticket.";
        new Transcript(ticket).addMessage(content);
        ticket.getChannel().upsertPermissionOverride(guild.getMember(user)).setAllowed(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND).queue();
        ticket.addInvolved(user.getId());
        return true;
    }

    public boolean removeUser(Ticket ticket, User user) {
        PermissionOverride permissionOverride = ticket.getChannel().getPermissionOverride(guild.getMember(user));
        if (permissionOverride == null || !permissionOverride.getAllowed().contains(Permission.VIEW_CHANNEL)) {
            return false;
        }
        String content = new SimpleDateFormat("[hh:mm:ss a '|' dd'th' MMM yyyy] ").format(new Date(System.currentTimeMillis()))
                + "> [" + user.getName() + "#" + user.getDiscriminator() + "] got removed from the ticket.";
        new Transcript(ticket).addMessage(content);
        ticket.getChannel().upsertPermissionOverride(guild.getMember(user)).setDenied(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND).queue();
        ticket.removeInvolved(user.getId());
        return true;
    }

    public boolean setOwner(Ticket ticket, Member owner) {
        if (ticket.getChannel().getPermissionOverride(owner) == null) {
            return false;
        }
        if (!ticket.getChannel().getPermissionOverride(owner).getAllowed().contains(Permission.VIEW_CHANNEL)) {
            return false;
        }
        String content = new SimpleDateFormat("[hh:mm:ss a '|' dd'th' MMM yyyy] ").format(new Date(System.currentTimeMillis()))
                + "> [" + owner.getEffectiveName() + "#" + owner.getUser().getDiscriminator() + "] is the new ticket owner.";
        new Transcript(ticket).addMessage(content);
        ticket.setOwner(owner.getUser());
        updateTopic(ticket);
        return true;
    }

    public void setTopic(Ticket ticket, String topic) {
        String content = new SimpleDateFormat("[hh:mm:ss a '|' dd'th' MMM yyyy] ").format(new Date(System.currentTimeMillis()))
                + "> Set new topic to '" + topic + "'";
        new Transcript(ticket).addMessage(content);
        ticket.setTopic(topic);
        updateTopic(ticket);
    }

    public Ticket getTicketByChannelId(long idLong) {
        Optional<Ticket> optionalTicket = allCurrentTickets.stream()
                .filter(ticket -> ticket.getChannel().getIdLong() == idLong)
                .findAny();

        return optionalTicket.orElseGet(() -> {
            Ticket loadedTicket = ticketData.loadTicket(idLong);
            if (loadedTicket.getOwner() == null) return null;
            allCurrentTickets.add(loadedTicket);
            return loadedTicket;
        });
    }

    public Ticket getTicketByTicketId(String ticketID) {
        Optional<Ticket> optionalTicket = allCurrentTickets.stream()
                .filter(ticket -> ticket.getId().equals(ticketID))
                .findAny();

        return optionalTicket.orElseGet(() -> {
            Ticket loadedTicket = ticketData.loadTicket(ticketID);
            if (loadedTicket.getOwner() == null) return null;
            allCurrentTickets.add(loadedTicket);
            return loadedTicket;
        });
    }

    public List<String> getTicketIdsByOwner(User owner) {
        return ticketData.getTicketIdsByUser(owner);
    }

    private void updateTopic(Ticket ticket) {
        if (ticket.getSupporter() == null) {
            ticket.getChannel().getManager().setTopic(ticket.getOwner().getAsMention() + " | " + ticket.getTopic()).queue();
        }else {
            ticket.getChannel().getManager().setTopic(ticket.getOwner().getAsMention() + " | " + ticket.getTopic() + " | " + ticket.getSupporter().getAsMention()).queue();
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
        } else if (topic.contains(" wants to report ")) {
            name = "report-" + ticketId;
        } else {
            name = "ticket-" + ticketId;
        }
        return name;
    }
}