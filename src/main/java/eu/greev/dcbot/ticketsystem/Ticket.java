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
    private String topic;
    private final String id;
    private final JDA jda;
    private final long serverID = new Data().testID;
    private final long staffID = new Data().teamID;
    private final long supportCategory = new Data().teamID;
    private final TextChannel baseChannel;

    protected Ticket(User owner, JDA jda) {
        this.owner = owner;
        this.jda = jda;
        baseChannel = jda.getTextChannelById(new Data().baseChannel);
        id = "";
    }

    protected Ticket(Ticket ticket, JDA jda) {
        this.owner = ticket.getOwner();
        this.jda = jda;
        baseChannel = jda.getTextChannelById(new Data().baseChannel);
        id = "";
    }

    protected void setup() {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setFooter("Powered by Greev.eu", "https://cdn.discordapp.com/emojis/355825850152779786.webp?size=96&quality=lossless");
        builder.addField(new MessageEmbed.Field("**Support request**", """
                You have questions or a problem?
                Just click the button below or use `/ticket create` somewhere else.
                We will try to handle your ticket as soon as possible.
                """, false));
        builder.setColor(new Color(63,226,69,255));
        baseChannel.sendMessageEmbeds(builder.build())
                .setActionRow(Button.primary("ticket-base", "Create Ticket").withEmoji(Emoji.fromUnicode("\uD83D\uDCE9")))
                .queue();
    }

    //under development
    protected boolean createNewTicket() {
        Member member = (Member) owner;
        if (!jda.getGuildById(serverID).getTextChannelsByName(member.getEffectiveName().toLowerCase() + "ticket", true).isEmpty()) {

            String ticketMention = jda.getGuildById(serverID).getTextChannelsByName(member.getEffectiveName().toLowerCase() + "-ticket", true).get(0).getAsMention();
            //return if false ->   event.reply("Es befindet sich bereits ein offenes Ticket für sie hier -> " + ticketMention).setEphemeral(true).queue();
            return false;
        }

        jda.getGuildById(serverID).createTextChannel(member.getEffectiveName().toLowerCase() + "-ticket", jda.getCategoryById(new Data().supportCategory))
                .addRolePermissionOverride(jda.getGuildById(serverID).getPublicRole().getIdLong(), null, List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY))
                .addRolePermissionOverride(staffID, List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY), null)
                .addMemberPermissionOverride(member.getIdLong(), List.of(Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY), null)
                .queueAfter(500, TimeUnit.MILLISECONDS, success -> {
                    MessageChannel ticketChannel = null;
                    for (int i = 0; i < jda.getGuildById(serverID).getTextChannels().size(); i++) {
                        MessageChannel channel = jda.getGuildById(serverID).getTextChannels().get(i);
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

                    ticketChannel.sendMessageEmbeds(builder.build())
                            .setActionRow(Button.primary("ticket-claim", "Claim").withEmoji(Emoji.fromUnicode("\uD83C\uDF9F")),
                                    Button.danger("ticket-close", "Close").withEmoji(Emoji.fromUnicode("\uD83D\uDD12")),
                                    Button.danger("ticket-close-reason", "Close with Reason").withEmoji(Emoji.fromUnicode("\uD83D\uDD12")))
                            .queueAfter(10, TimeUnit.MILLISECONDS, s -> {
                                //file.set("tickets." + finalTicketChannel.getId() + ".ticketEmbedId", s.getId());
                            });
                    //return if true ->   event.reply("Es wurde erfolgreich ein Ticket für dich erstellt ->" + ticketChannel.getAsMention()).setEphemeral(true).queue();
                });
        return true;
    }

    protected void closeTicket() {

    }

    protected void addUser(User user) {

    }

    protected void removeUser(User user) {

    }

    protected void claim(User claimer) {

        this.supporter = claimer;
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
        if (((Member) supporter).getRoles().contains(jda.getGuildById(serverID).getRoleById(staffID))) {
            this.supporter = supporter;
            return true;
        }else {
            //if false -> event.reply("This member isn't in the staff team").setEphemeral(true).queue();
            return false;
        }
    }

    protected User getSupporter() {
        return supporter;
    }

    protected void setTopic(String topic) {
        this.topic = topic;
    }

    protected String getTopic() {
        return topic;
    }

    protected String getID() {
        return id;
    }
}