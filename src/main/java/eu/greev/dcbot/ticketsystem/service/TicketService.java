package eu.greev.dcbot.ticketsystem.service;

import eu.greev.dcbot.ticketsystem.Transcript;
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
import net.dv8tion.jda.api.utils.FileUpload;
import org.jdbi.v3.core.Jdbi;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
        Ticket ticket = new Ticket((ticketData.getLastTicketId() + 1) + "", ticketData);
        Guild guild = jda.getGuildById(Constants.SERVER_ID);
        for (TextChannel textChannel : guild.getTextChannels()) {
            if (textChannel.getName().contains("ticket-")) {
                if (ticketData.loadTicket(textChannel.getName().replaceAll("\uD83D\uDD50|✓|ticket|-", "")).getOwner().equals(owner)) return false;
            }
        }
        if (topic.equals("")) topic = "No topic given";
        TextChannel ticketChannel = guild.createTextChannel("ticket-" + (ticketData.getLastTicketId() + 1), jda.getCategoryById(Constants.SUPPORT_CATEGORY))
                .addRolePermissionOverride(guild.getPublicRole().getIdLong(), null, List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY))
                .addRolePermissionOverride(Constants.TEAM_ID, List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY), null)
                .addMemberPermissionOverride(owner.getIdLong(), List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY), null)
                .setTopic(owner.getAsMention() + " | " + topic)
                .complete();

        ticket.setChannel(ticketChannel);
        ticket.setOwner(owner);
        ticket.setTopic(topic);
        allCurrentTickets.add(ticket);

        jdbi.withHandle(handle -> handle.createUpdate("INSERT INTO tickets(ticketID, channelID, owner) VALUES(?, ?, ?)")
                .bind(0, ticket.getId())
                .bind(1, ticketChannel.getId())
                .bind(2, owner.getId())
                .execute());

        EmbedBuilder builder = new EmbedBuilder();
        builder.setColor(new Color(63,226,69,255));
        builder.setDescription("Hello there, " + owner.getAsMention() + "! " + """
                            A member of staff will assist you shortly.
                            In the meantime, please describe your issue in as much detail as possible! :)
                            """);
        builder.addField("Topic", ticket.getTopic(), false);
        builder.setAuthor(owner.getName(),null, owner.getEffectiveAvatarUrl());
        ticketChannel.sendMessage(owner.getAsMention() + " has created a new ticket").complete();

        String msgId = ticketChannel.sendMessageEmbeds(builder.build())
                .setActionRow(Button.primary("ticket-claim", "Claim"),
                        Button.danger("ticket-close", "Close")).complete().getId();

        EmbedBuilder builder1 = new EmbedBuilder();
        builder1.setColor(new Color(63,226,69,255));
        builder1.setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO);
        builder1.setDescription("""
            If you opened this ticket accidentally, you have now the opportunity to close it again for 1 minute! Just click `Nevermind!` below.
            This message will delete itself after this minute.
            """);

        if (!info.equals("")) {
            EmbedBuilder infoBuilder = new EmbedBuilder();
            if (ticket.getSupporter() != null) {
                infoBuilder.setAuthor(ticket.getSupporter().getName(), null, ticket.getSupporter().getEffectiveAvatarUrl());
            }
            infoBuilder.setColor(new Color(63,226,69,255));
            infoBuilder.setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO);
            infoBuilder.setTitle("**Extra**");
            infoBuilder.addField("Given Information⠀⠀⠀⠀⠀⠀⠀⠀⠀", info, false);
            ticketChannel.sendMessageEmbeds(infoBuilder.build()).submit();
        }

        ticketChannel.sendMessageEmbeds(builder1.build())
                .setActionRow(Button.danger("ticket-nevermind", "Nevermind!"))
                .queue(suc -> suc.delete().queueAfter(1, TimeUnit.MINUTES, msg -> {}, err -> {}));
        Transcript transcript = new Transcript(ticket);
        transcript.addMessage(msgId);
        return true;
    }

    public void closeTicket(Ticket ticket, boolean wasAccident) {
        EmbedBuilder builder = new EmbedBuilder();
        Transcript transcript = new Transcript(ticket);
        allCurrentTickets.remove(ticket);
        if (wasAccident) {
            ticket.getChannel().delete().queue();
            jdbi.withHandle(handle -> handle.createUpdate("DELETE FROM tickets WHERE ticketID=?").bind(0, ticket.getId()).execute());

            transcript.getTranscript().delete();
        }else {
            builder.setTitle("Ticket " + ticket.getId());
            builder.addField("Text Transcript⠀⠀⠀⠀⠀⠀⠀⠀", "See attachment", false);
            builder.setColor(new Color(37, 150, 190));
            builder.setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO);
            ticket.getOwner().openPrivateChannel()
                    .flatMap(channel -> channel.sendMessageEmbeds(builder.build()).setFiles(FileUpload.fromData(transcript.clean())))
                    .complete();
            ticket.getChannel().delete().queue();
        }
    }

    public boolean claim(Ticket ticket, User supporter) {
        if (supporter != ticket.getOwner()) {
            allCurrentTickets.remove(ticket);
            ticket.setSupporter(supporter);
            allCurrentTickets.add(ticket);
            updateTopic(ticket);
            ticket.getChannel().getManager().setName("✓-ticket-" + ticket.getId()).queue();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(new Color(63,226,69,255));
            builder.setDescription("Hello there, " + ticket.getOwner().getAsMention() + "!" + """
                            A member of staff will assist you shortly.
                            In the mean time, please describe your issue in as much detail as possible! :)
                            """);
            builder.addField("Topic", ticket.getTopic(), false);
            builder.setAuthor(ticket.getOwner().getName(), null, ticket.getOwner().getEffectiveAvatarUrl());
            builder.setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO);
            try {
                BufferedReader reader = new BufferedReader(new FileReader(new Transcript(ticket).getTranscript()));
                ticket.getChannel().editMessageEmbedsById(reader.lines().toList().get(1), builder.build()).setActionRow(Button.danger("ticket-close", "Close")).queue();
                reader.close();
            } catch (IOException e) {
                log.error("Could not get Embed ID from transcript because", e);
            }
            return true;
        }else {
            return false;
        }
    }

    public void toggleWaiting(Ticket ticket, boolean waiting) {
        if (waiting) {
            ticket.getChannel().getManager().setName("\uD83D\uDD50-ticket-" + ticket.getId()).queue();
        }else {
            ticket.getChannel().getManager().setName("✓-ticket-" + ticket.getId()).queue();
        }
    }

    public boolean addUser(Ticket ticket, User user) {
        PermissionOverride permissionOverride = ticket.getChannel().getPermissionOverride(guild.getMember(user));
        if ((permissionOverride != null && permissionOverride.getAllowed().contains(Permission.VIEW_CHANNEL)) || guild.getMember(user).getPermissions().contains(Permission.ADMINISTRATOR)) {
            return false;
        }else {
            allCurrentTickets.remove(ticket);
            ticket.getChannel().upsertPermissionOverride(guild.getMember(user)).setAllowed(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND).queue();
            ticket.addInvolved(user.getId());
            allCurrentTickets.add(ticket);
            return true;
        }
    }

    public boolean removeUser(Ticket ticket, User user) {
        PermissionOverride permissionOverride = ticket.getChannel().getPermissionOverride(guild.getMember(user));
        if (permissionOverride != null && permissionOverride.getAllowed().contains(Permission.VIEW_CHANNEL)) {
            allCurrentTickets.remove(ticket);
            ticket.getChannel().upsertPermissionOverride(guild.getMember(user)).setDenied(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND).queue();
            ticket.removeInvolved(user.getId());
            allCurrentTickets.add(ticket);
            return true;
        }else {
            return false;
        }
    }

    public boolean setOwner(Ticket ticket, Member owner) {
        if (ticket.getChannel().getPermissionOverride(owner) == null) return false;
        if (ticket.getChannel().getPermissionOverride(owner).getAllowed().contains(Permission.VIEW_CHANNEL)) {
            allCurrentTickets.remove(ticket);
            ticket.setOwner(owner.getUser());
            updateTopic(ticket);
            allCurrentTickets.add(ticket);
            return true;
        }else {
            return false;
        }
    }

    public void setTopic(Ticket ticket, String topic) {
        allCurrentTickets.remove(ticket);
        ticket.setTopic(topic);
        updateTopic(ticket);
        allCurrentTickets.add(ticket);
    }

    public Ticket getTicketByChannelId(long idLong) {
        Optional<Ticket> optionalTicket = allCurrentTickets.stream()
                .filter(ticket -> ticket.getChannel().getIdLong() == idLong)
                .findAny();

        return optionalTicket.orElseGet(() -> {
            Ticket loadedTicket = ticketData.loadTicket(idLong);
            allCurrentTickets.add(loadedTicket);
            return loadedTicket;
        });
    }

    public Ticket getTicketByTicketId(String ticketID) {
        Optional<Ticket> optionalTicket = allCurrentTickets.stream()
                .filter(ticket -> ticket.getId().equals(ticketID))
                .findAny();

        return optionalTicket.orElseGet(() -> {
            System.out.println("db");
            Ticket loadedTicket = ticketData.loadTicket(ticketID);
            allCurrentTickets.add(loadedTicket);
            return loadedTicket;
        });
    }

    private void updateTopic(Ticket ticket) {
        if (ticket.getSupporter() == null) {
            ticket.getChannel().getManager().setTopic(ticket.getOwner().getAsMention() + " | " + ticket.getTopic()).queue();
        }else {
            ticket.getChannel().getManager().setTopic(ticket.getOwner().getAsMention() + " | " + ticket.getTopic() + " | " + ticket.getSupporter().getAsMention()).queue();
        }
    }
}