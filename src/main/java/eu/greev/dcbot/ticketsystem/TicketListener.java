package eu.greev.dcbot.ticketsystem;

import eu.greev.dcbot.utils.data.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class TicketListener extends ListenerAdapter {
    //    private TicketManager ticketManager;
    private final long serverID = new Data().testID;
    private final Role staff;
    private final JDA jda;

    public TicketListener(JDA jda) {
        this.jda = jda;
        staff = jda.getGuildById(serverID).getRoleById(new Data().teamID);
    }

    /*
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getButton().getId() == null) return;
        if (event.getChannelType().equals(ChannelType.TEXT)) {
            ticketManager = new TicketManager(jda);
            if (event.getButton().getId().equals("ticket-base")) {
                ticketManager.createNewTicket(event);
            }else if (event.getButton().getId().equals("ticket-claim")) {
                ticketManager.claimTicket(event);
            }else if (event.getButton().getId().equals("ticket-close")) {
                ticketManager.confirmClosing(event);
            }else if (event.getButton().getId().equals("ticket-confirm")) {
                ticketManager.closeTicket(event);
            }else if (event.getButton().getId().equals("ticket-close-reason")) {
                TextInput reason = TextInput.create("reason", "Reason", TextInputStyle.SHORT)
                        .setPlaceholder("The reason for closing this ticket.")
                        .setMinLength(1)
                        .setMaxLength(50)
                        .build();
                Modal modal = Modal.create("close", "Close")
                        .addActionRows(ActionRow.of(reason))
                        .build();
                event.replyModal(modal).queue();
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getChannelType().equals(ChannelType.TEXT)) {
            ticketManager = new TicketManager(jda);
            if (event.getName().equals("ticket-setup")) {
                ticketManager.setupTicketSystem();
                event.reply("Das das Ticket-System wurde erfolgreich aktiviert").setEphemeral(true).queue();
            }else if (event.getName().equals("ticket-add")) {
                if (!ticketManager.getSupportersIDs().contains(event.getMember().getId())) {
                    if (event.getChannel().getName().contains("-ticket")) {
                        ticketManager.addMemberToTicket(event.getOption("member").getAsMember(), event);
                    } else {
                        event.reply("Führe diesen Befehl in einem Ticket aus!").setEphemeral(true).queue();
                    }
                }else {
                    event.reply("Du hast keine Rechte um diesen Befehl zu nutzen").setEphemeral(true).queue();
                }
            }else if (event.getName().equals("ticket-create")) {
                ticketManager.createNewTicket(event);
                if (jda.getGuildById(serverID).getTextChannelsByName(event.getMember().getEffectiveName().toLowerCase() + "-ticket", false).size() == 0) {
                    System.out.println("");
                }else if (jda.getGuildById(serverID).getTextChannelsByName(event.getMember().getEffectiveName().toLowerCase() + "-ticket", false).get(0) == null) {
                    event.reply("Du hast erfolgreich ein Ticket erstellt").setEphemeral(true).queue();
                }
            }else if (event.getName().equals("ticket-close")) {
                if (event.getChannel().getName().contains("-ticket")) {
                    if (event.getOption("reason") == null) {
                        ticketManager.confirmClosing(event);
                    }else {
                        ticketManager.closeTicketWithReason(event.getOption("reason").getAsString(), event);
                    }
                }else {
                    event.reply("Führe diesen Befehl in einem Ticket aus!").setEphemeral(true).queue();
                }
            }else if (event.getName().equals("ticket-claim")) {
                if (event.getChannel().getName().contains("-ticket")) {
                    ticketManager.claimTicket(event);
                }else {
                    event.reply("Führe diesen Befehl in einem Ticket aus!").setEphemeral(true).queue();
                }
            }else if (event.getName().equals("ticket-config-clear")) {
                if (event.getMember().getPermissions().contains(Permission.ADMINISTRATOR)) {
                    YamlFile file = Main.getTicketYAML();
                    try {
                        file.deleteFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Main.createTicketYAML();
                    event.reply("Die Ticket-Config wurde zurückgesetzt.").setEphemeral(true).queue();
                }else {
                    event.reply("Du hast hier keine Rechte, du SACK!").setEphemeral(true).queue();
                }
            }else if (event.getName().equals("ticket-remove")) {
                if (!ticketManager.getSupportersIDs().contains(event.getMember().getId())) {
                    if (event.getChannel().getName().contains("-ticket")) {
                        ticketManager.removeMemberFromTicket(event.getOption("member").getAsMember(), event);
                    } else {
                        event.reply("Führe diesen Befehl in einem Ticket aus!").setEphemeral(true).queue();
                    }
                }else {
                    event.reply("Du hast keine Rechte um diesen Befehl zu nutzen").setEphemeral(true).queue();
                }
            }
        }
    }

    @Override
    public void onModalInteraction(@Nonnull ModalInteractionEvent event) {
        if (event.getModalId().equals("close")) {
            String reason = event.getValue("reason").getAsString();
            ticketManager.closeTicketWithReason(reason, event);
            event.reply("Successfully closed with reason: " + reason).setEphemeral(true).queue();
        }
    }*/

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("ticket")) {
            if (event.getSubcommandName().equals("help")) {
                EmbedBuilder builder = new EmbedBuilder().setTitle("Commands").setColor(new Color(0, 153, 153, 255));
                builder.setFooter("Greev.eu", "https://cdn.discordapp.com/emojis/355825850152779786.webp?size=96&quality=lossless");
                builder.setDescription("The commands you have access to are listed below");
                builder.addField("", "**/ticket add <Member> ·** Add a member to a ticket channel", false);
                builder.addField("", "**/ticket remove <Member> ·** Remove a member from a ticket channel", false);
                builder.addField("", "**/ticket claim ·** Claim the ticket", false);
                builder.addField("", "**/ticket close ·** Close the ticket", false);
                builder.addField("", "**/ticket supporter <Staff> ·** Set the new supporter", false);
                builder.addField("", "**/ticket topic ·** Set the topic of the ticket", false);
                builder.addField("", "Please contact a member of staff if you require assistance.", false);

                event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            }else if (event.getSubcommandName().equals("setup")) {
                EmbedBuilder builder = new EmbedBuilder();
                builder.setFooter("Powered by Greev.eu", "https://cdn.discordapp.com/emojis/355825850152779786.webp?size=96&quality=lossless");
                builder.addField(new MessageEmbed.Field("**Support request**", """
                You have questions or a problem?
                Just click the button below or use `/ticket create` somewhere else.
                We will try to handle your ticket as soon as possible.
                """, false));
                builder.setColor(new Color(63,226,69,255));
                jda.getTextChannelById(new Data().baseChannel).sendMessageEmbeds(builder.build())
                        .setActionRow(Button.primary("ticket-base", "Create Ticket").withEmoji(Emoji.fromUnicode("\uD83D\uDCE9")))
                        .queue();
            }else if (event.getSubcommandName().equals("create")) {
                Ticket ticket = new Ticket(event.getUser(), jda);
                if (ticket.createNewTicket()) {
                    event.reply("Successfully created ticket -> " + ticket.getTicketChannel().getAsMention()).setEphemeral(true).queue();
                }else {
                    event.reply("There already is an opened ticket for you. Please use this instead first or close it -> " + ticket.getTicketChannel().getAsMention()).setEphemeral(true).queue();
                }
            }else if (event.getSubcommandName().equals("supporter")) {
                Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda);
                if (ticket.setSupporter(event.getOption("staff").getAsUser())) {
                    event.reply(event.getOption("staff").getAsUser().getAsMention() + " is now the new supporter").queue();
                }else {
                    event.reply("This member is either already the supporter or not a staff member").queue();
                }
            }else if (event.getSubcommandName().equals("remove")) {
                Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda);
                if (event.getMessageChannel().getName().contains("-ticket")) {
                    if (ticket.removeUser(event.getOption("member").getAsUser())) {
                        event.reply(event.getOption("member").getAsUser().getAsMention() + " got removed from the ticket").queue();
                    } else {
                        event.reply(event.getOption("member").getAsUser().getAsMention() + " is already not in the ticket").setEphemeral(true).queue();
                    }
                }else {
                    event.reply("You have to use this command in a ticket!").setEphemeral(true).queue();
                }
            }else if (event.getSubcommandName().equals("add")) {
                Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda);
                if (event.getMessageChannel().getName().contains("-ticket")) {
                    if (ticket.addUser(event.getOption("member").getAsUser())) {
                        event.reply(event.getOption("member").getAsUser().getAsMention() + " got added to the ticket").queue();
                    } else {
                        event.reply(event.getOption("member").getAsUser().getAsMention() + " is already in the ticket").setEphemeral(true).queue();
                    }
                }else {
                    event.reply("You have to use this command in a ticket!").setEphemeral(true).queue();
                }
            }else if (event.getSubcommandName().equals("claim")) {
                Ticket ticket = new Ticket(event.getMessageChannel().getIdLong(), jda);
                if (event.getMessageChannel().getName().contains("-ticket")) {
                    if (ticket.setSupporter(event.getMember().getUser())) {
                        event.reply(event.getMember().getAsMention() + " claimed the ticket!").queue();
                    }else {
                        event.reply("This member is either already the supporter or not a staff member").queue();
                    }
                }else {
                    event.reply("You have to use this command in a ticket!").setEphemeral(true).queue();
                }
            }
        }
    }
}