package eu.greev.dcbot.ticketsystem.interactions;


import net.dv8tion.jda.api.events.Event;

public interface Interaction {
    void execute(Event event);
}
