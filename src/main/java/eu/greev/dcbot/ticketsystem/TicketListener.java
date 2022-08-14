package eu.greev.dcbot.ticketsystem;

import eu.greev.dcbot.utils.data.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.awt.*;

public class TicketListener extends ListenerAdapter {
    private final long serverID = new Data().testID;
    private final Role staff;
    private final DataSource dataSource;
    private final JDA jda;

    public TicketListener(JDA jda, DataSource dataSource) {
        this.dataSource = dataSource;
        this.jda = jda;
        staff = jda.getGuildById(serverID).getRoleById(new Data().teamID);
    }


    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getButton().getId() == null) return;

    }

    @Override
    public void onModalInteraction(@Nonnull ModalInteractionEvent event) {

    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("ticket")) {
            switch (event.getSubcommandName()) {
                case "setup" -> {
                    if (jda.getTextChannelById(new Data().baseChannel).getHistory() == null) {
                        jda.getTextChannelById(new Data().baseChannel).getHistory().getRetrievedHistory().forEach(message -> {
                            message.delete().queue();
                        });
                    }
                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setFooter("Powered by Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
                    builder.addField(new MessageEmbed.Field("**Support request**", """
                        You have questions or a problem?
                        Just click the button below or use `/ticket create` somewhere else.
                        We will try to handle your ticket as soon as possible.
                        """, false));
                    builder.setColor(new Color(63,226,69,255));

                    jda.getTextChannelById(new Data().baseChannel).sendMessageEmbeds(builder.build())
                            .setActionRow(
                                    Button.primary("ticket-create-pardon", "Pardon"),
                                    Button.primary("ticket-create-report", "Report"),
                                    Button.primary("ticket-create-complain", "Complain"),
                                    Button.primary("ticket-create-custom", "Custom"))
                            .queue();
                }
                case "create" -> {
                    Ticket ticket = new Ticket(event.getUser(), jda, dataSource);
                    if (ticket.createNewTicket()) {
                        EmbedBuilder builder = new EmbedBuilder();
                        builder.setTitle("Title created");
                        builder.setAuthor(event.getMember().getAsMention(), null, event.getMember().getEffectiveAvatarUrl());
                        builder.addField("", "Successfully created a ticket for you " + ticket.getTicketChannel().getAsMention(), false);
                        builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                        event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                    }else {
                        event.reply("There already is an opened ticket for you. Please use this instead first or close it -> " + ticket.getTicketChannel().getAsMention()).setEphemeral(true).queue();
                    }
                }
                case "claim" -> {
                    Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);

                    if (event.getMessageChannel().getName().contains("ticket-")) {
                        if (ticket.claim(event.getUser())) {
                            event.reply(event.getMember().getAsMention() + " claimed the ticket!").queue();
                        }else {
                            event.reply("You are not permitted to claim this ticket").setEphemeral(true).queue();
                        }
                    }else {
                        event.reply("You have to use this command in a ticket!").setEphemeral(true).queue();
                    }
                }
                case "close" -> {
                    Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                    if (event.getMessageChannel().getName().contains("ticket-")) {
                        if (ticket.closeTicket(false)) {
                            event.reply(event.getMember().getAsMention() + " closed the ticket!").queue();
                        } else {
                            event.reply("You can not close this ticket").queue();
                        }
                    } else {
                        event.reply("You have to use this command in a ticket!").setEphemeral(true).queue();
                    }
                }
                case "add" -> {
                    Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                    if (event.getMessageChannel().getName().contains("ticket-")) {
                        if (ticket.addUser(event.getOption("member").getAsUser())) {
                            event.reply(event.getOption("member").getAsUser().getAsMention() + " got added to the ticket").queue();
                        } else {
                            event.reply(event.getOption("member").getAsUser().getAsMention() + " is already in the ticket").setEphemeral(true).queue();
                        }
                    }else {
                        event.reply("You have to use this command in a ticket!").setEphemeral(true).queue();
                    }
                }
                case "remove" -> {
                    Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                    if (event.getMessageChannel().getName().contains("ticket-")) {
                        if (ticket.removeUser(event.getOption("member").getAsUser())) {
                            event.reply(event.getOption("member").getAsUser().getAsMention() + " got removed from the ticket").queue();
                        } else {
                            event.reply(event.getOption("member").getAsUser().getAsMention() + " is already not in the ticket").setEphemeral(true).queue();
                        }
                    }else {
                        event.reply("You have to use this command in a ticket!").setEphemeral(true).queue();
                    }
                }
                case "supporter" -> {
                    Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                    if (event.getMessageChannel().getName().contains("ticket-")) {
                        if (ticket.setSupporter(event.getOption("staff").getAsUser())) {
                            event.reply(event.getOption("staff").getAsUser().getAsMention() + " is now the new supporter").queue();
                        }else {
                            event.reply("This member is either already the supporter or not a staff member").queue();
                        }
                    }else {
                        event.reply("You have to use this command in a ticket!").setEphemeral(true).queue();
                    }
                }
                case "owner" -> {
                    Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                    if (event.getMessageChannel().getName().contains("ticket-")) {
                        if (ticket.setOwner(event.getOption("member").getAsUser())) {
                            event.reply(event.getOption("member").getAsUser().getAsMention() + " is now the new owner of the ticket").queue();
                        }else {
                            event.reply("This member is already the creator").queue();
                        }
                    }else {
                        event.reply("You have to use this command in a ticket!").setEphemeral(true).queue();
                    }
                }
                case "waiting" -> {
                    Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                    if (event.getMessageChannel().getName().contains("ticket-")) {
                        if (ticket.setWaiting()) {
                            EmbedBuilder builder = new EmbedBuilder();
                            builder.setAuthor(event.getUser().getName(), event.getMember().getEffectiveAvatarUrl());
                            builder.setDescription("Waiting for response.");
                            builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
                            builder.setColor(new Color(63,226,69,255));
                            event.getMessageChannel().sendMessageEmbeds(builder.build()).queue();
                        }else {
                            event.reply("This ticket is already in waiting mode!").setEphemeral(true).queue();
                        }
                    }else {
                        event.reply("You have to use this command in a ticket!").setEphemeral(true).queue();
                    }

                }
                case "topic" -> {
                    Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                    if (event.getMessageChannel().getName().contains("ticket-")) {
                        ticket.setTopic(event.getOption("topic").getAsString());
                    }else {
                        event.reply("You have to use this command in a ticket!").setEphemeral(true).queue();
                    }
                }
            }
        }
    }
}