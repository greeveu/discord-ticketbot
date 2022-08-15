package eu.greev.dcbot.ticketsystem;

import eu.greev.dcbot.utils.Utils;
import eu.greev.dcbot.utils.data.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import javax.sql.DataSource;
import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class Ticket {
    private User owner;
    private User supporter;
    private String topic = "No topic given";
    private final String id;
    private final TicketData ticketData;
    private File transcript;
    private final JDA jda;
    private final String serverID = new Data().serverID;
    private final long staffID = new Data().teamID;
    private TextChannel ticketChannel;
    private final DataSource dataSource;
    private boolean closeableByCreator = false;

    protected Ticket(User owner, JDA jda, DataSource dataSource) {
        this.dataSource = dataSource;
        this.owner = owner;
        this.jda = jda;
        id = "";
        ticketData = new TicketData(id, dataSource);
    }

    protected Ticket(long ticketChannelId, JDA jda, DataSource dataSource) {
        this.dataSource = dataSource;
        this.jda = jda;
        id = jda.getTextChannelById(ticketChannelId).getName().replaceAll("\uD83D\uDD50|✓|ticket|-", "");
        ticketData = new TicketData(id, dataSource);
    }

    protected boolean createNewTicket(String info) {
        Member owner = jda.getGuildById(serverID).getMember(this.owner);

        for (TextChannel textChannel : jda.getGuildById(serverID).getTextChannels()) {
            if (textChannel.getName().contains("ticket-") &&
                    new TicketData(textChannel.getName().replaceAll("\uD83D\uDD50|✓|ticket|-", ""), dataSource).getOwner().equals(owner.getId())) {
                return false;
            }
        }

        jda.getGuildById(serverID).createTextChannel("ticket" + (ticketData.getCurrentTickets().size() + 1), jda.getCategoryById(new Data().supportCategory))
                .addRolePermissionOverride(jda.getGuildById(serverID).getPublicRole().getIdLong(), null, List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY))
                .addRolePermissionOverride(staffID, List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY), null)
                .addMemberPermissionOverride(owner.getIdLong(), List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY), null)
                .setTopic(owner.getAsMention() + " | " + topic)
                .queueAfter(500, TimeUnit.MILLISECONDS, success -> {
                    this.ticketChannel = success;
                    TicketData ticketData = new TicketData((this.ticketData.getCurrentTickets().size() + 1) + "", dataSource);
                    ticketData.setOwner(owner.getId());

                    try {
                        new File("./GreevTickets/transcripts").mkdirs();
                        File transcript = new File("./GreevTickets/transcripts/123.txt");
                        transcript.createNewFile();
                        this.transcript = transcript;
                    } catch (IOException e) {
                        System.out.println("Could not create transcript");
                    }

                    EmbedBuilder builder = new EmbedBuilder();
                    EmbedBuilder builder1 = new EmbedBuilder();
                    builder.setColor(new Color(63,226,69,255));
                    builder.setDescription("Hello there, " + owner.getAsMention() + "!" + """
                            A member of staff will assist you shortly.
                            In the mean time, please describe your issue in as much detail as possible! :)
                            """);
                    builder.addField("Topic", topic, false);
                    builder.setAuthor(owner.getEffectiveName(), owner.getEffectiveAvatarUrl());
                    builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                    builder1.setColor(new Color(63,226,69,255));
                    builder1.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
                    builder1.setDescription("If you opened this ticket accidentally, you have now the change to close it again for 1 minute! Just write 'nevermind'\nThis message will delete itself after this minute");

                    success.sendMessage(owner.getAsMention() + " has created a new ticket"
                            + "https://cdn.discordapp.com/attachments/1002631949275365488/1002631954727964772/how-can-i-help-2.gif").queueAfter(10, TimeUnit.MILLISECONDS);
                    success.sendMessageEmbeds(builder.build())
                            .setActionRow(Button.primary("ticket-claim", "Claim"),
                                    Button.danger("ticket-close", "Close"))
                            .queueAfter(15, TimeUnit.MILLISECONDS, s -> {
                                try {
                                    new File("./GreevTickets/transcripts").mkdirs();
                                    File file = new File("./GreevTickets/transcripts/123.txt");
                                    file.createNewFile();

                                    BufferedWriter fw = new BufferedWriter(new FileWriter(file, true));
                                    fw.write(success.getId());
                                    fw.newLine();
                                    fw.close();
                                } catch (IOException e) {
                                    System.out.println("Could not create transcript");
                                }
                            });
                    success.sendMessageEmbeds(builder1.build()).queueAfter(20, TimeUnit.MILLISECONDS, s -> {
                        closeableByCreator = true;
                        while (closeableByCreator) {
                            boolean a = true;
                            if (a) {
                                Timer timer = new Timer();
                                timer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        closeableByCreator = false;
                                        s.delete().queue();
                                        timer.cancel();
                                    }
                                }, TimeUnit.MILLISECONDS.toMinutes(1), TimeUnit.MILLISECONDS.toSeconds(1));
                                a = false;
                            }
                            if (success.getHistory().getMessageById(success.getLatestMessageId()).getContentRaw().equals("nevermind")) {
                                closeTicket(true);
                                ticketData.deleteEntry();
                                closeableByCreator = false;
                            }
                        }
                    });

                    if (!info.equals("")) {
                        EmbedBuilder infoBuilder = new EmbedBuilder();
                        infoBuilder.setColor(new Color(63,226,69,255));
                        infoBuilder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
                        infoBuilder.setTitle("**Extra**");
                        infoBuilder.addField("Given Information", info, false);
                        success.sendMessageEmbeds(infoBuilder.build()).queue();
                    }
                });
        return true;
    }

    protected void closeTicket(boolean wasAccident) {
        jda.retrieveUserById(ticketData.getSupporter()).queue(s -> {
            supporter = s;
        });
        jda.retrieveUserById(ticketData.getOwner()).queue(s -> {
            owner = s;
        });
        if (wasAccident) {
            ticketChannel.delete().queue();
        }else {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setAuthor(supporter.getName(), supporter.getEffectiveAvatarUrl());
            builder.setTitle("Ticket " + id);
            builder.addField("Text Transcript", "See attachment", false);
            builder.setColor(new Color(37, 150, 190));
            builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

            jda.getGuildById(serverID).loadMembers(member -> {
                if (member.getId().equals(owner.getId())) {
                    try {
                        Utils.sendPrivateEmbed(member.getUser(), builder);
                    }catch (Exception e) {
                        System.out.println("Kritischer Fehler beim senden des Ticket-Close-Embeds");
                        e.printStackTrace();
                        Utils.sendPrivateMessage(member.getUser(), "Es ist ein Fehler beim Senden des Closed-Embeds aufgetreten!\nBitte kontaktieren Sie <@!661151077600722947>");
                    }
                }
            });
            ticketChannel.delete().queue();
        }
    }

    protected boolean claim(User supporter) {
        jda.retrieveUserById(ticketData.getSupporter()).queue(s -> {
            this.supporter = s;
        });
        jda.retrieveUserById(ticketData.getOwner()).queue(s -> {
            owner = s;
        });

        if (supporter != owner) {
            this.supporter = supporter;
            ticketData.setSupporter(supporter.getId());
            ticketChannel.getManager().setName("✓-ticket-" + id).queue();

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
            } catch (FileNotFoundException e) {
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

    protected boolean isWaiting() {
        return ticketChannel.getName().contains("\uD83D\uDD50");
    }

    protected boolean addUser(User user) {
        if (getTicketChannel().getPermissionOverride(jda.getGuildById(serverID).getMember(user)).getAllowed().contains(Permission.VIEW_CHANNEL)) {
            return false;
        }else {
            getTicketChannel().upsertPermissionOverride((IPermissionHolder) user).setAllowed(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND).queue();
            ticketData.addInvolved(user.getId());
            return true;
        }
    }

    protected boolean removeUser(User user) {
        if (getTicketChannel().getPermissionOverride(jda.getGuildById(serverID).getMember(user)).getAllowed().contains(Permission.VIEW_CHANNEL)) {
            getTicketChannel().upsertPermissionOverride((IPermissionHolder) user).setDenied(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND).queue();
            ticketData.removeInvolved(user.getId());
            return true;
        }else {
            return false;
        }
    }

    protected TextChannel getTicketChannel() {
        return ticketChannel;
    }

    protected boolean setOwner(User owner) {
        jda.retrieveUserById(ticketData.getSupporter()).queue(s -> {
            supporter = s;
        });
        jda.retrieveUserById(ticketData.getOwner()).queue(s -> {
            this.owner = s;
        });
        if (this.owner.equals(owner)) {
            return false;
        } else {
            this.owner = owner;
            ticketData.setOwner(owner.getId());
            if (supporter != null) {
                getTicketChannel().getManager().setTopic(owner.getAsMention() + " | " + topic + " | " + supporter.getAsMention()).queue();
            }else {
                getTicketChannel().getManager().setTopic(owner.getAsMention() + " | " + topic).queue();
            }
            return true;
        }
    }

    protected boolean setSupporter(User supporter) {
        jda.retrieveUserById(ticketData.getSupporter()).queue(s -> {
            this.supporter = s;
        });
        jda.retrieveUserById(ticketData.getOwner()).queue(s -> {
            owner = s;
        });
        if (((Member) supporter).getRoles().contains(jda.getGuildById(serverID).getRoleById(staffID)) && this.supporter != supporter) {
            this.supporter = supporter;
            ticketData.setSupporter(supporter.getId());
            getTicketChannel().getManager().setTopic(owner.getAsMention() + " | " + topic + " | " + supporter.getAsMention()).queue();
            return true;
        }else {
            return false;
        }
    }

    protected void setTopic(String topic) {
        jda.retrieveUserById(ticketData.getSupporter()).queue(s -> {
            supporter = s;
        });
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

    protected File getTranscript() {
        return transcript;
    }

    protected String getID() {
        return id;
    }
}