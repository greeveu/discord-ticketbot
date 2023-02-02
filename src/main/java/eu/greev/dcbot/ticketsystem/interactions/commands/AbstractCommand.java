package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;

import java.awt.*;

public abstract class AbstractCommand implements Interaction {
    final TicketService ticketService;
    final EmbedBuilder missingPerm;
    final Config config;
    final JDA jda;

    protected AbstractCommand(Config config, TicketService ticketService, EmbedBuilder missingPerm, JDA jda) {
        this.ticketService = ticketService;
        this.missingPerm = missingPerm;
        this.config = config;
        this.jda = jda;
    }

    protected boolean fromGuild(IReplyCallback event) {
        if (event.isFromGuild()) return true;
        event.replyEmbeds(new EmbedBuilder()
                .setColor(Color.RED)
                .setDescription("You have to use this command in a guild!")
                .build())
                .setEphemeral(true)
                .queue();
        return false;
    }
}