package eu.greev.dcbot.ticketsystem.service;

import eu.greev.dcbot.ticketsystem.Transcript;
import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.sql.DataSource;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TicketService {
    private final Ticket ticket;
    private final JDA jda;
    private final DataSource dataSource;
    private final Guild guild;
    private TextChannel ticketChannel;

    public TicketService(Ticket ticket, JDA jda, DataSource dataSource) {
        this.dataSource = dataSource;
        this.ticket = ticket;
        this.jda = jda;
        this.guild = jda.getGuildById(Constants.SERVER_ID);
    }

    public boolean createNewTicket(String info, String topic, User owner) {
        Guild guild = jda.getGuildById(Constants.SERVER_ID);
        for (TextChannel textChannel : guild.getTextChannels()) {
            if (textChannel.getName().contains("ticket-")) {
                if (TicketData.loadTicket(textChannel.getName().replaceAll("\uD83D\uDD50|✓|ticket|-", "")).getOwner().equals(owner)) return false;
            }
        }
        if (topic.equals("")) topic = "No topic given";
        ticket.setTopic(topic);
        ticket.setOwner(owner);

        try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement(
                "INSERT INTO tickets(ticketID) VALUES(?)"
        )) {
            statement.setString(1, ticket.getId());
            statement.execute();
        } catch (SQLException e) {
            log.error(ticket.getId() + ": Could not set ticketID", e);
        }

        guild.createTextChannel("ticket-" + (TicketData.getCurrentTickets().size() + 1), jda.getCategoryById(Constants.SUPPORT_CATEGORY))
                .addRolePermissionOverride(guild.getPublicRole().getIdLong(), null, List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY))
                .addRolePermissionOverride(Constants.TEAM_ID, List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY), null)
                .addMemberPermissionOverride(owner.getIdLong(), List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY), null)
                .setTopic(owner.getAsMention() + " | " + topic)
                .queue(success -> {
                    ticketChannel = guild.getTextChannelById(success.getIdLong());
                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setColor(new Color(63,226,69,255));
                    builder.setDescription("Hello there, " + owner.getAsMention() + "! " + """
                            A member of staff will assist you shortly.
                            In the mean time, please describe your issue in as much detail as possible! :)
                            """);
                    builder.addField("Topic", ticket.getTopic(), false);
                    builder.setAuthor(owner.getName(),null, owner.getEffectiveAvatarUrl());
                    builder.setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO);

                    success.sendMessage(owner.getAsMention() + " has created a new ticket").queue(s -> {
                        success.sendMessageEmbeds(builder.build())
                                .setActionRow(Button.primary("ticket-claim", "Claim"),
                                        Button.danger("ticket-close", "Close"))
                                .queue(q -> {
                                    EmbedBuilder builder1 = new EmbedBuilder();
                                    builder1.setColor(new Color(63,226,69,255));
                                    builder1.setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO);
                                    builder1.setDescription("If you opened this ticket accidentally, you have now the opportunity to close it again for 1 minute! Just click `Nevermind!` below\nThis message will delete itself after this minute");

                                    if (!info.equals("")) {
                                        EmbedBuilder infoBuilder = new EmbedBuilder();
                                        if (ticket.getSupporter() != null) {
                                            infoBuilder.setAuthor(ticket.getSupporter().getName(), null, ticket.getSupporter().getEffectiveAvatarUrl());
                                        }
                                        infoBuilder.setColor(new Color(63,226,69,255));
                                        infoBuilder.setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO);
                                        infoBuilder.setTitle("**Extra**");
                                        infoBuilder.addField("Given Information⠀⠀⠀⠀⠀⠀⠀⠀⠀", info, false);
                                        success.sendMessageEmbeds(infoBuilder.build()).submit();
                                    }

                                    success.sendMessageEmbeds(builder1.build())
                                            .setActionRow(Button.danger("ticket-nevermind", "Nevermind!"))
                                            .queue(suc -> {
                                                suc.delete().queueAfter(1, TimeUnit.MINUTES, msg -> {}, err -> {});
                                            });

                                    Transcript transcript = new Transcript(ticket);
                                    transcript.addMessage(q.getId());
                                });
                    });
                });
        return true;
    }

    public void closeTicket(boolean wasAccident) {
        EmbedBuilder builder = new EmbedBuilder();
        Transcript transcript = new Transcript(ticket);
        if (wasAccident) {
            ticketChannel.delete().queue();
            try (Connection conn = dataSource.getConnection(); PreparedStatement statement = conn.prepareStatement(
                    "DELETE FROM tickets WHERE ticketID=?"
            )) {
                statement.setString(1, ticket.getId());
                statement.execute();
            } catch (SQLException e) {
                log.error(ticket.getId() + ": Could not delete entry", e);
            }
            transcript.getTranscript().delete();
        }else {
            builder.setTitle("Ticket " + ticket.getId());
            builder.addField("Text Transcript⠀⠀⠀⠀⠀⠀⠀⠀", "See attachment", false);
            builder.setColor(new Color(37, 150, 190));
            builder.setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO);
            ticket.getOwner().openPrivateChannel()
                    .flatMap(channel -> channel.sendMessageEmbeds(builder.build()).setFiles(FileUpload.fromData(transcript.clean())))
                    .complete();
            ticketChannel.delete().queue();
        }
    }

    public boolean claim(User supporter) {
        if (supporter != ticket.getOwner()) {
            ticketChannel.getManager().setTopic(ticket.getOwner() + " | " + ticket.getTopic() + " | " + supporter.getAsMention()).setName("✓-ticket-" + ticket.getId()).queue();
            ticket.setSupporter(supporter);

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(new Color(63,226,69,255));
            builder.setDescription("Hello there, " + ticket.getOwner().getAsMention() + "!" + """
                            A member of staff will assist you shortly.
                            In the mean time, please describe your issue in as much detail as possible! :)
                            """);
            builder.addField("Topic", ticket.getTopic(), false);
            builder.setAuthor(ticket.getOwner().getName(), ticket.getOwner().getEffectiveAvatarUrl());
            builder.setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO);
            try {
                BufferedReader reader = new BufferedReader(new FileReader(new Transcript(ticket).getTranscript()));
                ticketChannel.editMessageEmbedsById(reader.lines().toList().get(0), builder.build()).setActionRow(Button.danger("ticket-close", "Close")).queue();
                reader.close();
            } catch (IOException e) {
                log.error("Could not get Embed ID from transcript because", e);
            }
            return true;
        }else {
            return false;
        }
    }

    public void toggleWaiting(boolean waiting) {
        if (waiting) {
            ticketChannel.getManager().setName("\uD83D\uDD50-ticket-" + ticket.getId()).queue();
        }else {
            ticketChannel.getManager().setName("✓-ticket-" + ticket.getId()).queue();
        }
    }

    public boolean addUser(User user) {
        PermissionOverride permissionOverride = ticketChannel.getPermissionOverride(guild.getMember(user));
        if ((permissionOverride != null && permissionOverride.getAllowed().contains(Permission.VIEW_CHANNEL)) || guild.getMember(user).getPermissions().contains(Permission.ADMINISTRATOR)) {
            return false;
        }else {
            ticketChannel.upsertPermissionOverride(guild.getMember(user)).setAllowed(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND).queue();
            ticket.addInvolved(user.getId());
            return true;
        }
    }

    public boolean removeUser(User user) {
        PermissionOverride permissionOverride = ticketChannel.getPermissionOverride(guild.getMember(user));
        if (permissionOverride != null && permissionOverride.getAllowed().contains(Permission.VIEW_CHANNEL)) {
            ticketChannel.upsertPermissionOverride(guild.getMember(user)).setDenied(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND).queue();
            ticket.removeInvolved(user.getId());
            return true;
        }else {
            return false;
        }
    }
}