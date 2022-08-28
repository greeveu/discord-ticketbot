package eu.greev.dcbot.ticketsystem;

import eu.greev.dcbot.data.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
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
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TicketListener extends ListenerAdapter {
    private final Role staff;
    private final DataSource dataSource;
    private final JDA jda;
    private final Data data = new Data();
    private final EmbedBuilder missingPerm;
    private final EmbedBuilder wrongChannel;

    public TicketListener(JDA jda, DataSource dataSource) {
        this.dataSource = dataSource;
        this.jda = jda;
        String serverID = data.serverID;
        staff = jda.getGuildById(serverID).getRoleById(data.teamID);
        missingPerm = new EmbedBuilder();
        missingPerm.setColor(Color.RED);
        missingPerm.addField("❌ **Missing permission**", "You are not permitted to use this command!", false);
        missingPerm.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

        wrongChannel = new EmbedBuilder();
        wrongChannel.setColor(Color.RED);
        wrongChannel.addField("❌ **Wrong channel**", "You have to use this command in a ticket!", false);
        wrongChannel.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getButton().getId() == null) return;
        Ticket ticket;
        switch (event.getButton().getId()) {
            case "ticket-claim" -> {
                if (event.getMember().getRoles().contains(staff)) {
                    if (event.getMessageChannel().getName().contains("ticket-")) {

                        ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                        if (ticket.claim(event.getUser())) {
                            EmbedBuilder builder = new EmbedBuilder();
                            builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
                            builder.setColor(new Color(63, 226, 69, 255));
                            builder.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl());
                            builder.addField("✅ **Ticket claimed**", "Your ticket will be handled by " + event.getUser().getAsMention(), false);

                            event.replyEmbeds(builder.build()).queue();
                        } else {
                            EmbedBuilder builder = new EmbedBuilder();
                            builder.setColor(Color.RED);
                            builder.addField("❌ **Failed claiming**", "You can not claim this ticket!", false);
                            builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                        }
                    } else {
                        event.replyEmbeds(wrongChannel.build()).setEphemeral(true).queue();
                    }
                } else {
                    event.replyEmbeds(missingPerm.build()).setEphemeral(true).queue();
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
                        event.replyEmbeds(wrongChannel.build()).setEphemeral(true).queue();
                    }
                } else {
                    event.replyEmbeds(missingPerm.build()).setEphemeral(true).queue();
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
                        .setPlaceholder("Who do you wanna report?")
                        .setMinLength(2)
                        .setMaxLength(12)
                        .build();
                TextInput reason = TextInput.create("reason", "Reason", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("The reason for reporting")
                        .setMinLength(2)
                        .build();
                Modal modal = Modal.create("report", "Give us more information!")
                        .addActionRows(ActionRow.of(member), ActionRow.of(hacker), ActionRow.of(reason))
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
            case "ticket-nevermind" -> {
                ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                if (ticket.getOwner().equals(event.getUser())) {
                    ticket.closeTicket(true);
                }else {
                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setColor(Color.RED);
                    builder.addField("❌ **Missing access**", "You can not click this button", false);
                    builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
                    event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                }
            }
        }
    }

    @Override
    public void onModalInteraction(@Nonnull ModalInteractionEvent event) {
        Ticket ticket = new Ticket(event.getUser(), jda, dataSource);
        switch (event.getModalId()) {
            case "custom" -> {
                String topic = event.getValue("topic").getAsString();
                if (ticket.createNewTicket("", topic)) {
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            EmbedBuilder builder = new EmbedBuilder();
                            builder.setAuthor(event.getMember().getEffectiveName(), null, event.getMember().getEffectiveAvatarUrl());
                            builder.addField("✅ **Ticket created**", "Successfully created a ticket for you " + ticket.getTicketChannel().getAsMention(), false);
                            builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                            timer.cancel();
                        }
                    }, 1000, 50);
                } else {
                    EmbedBuilder builder = new EmbedBuilder();
                    event.getGuild().getTextChannels().forEach(channel -> {
                        if (channel.getName().contains("ticket-") && channel.getPermissionOverride(event.getMember()).getAllowed().contains(Permission.VIEW_CHANNEL)) {
                            builder.setColor(Color.RED);
                            builder.addField("❌ **Creating ticket failed**", "There is already an opened ticket for you. Please use this instead first or close it -> " + channel.getAsMention(), false);
                            builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
                        }
                    });
                    event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                }
            }
            case "complain" -> {
                String complain = event.getValue("complain").getAsString();
                if (ticket.createNewTicket(complain, "Complain")) {
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            EmbedBuilder builder = new EmbedBuilder();
                            builder.setAuthor(event.getMember().getEffectiveName(), null, event.getMember().getEffectiveAvatarUrl());
                            builder.addField("✅ **Ticket created**", "Successfully created a ticket for you " + ticket.getTicketChannel().getAsMention(), false);
                            builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                            timer.cancel();
                        }
                    }, 1000, 50);
                } else {
                    EmbedBuilder builder = new EmbedBuilder();
                    event.getGuild().getTextChannels().forEach(channel -> {
                        if (channel.getName().contains("ticket-") && channel.getPermissionOverride(event.getMember()).getAllowed().contains(Permission.VIEW_CHANNEL)) {
                            builder.setColor(Color.RED);
                            builder.addField("❌ **Creating ticket failed**", "There is already an opened ticket for you. Please use this instead first or close it -> " + channel.getAsMention(), false);
                            builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
                        }
                    });
                    event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                }
            }
            case "report" -> {
                String name = event.getValue("member").getAsString();
                String report = event.getValue("hacker").getAsString();
                String reason = event.getValue("reason").getAsString();

                if (ticket.createNewTicket(name + " wants to report " + report + "\n\n**Reason:**\n" + reason, name + " wants to report " + report)) {
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            EmbedBuilder builder = new EmbedBuilder();
                            builder.setAuthor(event.getMember().getEffectiveName(), null, event.getMember().getEffectiveAvatarUrl());
                            builder.addField("✅ **Ticket created**", "Successfully created a ticket for you " + ticket.getTicketChannel().getAsMention(), false);
                            builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                            timer.cancel();
                        }
                    }, 1000, 50);
                } else {
                    EmbedBuilder builder = new EmbedBuilder();
                    event.getGuild().getTextChannels().forEach(channel -> {
                        if (channel.getName().contains("ticket-") && channel.getPermissionOverride(event.getMember()).getAllowed().contains(Permission.VIEW_CHANNEL)) {
                            builder.setColor(Color.RED);
                            builder.addField("❌ **Creating ticket failed**", "There is already an opened ticket for you. Please use this instead first or close it -> " + channel.getAsMention(), false);
                            builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
                        }
                    });
                    event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                }
            }
            case "pardon" -> {
                String name = event.getValue("member").getAsString();
                String info = event.getValue("info").getAsString();

                if (ticket.createNewTicket(info, name + " wants pardon")) {
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            EmbedBuilder builder = new EmbedBuilder();
                            builder.setAuthor(event.getMember().getEffectiveName(), null, event.getMember().getEffectiveAvatarUrl());
                            builder.addField("✅ **Ticket created**", "Successfully created a ticket for you " + ticket.getTicketChannel().getAsMention(), false);
                            builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                            timer.cancel();
                        }
                    }, 1000, 50);
                } else {
                    EmbedBuilder builder = new EmbedBuilder();
                    event.getGuild().getTextChannels().forEach(channel -> {
                        if (channel.getName().contains("ticket-") && channel.getPermissionOverride(event.getMember()).getAllowed().contains(Permission.VIEW_CHANNEL)) {
                            builder.setColor(Color.RED);
                            builder.addField("❌ **Creating ticket failed**", "There is already an opened ticket for you. Please use this instead first or close it -> " + channel.getAsMention(), false);
                            builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
                        }
                    });
                    event.replyEmbeds(builder.build()).setEphemeral(true).queue();
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
                        EmbedBuilder builder = new EmbedBuilder();
                        builder.setFooter("Powered by Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
                        builder.addField(new MessageEmbed.Field("**Support request**", """
                                You have questions or a problem?
                                Just click the one of the buttons below or use </ticket create:1007234722805592126> somewhere else.
                                We will try to handle your ticket as soon as possible.
                                """, false));
                        builder.setColor(new Color(63, 226, 69, 255));

                        jda.getTextChannelById(data.baseChannel).sendMessageEmbeds(builder.build())
                                .setActionRow(
                                        Button.primary("ticket-create-pardon", "Pardon"),
                                        Button.primary("ticket-create-report", "Report"),
                                        Button.primary("ticket-create-complain", "Complain"),
                                        Button.primary("ticket-create-custom", "Custom"))
                                .queue();

                        EmbedBuilder builder1 = new EmbedBuilder();
                        builder1.setAuthor(member.getEffectiveName(), null, event.getMember().getEffectiveAvatarUrl());
                        builder1.addField("✅ **Ticket created**", "Successfully setup ticketsystem " + event.getGuild().getTextChannelById(data.baseChannel).getAsMention(), false);
                        builder1.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                        event.replyEmbeds(builder1.build()).setEphemeral(true).queue();
                    } else {
                        event.replyEmbeds(missingPerm.setAuthor(event.getUser().getName(), event.getUser().getEffectiveAvatarUrl()).build()).setEphemeral(true).queue();
                    }
                }
                case "create" -> {
                    Ticket ticket = new Ticket(event.getUser(), jda, dataSource);
                    String topic;
                    if (event.getOption("topic") == null) {
                        topic = "";
                    }else {
                        topic = event.getOption("topic").getAsString();
                    }
                    if (ticket.createNewTicket("", topic)) {
                        Timer timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                EmbedBuilder builder = new EmbedBuilder();
                                builder.setAuthor(member.getEffectiveName(), null, event.getMember().getEffectiveAvatarUrl());
                                builder.addField("✅ **Ticket created**", "Successfully created a ticket for you " + ticket.getTicketChannel().getAsMention(), false);
                                builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                                event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                                timer.cancel();
                            }
                        }, 1000, 50);
                    } else {
                        EmbedBuilder builder = new EmbedBuilder();
                        event.getGuild().getTextChannels().forEach(channel -> {
                            if (channel.getName().contains("ticket-") && channel.getPermissionOverride(event.getMember()).getAllowed().contains(Permission.VIEW_CHANNEL)) {
                                builder.setColor(Color.RED);
                                builder.addField("❌ **Creating ticket failed**", "There is already an opened ticket for you. Please use this instead first or close it -> " + channel.getAsMention(), false);
                                builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
                            }
                        });
                        event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                    }
                }
                case "claim" -> {
                    if (member.getRoles().contains(staff)) {
                        if (event.getMessageChannel().getName().contains("ticket-")) {
                            Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                            if (ticket.claim(event.getUser())) {
                                EmbedBuilder builder = new EmbedBuilder();
                                builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
                                builder.setColor(new Color(63, 226, 69, 255));
                                builder.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl());
                                builder.addField("✅ **Ticket claimed**", "Your ticket will be handled by " + event.getUser().getAsMention(), false);

                                File transcript = new File("./GreevTickets/transcripts/" + ticket.getID() + ".txt");
                                try {
                                    BufferedReader reader = new BufferedReader(new FileReader(transcript));
                                    List<String> lines = reader.lines().toList();
                                    reader.close();
                                    EmbedBuilder builder1 = new EmbedBuilder();
                                    builder1.setColor(new Color(63,226,69,255));
                                    builder1.setDescription("Hello there, " + ticket.getOwner().getAsMention() + "! " + """
                                        A member of staff will assist you shortly.
                                        In the mean time, please describe your issue in as much detail as possible! :)
                                        """);
                                    builder1.addField("Topic", ticket.getTopic(), false);
                                    builder1.setAuthor(ticket.getOwner().getName(),null, ticket.getOwner().getEffectiveAvatarUrl());
                                    builder1.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                                    ticket.getTicketChannel().editMessageEmbedsById(lines.get(0), builder1.build()).setActionRow(Button.danger("ticket-close", "Close")).queue();
                                } catch (IOException e) {
                                    System.out.println("Failed reading File: " + e);
                                }

                                event.replyEmbeds(builder.build()).queue();
                            } else {
                                EmbedBuilder builder = new EmbedBuilder();
                                builder.setColor(Color.RED);
                                builder.addField("❌ **Failed claiming**", "You can not claim this ticket!", false);
                                builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                                event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                            }
                        } else {
                            event.replyEmbeds(wrongChannel.build()).setEphemeral(true).queue();
                        }
                    } else {
                        event.replyEmbeds(missingPerm.build()).setEphemeral(true).queue();
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
                            event.replyEmbeds(wrongChannel.build()).setEphemeral(true).queue();
                        }
                    } else {
                        event.replyEmbeds(missingPerm.build()).setEphemeral(true).queue();
                    }
                }
                case "add" -> {
                    if (member.getRoles().contains(staff)) {
                        if (event.getMessageChannel().getName().contains("ticket-")) {
                            Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                            if (ticket.addUser(event.getOption("member").getAsUser())) {
                                EmbedBuilder builder = new EmbedBuilder();
                                builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
                                builder.setColor(new Color(63, 226, 69, 255));
                                builder.setAuthor(event.getUser().getName(), event.getUser().getEffectiveAvatarUrl());
                                builder.addField("✅ **Member added**", event.getOption("member").getAsUser().getAsMention() + " got added to the ticket", false);

                                event.replyEmbeds(builder.build()).queue();
                            } else {
                                EmbedBuilder builder = new EmbedBuilder();
                                builder.setColor(Color.RED);
                                builder.addField("❌ **Adding member failed**", event.getOption("member").getAsUser().getAsMention() + " is already in the ticket", false);
                                builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                                event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                            }
                        } else {
                            event.replyEmbeds(wrongChannel.build()).setEphemeral(true).queue();
                        }
                    } else {
                        event.replyEmbeds(missingPerm.build()).setEphemeral(true).queue();
                    }
                }
                case "remove" -> {
                    if (member.getRoles().contains(staff)) {
                        if (event.getMessageChannel().getName().contains("ticket-")) {
                            Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                            if (ticket.removeUser(event.getOption("member").getAsUser())) {
                                EmbedBuilder builder = new EmbedBuilder();
                                builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
                                builder.setColor(new Color(63, 226, 69, 255));
                                builder.setAuthor(event.getUser().getName(), event.getUser().getEffectiveAvatarUrl());
                                builder.addField("✅ **Member removed**", event.getOption("member").getAsUser().getAsMention() + " got removed from the ticket", false);

                                event.replyEmbeds(builder.build()).queue();
                            } else {
                                EmbedBuilder builder = new EmbedBuilder();
                                builder.setColor(Color.RED);
                                builder.addField("❌ **Removing member failed**", event.getOption("member").getAsUser().getAsMention() + " is already not in the ticket", false);
                                builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                                event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                            }
                        } else {
                            event.replyEmbeds(wrongChannel.build()).setEphemeral(true).queue();
                        }
                    } else {
                        event.replyEmbeds(missingPerm.build()).setEphemeral(true).queue();
                    }
                }
                case "set-supporter" -> {
                    if (member.getRoles().contains(staff)) {
                        if (event.getMessageChannel().getName().contains("ticket-")) {
                            Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                            if (ticket.setSupporter(event.getOption("staff").getAsUser())) {
                                EmbedBuilder builder = new EmbedBuilder();
                                builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
                                builder.setColor(new Color(63, 226, 69, 255));
                                builder.setAuthor(event.getUser().getName(), event.getUser().getEffectiveAvatarUrl());
                                builder.addField("✅ **New supporter**", event.getOption("staff").getAsUser().getAsMention() + " is the new supporter", false);

                                event.replyEmbeds(builder.build()).queue();
                            } else {
                                EmbedBuilder builder = new EmbedBuilder();
                                builder.setColor(Color.RED);
                                builder.addField("❌ **Setting new supporter failed**", "This member is either already the supporter or not a staff member", false);
                                builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                                event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                            }
                        } else {
                            event.replyEmbeds(wrongChannel.build()).setEphemeral(true).queue();
                        }
                    } else {
                        event.replyEmbeds(missingPerm.build()).setEphemeral(true).queue();
                    }
                }
                case "set-owner" -> {
                    if (member.getRoles().contains(staff)) {
                        if (event.getMessageChannel().getName().contains("ticket-")) {
                            Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                            if (!ticket.hasAccess(member)) {
                                EmbedBuilder builder = new EmbedBuilder();
                                builder.setColor(Color.RED);
                                builder.addField("❌ **Setting new owner failed**", "This user has not access to this channel", false);
                                builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                                event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                                return;
                            }

                            if (ticket.setOwner(event.getOption("member").getAsUser())) {
                                EmbedBuilder builder = new EmbedBuilder();
                                builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
                                builder.setColor(new Color(63, 226, 69, 255));
                                builder.setAuthor(event.getUser().getName(), event.getUser().getEffectiveAvatarUrl());
                                builder.addField("✅ **New owner**", event.getOption("member").getAsUser().getAsMention() + " is now the new owner of the ticket", false);

                                event.replyEmbeds(builder.build()).queue();
                            } else {
                                EmbedBuilder builder = new EmbedBuilder();
                                builder.setColor(Color.RED);
                                builder.addField("❌ **Setting new owner failed**", "This member is already the creator", false);
                                builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                                event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                            }
                        } else {
                            event.replyEmbeds(wrongChannel.build()).setEphemeral(true).queue();
                        }
                    } else {
                        event.replyEmbeds(missingPerm.build()).setEphemeral(true).queue();
                    }
                }
                case "set-waiting" -> {
                    if (member.getRoles().contains(staff)) {
                        if (event.getMessageChannel().getName().contains("ticket-")) {
                            Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                            if (!ticket.isWaiting()) {
                                ticket.toggleWaiting(true);
                                EmbedBuilder builder = new EmbedBuilder();
                                builder.setAuthor(member.getEffectiveName(), null, member.getEffectiveAvatarUrl());
                                builder.setDescription("Waiting for response.");
                                builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
                                builder.setColor(new Color(63, 226, 69, 255));
                                event.replyEmbeds(builder.build()).queue();
                            }else {
                                EmbedBuilder builder = new EmbedBuilder();
                                builder.setColor(Color.RED);
                                builder.addField("❌ **Changing waiting mode failed**", "This ticket is already in waiting mode!", false);
                                builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                                event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                            }
                        } else {
                            event.replyEmbeds(wrongChannel.build()).setEphemeral(true).queue();
                        }
                    } else {
                        event.replyEmbeds(missingPerm.build()).setEphemeral(true).queue();
                    }
                }
                case "set-topic" -> {
                    if (member.getRoles().contains(staff)) {
                        if (event.getMessageChannel().getName().contains("ticket-")) {
                            Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda, dataSource);
                            ticket.setTopic(event.getOption("topic").getAsString());

                            EmbedBuilder builder = new EmbedBuilder();
                            builder.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");
                            builder.setColor(new Color(63, 226, 69, 255));
                            builder.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl());
                            builder.addField("✅ **New Topic**", "Changed topic to '" + event.getOption("topic").getAsString() + "'", false);

                            File transcript = new File("./GreevTickets/transcripts/" + ticket.getID() + ".txt");
                            try {
                                BufferedReader reader = new BufferedReader(new FileReader(transcript));
                                List<String> lines = reader.lines().toList();
                                reader.close();
                                EmbedBuilder builder1 = new EmbedBuilder();
                                builder1.setColor(new Color(63,226,69,255));
                                builder1.setDescription("Hello there, " + ticket.getOwner().getAsMention() + "! " + """
                                        A member of staff will assist you shortly.
                                        In the mean time, please describe your issue in as much detail as possible! :)
                                        """);
                                builder1.addField("Topic", ticket.getTopic(), false);
                                builder1.setAuthor(ticket.getOwner().getName(),null, ticket.getOwner().getEffectiveAvatarUrl());
                                builder1.setFooter("Greev.eu", "https://cdn.pluoi.com/greev/logo-clear.png");

                                if (ticket.isClaimed()) {
                                    ticket.getTicketChannel().editMessageEmbedsById(lines.get(0), builder1.build()).setActionRow(Button.danger("ticket-close", "Close")).queue();
                                }else {
                                    ticket.getTicketChannel().editMessageEmbedsById(lines.get(0), builder1.build()).setActionRow(Button.primary("ticket-claim", "Claim"),
                                            Button.danger("ticket-close", "Close")).queue();
                                }
                            } catch (IOException e) {
                                System.out.println("Failed reading File: " + e);
                            }
                            event.replyEmbeds(builder.build()).queue();
                        } else {
                            event.replyEmbeds(wrongChannel.build()).setEphemeral(true).queue();
                        }
                    } else {
                        event.replyEmbeds(missingPerm.build()).setEphemeral(true).queue();
                    }
                }
            }
        }
    }

    /*
     *Methods to write the transcript
     */
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.isFromGuild() && event.getChannelType().equals(ChannelType.TEXT) && event.getChannel().getName().contains("ticket-") && !event.getAuthor().isBot()) {
            //Handling Waiting stage
            Ticket ticket = new Ticket(event.getChannel().getIdLong(), jda, dataSource);
            if (ticket.isWaiting()) {
                ticket.toggleWaiting(false);
            }

            File transcript = ticket.getTranscript();
            String log = event.getMessageId() + "} "
                    + new SimpleDateFormat("[hh:mm:ss a '|' dd'th' MMM yyyy] ").format(new Date(System.currentTimeMillis()))
                    + "[" + event.getMember().getEffectiveName() + "#" + event.getMember().getUser().getDiscriminator() + "]"
                    + ":>>> " + event.getMessage().getContentDisplay();
            try {
                try {
                    new File("./GreevTickets/transcripts").mkdirs();
                    if (transcript.createNewFile()) {
                        BufferedWriter writer = new BufferedWriter(new FileWriter(transcript, true));
                        writer.write("Transcript of ticket #" + ticket.getID());
                        writer.newLine();
                        writer.close();
                    }
                } catch (IOException e) {
                    System.out.println("Could not create transcript");
                }

                BufferedWriter writer = new BufferedWriter(new FileWriter(transcript, true));
                writer.write(log);
                writer.newLine();
                writer.close();
            } catch (IOException e) {
                System.out.println("Could not write in file: " + e);
            }
        }
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        if (event.isFromGuild() && event.getChannelType().equals(ChannelType.TEXT) && event.getChannel().getName().contains("ticket-")) {
            Ticket ticket = new Ticket(event.getChannel().getIdLong(), jda, dataSource);
            File transcript = ticket.getTranscript();
            File temp = new File("./GreevTickets/transcripts/" + ticket.getID() + ".temp");

            try {
                temp.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(temp, true));
                BufferedReader reader = new BufferedReader(new FileReader(transcript));
                List<String> lines = reader.lines().toList();
                reader.close();

                for (String line : lines) {
                    if (line.contains(event.getMessageId())) {
                        String log = null;
                        try {
                            log = line.replace(":>>> " + line.split(":>>> ")[1], ":>>> ~~" + line.split(":>>> ")[1] + "~~");
                        } catch (ArrayIndexOutOfBoundsException e) {
                            return;
                        }

                        writer.write(log);
                        writer.newLine();
                    } else {
                        writer.write(line);
                        writer.newLine();
                    }
                }
                writer.close();
                transcript.delete();
                temp.renameTo(transcript);
            } catch (IOException e) {
                System.out.println("Could not read file: ");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        if (event.isFromGuild() && event.getChannelType().equals(ChannelType.TEXT) && event.getChannel().getName().contains("ticket-") && !event.getAuthor().isBot()) {
            Ticket ticket = new Ticket(event.getChannel().getIdLong(), jda, dataSource);
            File transcript = ticket.getTranscript();
            File temp = new File("./GreevTickets/transcripts/" + ticket.getID() + ".temp");

            try {
                temp.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(temp, true));
                BufferedReader reader = new BufferedReader(new FileReader(transcript));
                List<String> lines = reader.lines().toList();
                reader.close();
                String msgID = event.getMessage().getId();
                int edits = 1;

                for (String line : lines) {
                    if (line.split("}")[0].equals(msgID)) {
                        if (line.split("~edit-").length > 1) {
                            edits += Integer.parseInt(line.split("~edit-")[line.split("~edit-").length - 1].substring(0, 1));
                            System.out.println(edits);
                        }
                        String message;
                        if (edits == 1) {
                            System.out.println(1);
                            message = "~original~: " + line.split(":>>> ")[1] + " | ~edit-1~:>> " + event.getMessage().getContentDisplay();
                        } else {
                            System.out.println(2);
                            message = line.split("~edit-" + edits)[0].split(":>>> ")[1] + "  ~edit-" + edits + "~:>> " + event.getMessage().getContentDisplay();
                        }
                        writer.write(line.split(":>>> ")[0] + ":>>> " + message);
                        writer.newLine();
                    } else {
                        writer.write(line);
                        writer.newLine();
                    }
                }
                writer.close();
                transcript.delete();
                temp.renameTo(transcript);
            } catch (IOException e) {
                System.out.println("Could not read file: ");
                e.printStackTrace();
            }
        }
    }
}