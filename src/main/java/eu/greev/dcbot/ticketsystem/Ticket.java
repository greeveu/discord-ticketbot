package eu.greev.dcbot.ticketsystem;

import eu.greev.dcbot.data.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.sql.DataSource;
import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Ticket {
    private User owner;
    private User supporter;
    private File transcript;
    private TextChannel ticketChannel;
    private long ticketChannelID;
    private String id;
    private String topic = "No topic given";
    private final TicketData ticketData;
    private final JDA jda;
    private final DataSource dataSource;
    private final String serverID = new Data().serverID;
    private final long staffID = new Data().teamID;

    protected Ticket(User owner, JDA jda, DataSource dataSource) {
        this.dataSource = dataSource;
        this.owner = owner;
        this.jda = jda;
        id = "";
        ticketData = new TicketData(dataSource);
    }

    protected Ticket(long ticketChannelId, JDA jda, DataSource dataSource) {
        this.dataSource = dataSource;
        this.jda = jda;
        ticketChannel = jda.getTextChannelById(ticketChannelId);
        this.ticketChannelID = ticketChannelId;
        id = ticketChannel.getName().replaceAll("\uD83D\uDD50|✓|ticket|-", "");
        ticketData = new TicketData(id, dataSource);
        owner = jda.getUserById(ticketData.getOwner());
        transcript = new File("./GreevTickets/transcripts/" + getID() + ".txt");
    }

    protected boolean createNewTicket(String info, String topic) {
        Member owner = jda.getGuildById(serverID).getMember(this.owner);
        if (topic.equals("")) topic = "No topic given";
        this.topic = topic;

        for (TextChannel textChannel : jda.getGuildById(serverID).getTextChannels()) {
            if (textChannel.getName().contains("ticket-")) {
                if (new TicketData(textChannel.getName().replaceAll("\uD83D\uDD50|✓|ticket|-", ""), dataSource).getOwner().equals(owner.getId())) return false;
            }
        }

        jda.getGuildById(serverID).createTextChannel("ticket-" + (ticketData.getCurrentTickets().size() + 1), jda.getCategoryById(new Data().supportCategory))
                .addRolePermissionOverride(jda.getGuildById(serverID).getPublicRole().getIdLong(), null, List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY))
                .addRolePermissionOverride(staffID, List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY), null)
                .addMemberPermissionOverride(owner.getIdLong(), List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY), null)
                .setTopic(owner.getAsMention() + " | " + topic)
                .queue(success -> {
                    ticketChannelID = success.getIdLong();
                    ticketChannel = jda.getGuildById(serverID).getTextChannelById(ticketChannelID);

                    id = !new TicketData(dataSource).getCurrentTickets().isEmpty() ? (new TicketData(dataSource).getCurrentTickets().size() + 1) + "" : "1";
                    TicketData ticketData = new TicketData(id, dataSource);
                    ticketData.setOwner(owner.getId());

                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setColor(new Color(63,226,69,255));
                    builder.setDescription("Hello there, " + owner.getAsMention() + "! " + """
                            A member of staff will assist you shortly.
                            In the mean time, please describe your issue in as much detail as possible! :)
                            """);
                    builder.addField("Topic", this.topic, false);
                    builder.setAuthor(owner.getEffectiveName(),null, owner.getEffectiveAvatarUrl());
                    builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                    success.sendMessage(owner.getAsMention() + " has created a new ticket").queue(s -> {
                        success.sendMessageEmbeds(builder.build())
                                .setActionRow(Button.primary("ticket-claim", "Claim"),
                                        Button.danger("ticket-close", "Close"))
                                .queue(q -> {
                                    EmbedBuilder builder1 = new EmbedBuilder();
                                    builder1.setColor(new Color(63,226,69,255));
                                    builder1.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
                                    builder1.setDescription("If you opened this ticket accidentally, you have now the opportunity to close it again for 1 minute! Just click `Nevermind!` below\nThis message will delete itself after this minute");

                                    if (!info.equals("")) {
                                        EmbedBuilder infoBuilder = new EmbedBuilder();
                                        if (supporter != null) {
                                            infoBuilder.setAuthor(supporter.getName(), null, supporter.getEffectiveAvatarUrl());
                                        }
                                        infoBuilder.setColor(new Color(63,226,69,255));
                                        infoBuilder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
                                        infoBuilder.setTitle("**Extra**");
                                        infoBuilder.addField("Given Information⠀⠀⠀⠀⠀⠀⠀⠀⠀", info, false);
                                        success.sendMessageEmbeds(infoBuilder.build()).submit();
                                    }

                                    success.sendMessageEmbeds(builder1.build())
                                            .setActionRow(Button.danger("ticket-nevermind", "Nevermind!"))
                                            .queue(suc -> {
                                                suc.delete().queueAfter(1, TimeUnit.MINUTES, msg -> {}, err -> {});
                                            });

                                    new File("./GreevTickets/transcripts").mkdirs();
                                    File transcript = new File("./GreevTickets/transcripts/" + id + ".txt");
                                    try {
                                        transcript.createNewFile();
                                        this.transcript = transcript;
                                        BufferedWriter writer = new BufferedWriter(new FileWriter(transcript, true));
                                        writer.write(q.getId());
                                        writer.newLine();
                                        writer.close();
                                    } catch (IOException e) {
                                        System.out.println("Could not create transcript");
                                    }
                                });
                    });
                });
        return true;
    }

    protected void closeTicket(boolean wasAccident) {
        EmbedBuilder builder = new EmbedBuilder();
        if (!ticketData.getSupporter().equals("")) {
            jda.retrieveUserById(ticketData.getSupporter()).queue(s -> {
                supporter = s;
                builder.setAuthor(supporter.getName(), null, supporter.getEffectiveAvatarUrl());
            });
        }
        jda.retrieveUserById(ticketData.getOwner()).queue(s -> {
            owner = s;
        });
        if (wasAccident) {
            ticketChannel.delete().queue();
            new TicketData(id, dataSource).deleteEntry();
            new File("./GreevTickets/transcripts/" + id + ".txt").delete();
        }else {
            builder.setTitle("Ticket " + id);
            builder.addField("Text Transcript⠀⠀⠀⠀⠀⠀⠀⠀", "See attachment", false);
            builder.setColor(new Color(37, 150, 190));
            builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
            owner.openPrivateChannel()
                    .flatMap(channel -> channel.sendMessageEmbeds(builder.build()).setFiles(FileUpload.fromData(transcript)))
                    .complete();
            ticketChannel.delete().queue();
        }
    }

    protected boolean claim(User supporter) {
        jda.retrieveUserById(ticketData.getOwner()).queue(s -> {
            owner = s;
        });

        if (supporter != owner) {
            this.topic = ticketChannel.getTopic().split(" \\|")[1];
            this.supporter = supporter;
            ticketData.setSupporter(supporter.getId());

            ticketChannel.getManager().setTopic(owner.getAsMention() + " | " + topic + " | " + supporter.getAsMention()).setName("✓-ticket-" + id).queue();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(new Color(63,226,69,255));
            builder.setDescription("Hello there, " + owner.getAsMention() + "!" + """
                            A member of staff will assist you shortly.
                            In the mean time, please describe your issue in as much detail as possible! :)
                            """);
            builder.addField("Topic", topic, false);
            builder.setAuthor(owner.getName(), owner.getEffectiveAvatarUrl());
            builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
            try {
                BufferedReader reader = new BufferedReader(new FileReader(transcript));
                ticketChannel.editMessageEmbedsById(reader.lines().toList().get(0), builder.build()).setActionRow(Button.danger("ticket-close", "Close")).queue();
                reader.close();
            } catch (IOException e) {
                System.out.println("Could not get Embed ID from transcript because: " + e);
            }
            return true;
        }else {
            return false;
        }
    }

    protected void toggleWaiting(boolean waiting) {
        if (waiting) {
            ticketChannel.getManager().setName("\uD83D\uDD50-ticket-" + id).queue();
        }else {
            ticketChannel.getManager().setName("✓-ticket-" + id).queue();
        }
    }

    protected boolean addUser(User user) {
        PermissionOverride permissionOverride = getTicketChannel().getPermissionOverride(jda.getGuildById(serverID).getMember(user));
        if ((permissionOverride != null && permissionOverride.getAllowed().contains(Permission.VIEW_CHANNEL)) || jda.getGuildById(serverID).getMember(user).getPermissions().contains(Permission.ADMINISTRATOR)) {
            return false;
        }else {
            getTicketChannel().upsertPermissionOverride(jda.getGuildById(serverID).getMember(user)).setAllowed(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND).queue();
            ticketData.addInvolved(user.getId());
            return true;
        }
    }

    protected boolean removeUser(User user) {
        PermissionOverride permissionOverride = getTicketChannel().getPermissionOverride(jda.getGuildById(serverID).getMember(user));
        if (permissionOverride != null && permissionOverride.getAllowed().contains(Permission.VIEW_CHANNEL)) {
            getTicketChannel().upsertPermissionOverride(jda.getGuildById(serverID).getMember(user)).setDenied(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND).queue();
            ticketData.removeInvolved(user.getId());
            return true;
        }else {
            return false;
        }
    }

    protected boolean setOwner(User owner) {
        if (!ticketData.getSupporter().equals("")) {
            jda.retrieveUserById(ticketData.getSupporter()).queue(s -> {
                supporter = s;
            });
        }
        if (this.owner.equals(owner)) {
            return false;
        } else {
            ticketChannel.upsertPermissionOverride(jda.getGuildById(serverID).getMember(this.owner)).setDenied(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND).queue();
            this.owner = owner;
            ticketData.setOwner(owner.getId());
            ticketChannel.upsertPermissionOverride(jda.getGuildById(serverID).getMember(owner)).setAllowed(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND).queue();
            if (supporter != null) {
                getTicketChannel().getManager().setTopic(owner.getAsMention() + " | " + topic + " | " + supporter.getAsMention()).queue();
            }else {
                getTicketChannel().getManager().setTopic(owner.getAsMention() + " | " + topic).queue();
            }
            return true;
        }
    }

    protected boolean setSupporter(User supporter) {
        jda.retrieveUserById(ticketData.getOwner()).queue(s -> {
            owner = s;
        });
        if (jda.getGuildById(serverID).getMember(supporter).getRoles().contains(jda.getGuildById(serverID).getRoleById(staffID)) && this.supporter != supporter) {
            this.supporter = supporter;
            ticketData.setSupporter(supporter.getId());
            getTicketChannel().getManager().setTopic(owner.getAsMention() + " | " + topic + " | " + supporter.getAsMention()).queue();
            return true;
        }else {
            return false;
        }
    }

    protected void setTopic(String topic) {
        if (!ticketData.getSupporter().equals("")) {
            jda.retrieveUserById(ticketData.getSupporter()).queue(s -> {
                supporter = s;
            });
        }
        jda.retrieveUserById(ticketData.getOwner()).queue(s -> {
            owner = s;
        });
        this.topic = topic;
        if (supporter != null) {
            getTicketChannel().getManager().setTopic(owner.getAsMention() + " | " + topic + " | " + supporter.getAsMention()).queue();
        }else {
            getTicketChannel().getManager().setTopic(owner.getAsMention() + " | " + topic).queue();
        }
    }

    protected boolean hasAccess(Member member) {
        return member.getUser().equals(supporter) || member.getUser().equals(owner) || member.getRoles().contains(jda.getGuildById(serverID).getRoleById(staffID));
    }

    protected boolean isWaiting() {
        return ticketChannel.getName().contains("\uD83D\uDD50");
    }

    protected boolean isClaimed() {
        return ticketChannel.getTopic().split(" \\| ").length > 2;
    }

    protected String getTopic() {
        return topic;
    }

    protected User getOwner() {
        return owner;
    }

    protected File getTranscript() {
        return transcript;
    }

    protected TextChannel getTicketChannel() {
        return ticketChannel;
    }

    protected String getID() {
        return id;
    }
}