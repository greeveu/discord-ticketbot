package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.utils.Constants;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;

@AllArgsConstructor
public class Setup extends AbstractCommand {
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
        EmbedBuilder builder = new EmbedBuilder().setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO)
                .setColor(Constants.GREEV_GREEN)
                .addField(new MessageEmbed.Field("**Support request**", """
                        You have questions or a problem?
                        Just click the one of the buttons below or use </ticket create:1013502082629767238> somewhere else.
                        We will try to handle your ticket as soon as possible.
                        """, false));

        SelectMenu.Builder selectionBuilder = SelectMenu.create("ticket-create-topic")
                .setPlaceholder("Select your ticket topic")
                .addOption("Report a bug","select-bug","Bugs can be annoying. Better call the exterminator.")
                .addOption("Application", "select-application", "The place for Applications and Questions about it.")
                .addOption( "Write a ban- or muteappeal","select-pardon","Got muted or banned for no reason?")
                .addOption("Your own topic","select-custom","You have another reason for opening the ticket? Specify!");

        jda.getTextChannelById(Constants.BASE_CHANNEL).sendMessageEmbeds(builder.build())
                .setActionRow(selectionBuilder.build())
                .queue();

        EmbedBuilder builder1 = new EmbedBuilder().setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO)
                .setAuthor(member.getEffectiveName(), null, event.getMember().getEffectiveAvatarUrl())
                .addField("✅ **Ticket created**", "Successfully setup ticketsystem " + event.getGuild().getTextChannelById(Constants.BASE_CHANNEL).getAsMention(), false);

        event.replyEmbeds(builder1.build()).setEphemeral(true).queue();
    }
}
