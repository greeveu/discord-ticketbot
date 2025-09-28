package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.util.Map;

public class SetClaimEmoji extends AbstractCommand {
    public SetClaimEmoji(Config config, TicketService ticketService, EmbedBuilder missingPerm, JDA jda) {
        super(config, ticketService, missingPerm, jda);
    }

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        if (!event.getMember().getRoles().contains(jda.getRoleById(config.getStaffId()))) {
            event.replyEmbeds(missingPerm.setFooter(config.getServerName(), config.getServerLogo()).build()).setEphemeral(true).queue();
            return;
        }

        String emoji = event.getOption("emoji").getAsString();
        int codePoint = emoji.codePointAt(0);

        if (!Character.isEmoji(codePoint)) {
            event.replyEmbeds(new EmbedBuilder()
                    .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                    .setTitle("❌ **Please provide a valid emoji**")
                    .setColor(Color.RED)
                    .setFooter(config.getServerName(), config.getServerLogo())
                    .build()
            ).setEphemeral(true).queue();
            return;
        }

        Map<Long, String> emojis = config.getClaimEmojis();

        if (emojis.containsValue(emoji) && !emoji.equals(emojis.get(event.getUser().getIdLong()))) {
            event.replyEmbeds(new EmbedBuilder()
                    .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                    .setTitle("❌ **This emoji is already in use by another staff member**")
                    .setColor(Color.RED)
                    .setFooter(config.getServerName(), config.getServerLogo())
                    .build()
            ).setEphemeral(true).queue();
            return;
        }

        emojis.put(event.getUser().getIdLong(), emoji);
        config.setClaimEmojis(emojis);
        config.dumpConfig("./Tickets/config.yml");

        event.replyEmbeds(new EmbedBuilder()
                .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                .setTitle("✅ **Successfully set your claim emoji to %s**".formatted(emoji))
                .setColor(Color.decode(config.getColor()))
                .setFooter(config.getServerName(), config.getServerLogo())
                .build()
        ).setEphemeral(true).queue();
    }
}
