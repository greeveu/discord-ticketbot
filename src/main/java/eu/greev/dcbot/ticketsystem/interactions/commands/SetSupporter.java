package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.ticketsystem.service.Transcript;
import eu.greev.dcbot.utils.Constants;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

@AllArgsConstructor
public class SetSupporter extends AbstractCommand {
    private final Role staff;
    private final JDA jda;
    private final TicketService ticketService;
    private final EmbedBuilder wrongChannel;
    private final EmbedBuilder missingPerm;

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        Member member = event.getMember();
        if (!member.getRoles().contains(staff)) {
            event.replyEmbeds(missingPerm.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl()).build()).setEphemeral(true).queue();
            return;
        }
        if (ticketService.getTicketByChannelId(event.getChannel().getIdLong()) == null) {
            event.replyEmbeds(wrongChannel.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl()).build()).setEphemeral(true).queue();
            return;
        }

        Ticket ticket = ticketService.getTicketByChannelId(event.getChannel().getIdLong());
        Member sup = event.getOption("staff").getAsMember();
        if (sup.getRoles().contains(jda.getGuildById(Constants.SERVER_ID).getRoleById(Constants.TEAM_ID)) || !sup.getUser().equals(ticket.getSupporter())) {
            ticket.setSupporter(event.getOption("staff").getAsUser());
            EmbedBuilder builder = new EmbedBuilder().setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO)
                    .setColor(Constants.GREEV_GREEN)
                    .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                    .addField("✅ **New supporter**", event.getOption("staff").getAsUser().getAsMention() + " is the new supporter", false);
            String content = new SimpleDateFormat("[hh:mm:ss a '|' dd'th' MMM yyyy] ").format(new Date(System.currentTimeMillis()))
                    + "> Ticket got transferred to [" + sup.getUser().getName() + "#" + sup.getUser().getDiscriminator() + "].";
            new Transcript(ticket).addMessage(content);
            event.replyEmbeds(builder.build()).queue();
        }else {
            EmbedBuilder builder = new EmbedBuilder().setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO)
                    .setColor(Color.RED)
                    .addField("❌ **Setting new supporter failed**", "This member is either already the supporter or not a staff member", false);
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
        }
    }
}
