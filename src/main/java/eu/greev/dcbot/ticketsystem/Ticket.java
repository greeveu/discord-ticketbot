package eu.greev.dcbot.ticketsystem;

import eu.greev.dcbot.utils.data.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Ticket {
    private User owner;
    private User supporter;
    private String topic = "No topic given";
    private final String id;
    private final JDA jda;
    private final long serverID = new Data().testID;
    private final long staffID = new Data().teamID;
    private TextChannel ticketChannel;

    protected Ticket(User owner, JDA jda) {
        this.owner = owner;
        this.jda = jda;
        id = "";
    }

    protected Ticket(long ticketChannelId, JDA jda) {
        this.jda = jda;
        id = "";
    }

    //under development
    protected boolean createNewTicket() {
        Member member = (Member) owner;
        if (!jda.getGuildById(serverID).getTextChannelsByName(member.getEffectiveName().toLowerCase() + "ticket", true).isEmpty()) {

            String ticketMention = jda.getGuildById(serverID).getTextChannelsByName(member.getEffectiveName().toLowerCase() + "-ticket", true).get(0).getAsMention();
            return false;
        }

        jda.getGuildById(serverID).createTextChannel(member.getEffectiveName().toLowerCase() + "-ticket", jda.getCategoryById(new Data().supportCategory))
                .addRolePermissionOverride(jda.getGuildById(serverID).getPublicRole().getIdLong(), null, List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY))
                .addRolePermissionOverride(staffID, List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY), null)
                .addMemberPermissionOverride(member.getIdLong(), List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY), null)
                .setTopic(member.getAsMention() + " | " + topic)
                .queueAfter(500, TimeUnit.MILLISECONDS, success -> {
                    TextChannel ticketChannel = null;
                    for (int i = 0; i < jda.getGuildById(serverID).getTextChannels().size(); i++) {
                        TextChannel channel = jda.getGuildById(serverID).getTextChannels().get(i);
                        if (channel.getName().equals(member.getEffectiveName().toLowerCase() + "-ticket")) {
                            ticketChannel = channel;
                            /*file.set("tickets." + ticketChannel.getId() + ".closeReason", "Not specified yet");
                            file.set("tickets." + ticketChannel.getId() + ".creator", member.getId());
                            file.set("tickets." + ticketChannel.getId() + ".timeCreated", day);*/
                        }
                    }
                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setColor(Color.white);
                    builder.addField(new MessageEmbed.Field("**Support request**", "Hello there, " + member.getAsMention() + "!" + """
                            A member of staff will assist you shortly.
                            In the mean time, please describe your issue in as much detail as possible! :)
                            """, false));

                    this.ticketChannel = ticketChannel;
                    ticketChannel.sendMessageEmbeds(builder.build())
                            .setActionRow(Button.primary("ticket-claim", "Claim").withEmoji(Emoji.fromUnicode("\uD83C\uDF9F")),
                                    Button.danger("ticket-close", "Close").withEmoji(Emoji.fromUnicode("\uD83D\uDD12")),
                                    Button.danger("ticket-close-reason", "Close with Reason").withEmoji(Emoji.fromUnicode("\uD83D\uDD12")))
                            .queueAfter(10, TimeUnit.MILLISECONDS, s -> {
                                //file.set("tickets." + finalTicketChannel.getId() + ".ticketEmbedId", s.getId());
                            });
                });
        return true;
    }

    protected void closeTicket() {

    }

    protected boolean addUser(User user) {
        if (getTicketChannel().getPermissionOverride(jda.getGuildById(serverID).getMember(user)).getAllowed().contains(Permission.VIEW_CHANNEL)) {
            return false;
        }else {
            getTicketChannel().upsertPermissionOverride((IPermissionHolder) user).setAllowed(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND).queue();
            return true;
        }
    }

    protected boolean removeUser(User user) {
        if (getTicketChannel().getPermissionOverride(jda.getGuildById(serverID).getMember(user)).getAllowed().contains(Permission.VIEW_CHANNEL)) {
            getTicketChannel().upsertPermissionOverride((IPermissionHolder) user).setDenied(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND).queue();
            return true;
        }else {
            return false;
        }
    }

    protected TextChannel getTicketChannel() {
        return ticketChannel;
    }

    protected void saveTicket() {

    }

    protected void setOwner(User owner) {
        this.owner = owner;
    }

    protected User getOwner() {
        return owner;
    }

    protected boolean setSupporter(User supporter) {
        if (((Member) supporter).getRoles().contains(jda.getGuildById(serverID).getRoleById(staffID)) && this.supporter != supporter) {
            getTicketChannel().getManager().setTopic(getOwner().getAsMention() + " | " + topic + " | " + supporter.getAsMention()).queue();
            this.supporter = supporter;
            return true;
        }else {
            return false;
        }
    }

    protected User getSupporter() {
        return supporter;
    }

    protected void setTopic(String topic) {
        this.topic = topic;
    }

    protected String getID() {
        return id;
    }
}