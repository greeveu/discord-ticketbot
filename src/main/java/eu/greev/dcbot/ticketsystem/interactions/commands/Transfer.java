package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.ticketsystem.service.Transcript;
import eu.greev.dcbot.utils.Config;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

@AllArgsConstructor
public class Transfer extends AbstractCommand {
    private final JDA jda;
    private final Config config;
    private final TicketService ticketService;
    private final EmbedBuilder wrongChannel;
    private final EmbedBuilder missingPerm;

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        if (config.getServerName() == null) {
            EmbedBuilder error = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setDescription("❌ **Ticketsystem wasn't setup, please tell an Admin to use </ticket setup:0>!**");
            event.replyEmbeds(error.build()).setEphemeral(true).queue();
            return;
        }
        if (!event.getMember().getRoles().contains(jda.getRoleById(config.getStaffId()))) {
            event.replyEmbeds(missingPerm.setFooter(config.getServerName(), config.getServerLogo()).build()).setEphemeral(true).queue();
            return;
        }
        if (ticketService.getTicketByChannelId(event.getChannel().getIdLong()) == null) {
            event.replyEmbeds(wrongChannel
                            .setFooter(config.getServerName(), config.getServerLogo())
                            .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                            .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }
        Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
        EmbedBuilder error = new EmbedBuilder()
                .setColor(Color.RED)
                .setFooter(config.getServerName(), config.getServerLogo());

        if (ticket.getSupporter() == null) {
            event.replyEmbeds(error.setDescription("You can not transfer a ticket which wasn't claimed!").build())
                    .setEphemeral(true)
                    .queue();
            return;
        }
         if (!ticket.getSupporter().equals(event.getUser()) && !event.getMember().getPermissions().contains(Permission.ADMINISTRATOR)) {
             event.replyEmbeds(error.setDescription("You can not transfer this ticket since you don't handle it or you don't have enough permissions!").build())
                     .setEphemeral(true)
                     .queue();
             return;
         }

        Member sup = event.getOption("staff").getAsMember();
        if (sup.getRoles().contains(jda.getRoleById(config.getStaffId())) || !sup.getUser().equals(ticket.getSupporter())) {
            ticket.setSupporter(sup.getUser());
            ticketService.updateChannelTopic(ticket);
            EmbedBuilder builder = new EmbedBuilder().setFooter(config.getServerName(), config.getServerLogo())
                    .setColor(Color.decode(config.getColor()))
                    .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                    .addField("✅ **New supporter**", sup.getAsMention() + " is the new supporter", false);
            String content = new SimpleDateFormat("[hh:mm:ss a '|' dd'th' MMM yyyy] ").format(new Date(System.currentTimeMillis()))
                    + "> Ticket got transferred to [" + sup.getUser().getName() + "#" + sup.getUser().getDiscriminator() + "].";
            new Transcript(ticket).addMessage(content);
            event.replyEmbeds(builder.build()).queue();
        }else {
            EmbedBuilder builder = new EmbedBuilder().setFooter(config.getServerName(), config.getServerLogo())
                    .setColor(Color.RED)
                    .addField("❌ **Setting new supporter failed**", "This member is either already the supporter or not a staff member", false);
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
        }
    }
}