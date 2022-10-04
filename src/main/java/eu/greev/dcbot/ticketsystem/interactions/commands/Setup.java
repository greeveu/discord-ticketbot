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
        if (member.getPermissions().contains(Permission.ADMINISTRATOR)) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO);
            builder.addField(new MessageEmbed.Field("**Support request**", """
                                You have questions or a problem?
                                Just click the one of the buttons below or use </ticket create:1013502082629767238> somewhere else.
                                We will try to handle your ticket as soon as possible.
                                """, false));
            builder.setColor(Constants.GREEV_GREEN);

            SelectMenu.Builder selectionBuilder = SelectMenu.create("test");
            selectionBuilder.setPlaceholder("Select your ticket topic")
                    .addOption("select-report", "Report a rulebreaker", "Found someone who broke the rules? Report them here!")
                    .addOption("select-complain", "Submit a complain", "You have a complain? Don't wait to tell us more!")
                    .addOption("select-pardon", "Write a ban- or muteappeal", "Got muted or banned for no reason?")
                    .addOption("select-bug", "Report a bug", "Bugs can be annoying. Better call the exterminator")
                    .addOption("select-question", "Pose a question", "Something is unclear? Don't hesitate to ask!")
                    .addOption("select-custom", "Your own topic", "You have another reason for opening the ticket? Specify!");

            jda.getTextChannelById(Constants.BASE_CHANNEL).sendMessageEmbeds(builder.build())
                    .setActionRow(selectionBuilder.build())
                    .queue();

            EmbedBuilder builder1 = new EmbedBuilder();
            builder1.setAuthor(member.getEffectiveName(), null, event.getMember().getEffectiveAvatarUrl());
            builder1.addField("✅ **Ticket created**", "Successfully setup ticketsystem " + event.getGuild().getTextChannelById(Constants.BASE_CHANNEL).getAsMention(), false);
            builder1.setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO);

            event.replyEmbeds(builder1.build()).setEphemeral(true).queue();
        } else {
            event.replyEmbeds(missingPerm.setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl()).build()).setEphemeral(true).queue();
        }
    }
}
