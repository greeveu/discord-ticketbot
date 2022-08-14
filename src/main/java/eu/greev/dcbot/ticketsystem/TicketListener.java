package eu.greev.dcbot.ticketsystem;

import eu.greev.dcbot.utils.data.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.awt.*;

public class TicketListener extends ListenerAdapter {
    private final Role staff;
    private final DataSource dataSource;
    private final JDA jda;

    public TicketListener(JDA jda, DataSource dataSource) {
        this.dataSource = dataSource;
        this.jda = jda;
        long serverID = new Data().testID;
        staff = jda.getGuildById(serverID).getRoleById(new Data().teamID);
    }


    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getButton().getId() == null) return;
        Ticket ticket;
        switch (event.getButton().getId()) {
            case "ticket-claim" -> {
                if (event.getMember().getRoles().contains(staff)) {
                    ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                    if (ticket.claim(event.getUser())) {
                        EmbedBuilder builder = new EmbedBuilder();
                        builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
                        builder.setColor(new Color(63,226,69,255));
                        builder.setAuthor(event.getUser().getName(), event.getUser().getEffectiveAvatarUrl());
                        builder.addField("✅ **Ticket claimed**", "Your ticket will be handled by " + event.getUser().getAsMention(), false);

                        event.replyEmbeds(builder.build()).queue();
                    }else {
                        event.reply("You can not claim this ticket").queue();
                    }
                }else {
                    event.reply("You are not permitted to use this command").setEphemeral(true).queue();
                }
            }
            case "ticket-close" -> {
                if (event.getMember().getRoles().contains(staff)) {
                    if (event.getMessageChannel().getName().contains("ticket-")) {
                        EmbedBuilder builder = new EmbedBuilder();
                        builder.setColor(Color.WHITE);
                        builder.addField("Close Confirmation", "Do you really want to close this ticket?", true);
                        event.replyEmbeds(builder.build())
                                .addActionRow(Button.primary("ticket-confirm", "✔️ Close"))
                                .setEphemeral(true)
                                .queue();
                    } else {
                        event.reply("You have to use this command in a ticket!").setEphemeral(true).queue();
                    }
                }else {
                    event.reply("You are not permitted to use this command").setEphemeral(true).queue();
                }
            }
            case "ticket-create-pardon" -> {
                TextInput member = TextInput.create("member", "Name", TextInputStyle.SHORT)
                        .setPlaceholder("Your Minecraft name")
                        .setMinLength(2)
                        .setMaxLength(12)
                        .build();
                TextInput info = TextInput.create("info", "Description", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("Give us more information about your problem")
                        .setMinLength(5)
                        .build();

                Modal modal = Modal.create("pardon", "Give us more information!")
                        .addActionRows(ActionRow.of(member), ActionRow.of(info))
                        .build();
                event.replyModal(modal).queue();
            }
            case "ticket-create-report" -> {
                TextInput member = TextInput.create("member", "Name", TextInputStyle.SHORT)
                        .setPlaceholder("Your Minecraft name")
                        .setMinLength(2)
                        .setMaxLength(12)
                        .build();
                TextInput hacker = TextInput.create("hacker", "Name", TextInputStyle.SHORT)
                        .setPlaceholder("Who do you want to report")
                        .setMinLength(2)
                        .setMaxLength(12)
                        .build();
                Modal modal = Modal.create("report", "Give us more information!")
                        .addActionRows(ActionRow.of(member), ActionRow.of(hacker))
                        .build();
                event.replyModal(modal).queue();
            }
            case "ticket-create-complain" -> {
                TextInput complain = TextInput.create("complain", "Complain", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("Give us more information about your problem")
                        .setMinLength(5)
                        .build();

                Modal modal = Modal.create("complain", "Give us more information!")
                        .addActionRows(ActionRow.of(complain))
                        .build();
                event.replyModal(modal).queue();
            }
            case "ticket-create-custom" -> {
                TextInput reason = TextInput.create("topic", "Topic", TextInputStyle.SHORT)
                        .setPlaceholder("Your own topic for the ticket.")
                        .setMinLength(1)
                        .setMaxLength(50)
                        .build();
                Modal modal = Modal.create("custom", "Create ticket with custom topic")
                        .addActionRows(ActionRow.of(reason))
                        .build();
                event.replyModal(modal).queue();
            }
            case "ticket-confirm" -> new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource).closeTicket(false);
        }

    }

    @Override
    public void onModalInteraction(@Nonnull ModalInteractionEvent event) {
        Ticket ticket = new Ticket(event.getUser(), jda, dataSource);;
        switch (event.getModalId()) {
            case "custom" -> {
                String topic = event.getValue("topic").getAsString();
                if (ticket.createNewTicket("")) {
                    ticket.setTopic(topic);
                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setTitle("Ticket created");
                    builder.setAuthor(event.getUser().getAsMention(), null, event.getMember().getEffectiveAvatarUrl());
                    builder.addField("", "Successfully created a ticket for you " + ticket.getTicketChannel().getAsMention(), false);
                    builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                    event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                }else {
                    event.reply("There already is an opened ticket for you. Please use this instead first or close it -> " + ticket.getTicketChannel().getAsMention()).setEphemeral(true).queue();
                }
            }
            case "complain" -> {
                String complain = event.getValue("complain").getAsString();
                if (ticket.createNewTicket(complain)) {
                    ticket.setTopic("Complain");
                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setTitle("Ticket created");
                    builder.setAuthor(event.getUser().getAsMention(), null, event.getMember().getEffectiveAvatarUrl());
                    builder.addField("", "Successfully created a ticket for you " + ticket.getTicketChannel().getAsMention(), false);
                    builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                    event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                }else {
                    event.reply("There already is an opened ticket for you. Please use this instead first or close it -> " + ticket.getTicketChannel().getAsMention()).setEphemeral(true).queue();
                }
            }
            case "report" -> {
                String name = event.getValue("member").getAsString();
                String report = event.getValue("hacker").getAsString();
                if (ticket.createNewTicket("")) {
                    ticket.setTopic(name + " reports " + report);
                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setTitle("Ticket created");
                    builder.setAuthor(event.getUser().getAsMention(), null, event.getMember().getEffectiveAvatarUrl());
                    builder.addField("", "Successfully created a ticket for you " + ticket.getTicketChannel().getAsMention(), false);
                    builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                    event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                }else {
                    event.reply("There already is an opened ticket for you. Please use this instead first or close it -> " + ticket.getTicketChannel().getAsMention()).setEphemeral(true).queue();
                }
            }
            case "pardon" -> {
                String name = event.getValue("member").getAsString();
                String info = event.getValue("info").getAsString();

                if (ticket.createNewTicket(info)) {
                    ticket.setTopic(name + " wants pardon");
                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setTitle("Ticket created");
                    builder.setAuthor(event.getUser().getAsMention(), null, event.getMember().getEffectiveAvatarUrl());
                    builder.addField("", "Successfully created a ticket for you " + ticket.getTicketChannel().getAsMention(), false);
                    builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                    event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                }else {
                    event.reply("There already is an opened ticket for you. Please use this instead first or close it -> " + ticket.getTicketChannel().getAsMention()).setEphemeral(true).queue();
                }
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("ticket")) {
            Member member = event.getMember();
            switch (event.getSubcommandName()) {
                case "setup" -> {
                    if (member.getPermissions().contains(Permission.ADMINISTRATOR)) {
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
                    }else {
                        event.reply("You are not permitted to use this command").setEphemeral(true).queue();
                    }
                }
                case "create" -> {
                    Ticket ticket = new Ticket(event.getUser(), jda, dataSource);
                    if (ticket.createNewTicket("")) {
                        EmbedBuilder builder = new EmbedBuilder();
                        builder.setTitle("Ticket created");
                        builder.setAuthor(member.getAsMention(), null, event.getMember().getEffectiveAvatarUrl());
                        builder.addField("", "Successfully created a ticket for you " + ticket.getTicketChannel().getAsMention(), false);
                        builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                        event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                    }else {
                        event.reply("There already is an opened ticket for you. Please use this instead first or close it -> " + ticket.getTicketChannel().getAsMention()).setEphemeral(true).queue();
                    }
                }
                case "claim" -> {
                    if (member.getRoles().contains(staff)) {
                        if (event.getMessageChannel().getName().contains("ticket-")) {
                            Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                            if (ticket.claim(event.getUser())) {
                                event.reply(member.getAsMention() + " claimed the ticket!").queue();
                            }else {
                                event.reply("You are not permitted to claim this ticket").setEphemeral(true).queue();
                            }
                        }else {
                            event.reply("You have to use this command in a ticket!").setEphemeral(true).queue();
                        }
                    }else {
                        event.reply("You are not permitted to use this command").setEphemeral(true).queue();
                    }
                }
                case "close" -> {
                    if (event.getMember().getRoles().contains(staff)) {
                        if (event.getMessageChannel().getName().contains("ticket-")) {
                            EmbedBuilder builder = new EmbedBuilder();
                            builder.setColor(Color.WHITE);
                            builder.addField("Close Confirmation", "Do you really want to close this ticket?", true);
                            event.replyEmbeds(builder.build())
                                    .addActionRow(Button.primary("ticket-confirm", "✔️ Close"))
                                    .setEphemeral(true)
                                    .queue();
                        } else {
                            event.reply("You have to use this command in a ticket!").setEphemeral(true).queue();
                        }
                    }else {
                        event.reply("You are not permitted to use this command").setEphemeral(true).queue();
                    }
                }
                case "add" -> {
                    if (member.getRoles().contains(staff)) {
                        if (event.getMessageChannel().getName().contains("ticket-")) {
                            Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                            if (ticket.addUser(event.getOption("member").getAsUser())) {
                                event.reply(event.getOption("member").getAsUser().getAsMention() + " got added to the ticket").queue();
                            } else {
                                event.reply(event.getOption("member").getAsUser().getAsMention() + " is already in the ticket").setEphemeral(true).queue();
                            }
                        }else {
                            event.reply("You have to use this command in a ticket!").setEphemeral(true).queue();
                        }
                    }else {
                        event.reply("You are not permitted to use this command").setEphemeral(true).queue();
                    }
                }
                case "remove" -> {
                    if (member.getRoles().contains(staff)) {
                        if (event.getMessageChannel().getName().contains("ticket-")) {
                            Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                            if (ticket.removeUser(event.getOption("member").getAsUser())) {
                                event.reply(event.getOption("member").getAsUser().getAsMention() + " got removed from the ticket").queue();
                            } else {
                                event.reply(event.getOption("member").getAsUser().getAsMention() + " is already not in the ticket").setEphemeral(true).queue();
                            }
                        }else {
                            event.reply("You have to use this command in a ticket!").setEphemeral(true).queue();
                        }
                    }else {
                        event.reply("You are not permitted to use this command").setEphemeral(true).queue();
                    }
                }
                case "supporter" -> {
                    if (member.getRoles().contains(staff)) {
                        if (event.getMessageChannel().getName().contains("ticket-")) {
                            Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                            if (ticket.setSupporter(event.getOption("staff").getAsUser())) {
                                event.reply(event.getOption("staff").getAsUser().getAsMention() + " is now the new supporter").queue();
                            }else {
                                event.reply("This member is either already the supporter or not a staff member").queue();
                            }
                        }else {
                            event.reply("You have to use this command in a ticket!").setEphemeral(true).queue();
                        }
                    }else {
                        event.reply("You are not permitted to use this command").setEphemeral(true).queue();
                    }
                }
                case "owner" -> {
                    if (member.getRoles().contains(staff)) {
                        if (event.getMessageChannel().getName().contains("ticket-")) {
                            Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                            if (ticket.setOwner(event.getOption("member").getAsUser())) {
                                event.reply(event.getOption("member").getAsUser().getAsMention() + " is now the new owner of the ticket").queue();
                            }else {
                                event.reply("This member is already the creator").queue();
                            }
                        }else {
                            event.reply("You have to use this command in a ticket!").setEphemeral(true).queue();
                        }
                    }else {
                        event.reply("You are not permitted to use this command").setEphemeral(true).queue();
                    }
                }
                case "waiting" -> {
                    if (member.getRoles().contains(staff)) {
                        if (event.getMessageChannel().getName().contains("ticket-")) {
                            Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                            if (ticket.toggleWaiting(true)) {
                                EmbedBuilder builder = new EmbedBuilder();
                                builder.setAuthor(member.getEffectiveName(), member.getEffectiveAvatarUrl());
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
                    }else {
                        event.reply("You are not permitted to use this command").setEphemeral(true).queue();
                    }
                }
                case "topic" -> {
                    if (member.getRoles().contains(staff)) {
                        if (event.getMessageChannel().getName().contains("ticket-")) {
                            Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                            ticket.setTopic(event.getOption("topic").getAsString());
                        }else {
                            event.reply("You have to use this command in a ticket!").setEphemeral(true).queue();
                        }
                    }else {
                        event.reply("You are not permitted to use this command").setEphemeral(true).queue();
                    }
                }
            }
        }
    }
}