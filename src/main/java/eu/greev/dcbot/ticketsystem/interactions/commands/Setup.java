package eu.greev.dcbot.ticketsystem.interactions.commands;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import org.simpleyaml.configuration.file.YamlFile;

import java.awt.*;
import java.io.IOException;

@AllArgsConstructor
@Slf4j
public class Setup extends AbstractCommand {
    private final EmbedBuilder missingPerm;
    private final JDA jda;
    private final YamlFile config;

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        Member member = event.getMember();
        if (!member.getPermissions().contains(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(missingPerm.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl()).build()).setEphemeral(true).queue();
            return;
        }
        String serverName = event.getGuild().getName();
        String serverLogo = event.getGuild().getIconUrl();
        long serverId = event.getGuild().getIdLong();
        long staffId = event.getOption("staff").getAsRole().getIdLong();

        if (!(event.getOption("base-channel").getAsChannel() instanceof TextChannel)) {
            event.replyEmbeds(new EmbedBuilder()
                    .setColor(Color.RED)
                    .setFooter(serverName, serverLogo)
                    .addField("❌ **Ticket setup failed**", "Option 'channel' has to be a valid text channel", false)
                    .build())
                    .setEphemeral(true)
                    .queue();
            return;
        } else if (!(event.getOption("support-category").getAsChannel() instanceof Category)) {
            event.replyEmbeds(new EmbedBuilder()
                            .setColor(Color.RED)
                            .setFooter(serverName, serverLogo)
                            .addField("❌ **Ticket setup failed**", "Option 'category' has to be a valid category", false)
                            .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }
        TextChannel baseChannel = event.getOption("base-channel").getAsChannel().asTextChannel();
        long supportCategory = event.getOption("support-category").getAsChannel().getIdLong();

        config.set("data.serverName", serverName);
        config.set("data.serverLogo", serverLogo);
        config.set("data.serverId", serverId);
        config.set("data.supportCategory", supportCategory);
        config.set("data.baseChannel", baseChannel.getIdLong());
        config.set("data.staffId", staffId);

        Color color = new Color(63, 226, 69, 255);
        if (event.getOption("color") == null) {
            config.set("data.color", "GREEV_GREEN");
        } else {
            switch (event.getOption("color").getAsString()) {
                case "BLACK" -> {
                    color = Color.BLACK;
                    config.set("data.color", "BLACK");
                }
                case "BLUE" -> {
                    color = Color.BLUE;
                    config.set("data.color", "BLUE");
                }
                case "CYAN" -> {
                    color = Color.CYAN;
                    config.set("data.color", "CYAN");
                }
                case "DARK_GRAY" -> {
                    color = Color.DARK_GRAY;
                    config.set("data.color", "DARK_GRAY");
                }
                case "GRAY" -> {
                    color = Color.GRAY;
                    config.set("data.color", "GRAY");
                }
                case "GREEN" -> {
                    color = Color.GREEN;
                    config.set("data.color", "GREEN");
                }
                case "LIGHT_GRAY" -> {
                    color = Color.LIGHT_GRAY;
                    config.set("data.color", "LIGHT_GRAY");
                }
                case "MAGENTA" -> {
                    color = Color.MAGENTA;
                    config.set("data.color", "MAGENTA");
                }
                case "ORANGE" -> {
                    color = Color.ORANGE;
                    config.set("data.color", "ORANGE");
                }
                case "PINK" -> {
                    color = Color.PINK;
                    config.set("data.color", "PINK");
                }
                case "RED" -> {
                    color = Color.RED;
                    config.set("data.color", "RED");
                }
                case "WHITE" -> {
                    color = Color.WHITE;
                    config.set("data.color", "WHITE");
                }
                case "YELLOW" -> {
                    color = Color.YELLOW;
                    config.set("data.color", "YELLOW");
                }
            }
        }
        try {
            config.save();
        } catch (IOException e) {
            log.error("Failed saving config", e);
        }

        EmbedBuilder builder = new EmbedBuilder().setFooter(config.getString("data.serverName"), config.getString("data.serverLogo"))
                .setColor(color)
                .addField(new MessageEmbed.Field("**Support request**", """
                        You have questions or a problem?
                        Just click the one of the buttons below or use </ticket create:0> somewhere else.
                        We will try to handle your ticket as soon as possible.
                        """, false));

        SelectMenu.Builder selectionBuilder = SelectMenu.create("ticket-create-topic")
                .setPlaceholder("Select your ticket topic")
                .addOption("Report a bug","select-bug","Bugs can be annoying. Better call the exterminator.")
                .addOption("Application", "select-application", "The place for Applications and Questions about it.")
                .addOption( "Write a ban- or mute appeal","select-pardon","Got muted or banned for no reason?")
                .addOption("Your own topic","select-custom","You have another reason for opening the ticket? Specify!");

        baseChannel.sendMessageEmbeds(builder.build())
                .setActionRow(selectionBuilder.build())
                .queue();

        EmbedBuilder builder1 = new EmbedBuilder().setFooter(serverName, serverLogo)
                .setColor(color)
                .setAuthor(member.getEffectiveName(), null, event.getMember().getEffectiveAvatarUrl())
                .addField("✅ **Ticket created**", "Successfully setup ticketsystem " + baseChannel.getAsMention(), false);

        event.replyEmbeds(builder1.build()).setEphemeral(true).queue();
    }
}
