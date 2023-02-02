package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;

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
}