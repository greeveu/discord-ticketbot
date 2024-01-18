package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.entities.Ticket;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.List;

public class GetTickets extends AbstractCommand {

    public GetTickets(Config config, TicketService ticketService, EmbedBuilder missingPerm, JDA jda) {
        super(config, ticketService, missingPerm, jda);
    }

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        if (!event.getMember().getRoles().contains(jda.getRoleById(config.getStaffId()))) {
            event.replyEmbeds(missingPerm.setFooter(config.getServerName(), config.getServerLogo()).build()).setEphemeral(true).queue();
            return;
        }
        User user = event.getOption("member").getAsUser();
        List<Integer> tickets = ticketService.getTicketIdsByOwner(user.getId());
        EmbedBuilder builder = new EmbedBuilder()
                .setAuthor(user.getName(), "https://www.discordapp.com/users/" + user.getId(), user.getAvatarUrl())
                .setTitle("This user opened following tickets:")
                .setFooter(config.getServerName(), config.getServerLogo())
                .setColor(Color.decode(config.getColor()));

        if (tickets.isEmpty()) {
            builder.setColor(Color.RED).setTitle("This user never opened a ticket");
            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            return;
        }

        for (int i = 0; i < 25; i++) {
            if (tickets.size() - 1 == i) break;
            Ticket ticket = ticketService.getTicketByTicketId(tickets.get(i));
            if (ticket == null) {
                EmbedBuilder error = new EmbedBuilder()
                        .setColor(Color.RED)
                        .setDescription("❌ **Something went wrong, please report this to the Bot creator!**");
                event.replyEmbeds(error.build()).setEphemeral(true).queue();
                return;
            }
            builder.addField(generateName(ticket.getTopic(), tickets.get(i)), ticket.getTopic(), false);
        }

        event.replyEmbeds(builder.setDescription("Page 1/%d".formatted(tickets.size() / 25 + (tickets.size() % 25 == 0 ? 0 : 1))).build()).setActionRow(
                Button.primary("tickets-backwards", Emoji.fromUnicode("◀️")),
                Button.primary("tickets-forwards", Emoji.fromUnicode("▶️"))
        ).setEphemeral(true).queue();
    }

    public static String generateName(String topic, int ticketId) {
        String name;
        if (topic.equals("Bugreport")) {
            name = "Bugreport #" + ticketId;
        } else if (topic.contains(" wants pardon ")) {
            name = "Pardon #" + ticketId;
        } else if (topic.contains(" apply ")) {
            name = "Application #" + ticketId;
        } else {
            name = "Ticket #" + ticketId;
        }
        return name;
    }
}