package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.Main;
import eu.greev.dcbot.ticketsystem.categories.ICategory;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
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
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

@Slf4j
public class Setup extends AbstractCommand {

    public Setup(Config config, TicketService ticketService, EmbedBuilder missingPerm, JDA jda) {
        super(config, ticketService, missingPerm, jda);
    }

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

        EmbedBuilder error = new EmbedBuilder()
                .setColor(Color.RED)
                .setFooter(serverName, serverLogo);

        if (!(event.getOption("base-channel").getAsChannel() instanceof TextChannel)) {
            event.replyEmbeds(error.addField("❌ **Ticket setup failed**", "Option 'channel' has to be a valid text channel", false)
                    .build())
                    .setEphemeral(true)
                    .queue();
            return;
        } else if (!(event.getOption("unclaimed-category").getAsChannel() instanceof Category)) {
            event.replyEmbeds(error.addField("❌ **Ticket setup failed**", "Option 'category' has to be a valid category", false)
                            .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        //#3fe245
        Color color = new Color(63, 226, 69, 255);
        OptionMapping clr = event.getOption("color");
        if (clr == null) {
            config.setColor("#3fe245");
        } else {
            try {
                color = Color.decode(clr.getAsString());
            } catch (NumberFormatException e) {
                event.replyEmbeds(error.addField("❌ **Ticket setup failed**", "Option 'color' has to be a hex code", false)
                        .build())
                        .setEphemeral(true)
                        .queue();
                return;
            }
            config.setColor(clr.getAsString());
        }

        TextChannel baseChannel = event.getOption("base-channel").getAsChannel().asTextChannel();
        long supportCategory = event.getOption("unclaimed-category").getAsChannel().getIdLong();

        config.setServerName(serverName);
        config.setServerLogo(serverLogo);
        config.setServerId(serverId);
        config.setUnclaimedCategory(supportCategory);
        config.setBaseChannel(baseChannel.getIdLong());
        config.setStaffId(staffId);
        config.setAddToTicketThread(new ArrayList<>());

        config.dumpConfig("./Tickets/config.yml");

        try {
            event.getGuild().getTextChannelById(config.getBaseChannel()).getIterableHistory()
                    .takeAsync(1000)
                    .get()
                    .forEach(m -> m.delete().queue());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Could not delete messages", e);
        }

        EmbedBuilder builder = new EmbedBuilder().setFooter(config.getServerName(), config.getServerLogo())
                .setColor(color)
                .addField(new MessageEmbed.Field("**Support request**", """
                        You have questions or a problem?
                        Just click the one of the buttons below.
                        We will try to handle your ticket as soon as possible.
                        """, false));

        StringSelectMenu.Builder selectionBuilder = StringSelectMenu.create("ticket-create-topic")
                .setPlaceholder("Select your ticket topic");

        for (ICategory category : Main.CATEGORIES) {
            selectionBuilder.addOption(category.getLabel(), "select-" + category.getId(), category.getDescription());
        }

        baseChannel.sendMessageEmbeds(builder.build())
                .setActionRow(selectionBuilder.build())
                .queue();

        EmbedBuilder builder1 = new EmbedBuilder().setFooter(serverName, serverLogo)
                .setColor(color)
                .setAuthor(member.getEffectiveName(), null, event.getMember().getEffectiveAvatarUrl())
                .addField("✅ **Ticket created**", "Successfully setup ticketsystem.\nDon't forget to add the ticket category ids to the config! " + baseChannel.getAsMention(), false);

        event.replyEmbeds(builder1.build()).setEphemeral(true).queue();
    }
}