package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.utils.Config;
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
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;

import java.awt.*;

@AllArgsConstructor
@Slf4j
public class Setup extends AbstractCommand {
    private final Config config;
    private final EmbedBuilder missingPerm;
    private final JDA jda;

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
        } else if (!(event.getOption("support-category").getAsChannel() instanceof Category)) {
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
        long supportCategory = event.getOption("support-category").getAsChannel().getIdLong();

        config.setServerName(serverName);
        config.setServerLogo(serverLogo);
        config.setServerId(serverId);
        config.setSupportCategory(supportCategory);
        config.setBaseChannel(baseChannel.getIdLong());
        config.setStaffId(staffId);

        config.dumpConfig("./Tickets/config.yml");

        EmbedBuilder builder = new EmbedBuilder().setFooter(config.getServerName(), config.getServerLogo())
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