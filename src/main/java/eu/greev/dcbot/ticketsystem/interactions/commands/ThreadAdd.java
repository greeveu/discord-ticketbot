package eu.greev.dcbot.ticketsystem.interactions.commands;

import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

public class ThreadAdd extends AbstractCommand {
    private final EmbedBuilder wrongChannel;

    public ThreadAdd(Config config, TicketService ticketService, EmbedBuilder wrongChannel, EmbedBuilder missingPerm, JDA jda) {
        super(config, ticketService, missingPerm, jda);
        this.wrongChannel = wrongChannel;
    }

    @Override
    public void execute(Event evt) {
        SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) evt;
        if (!event.getMember().getRoles().contains(jda.getRoleById(config.getStaffId()))) {
            event.replyEmbeds(missingPerm.setFooter(config.getServerName(), config.getServerLogo()).build()).setEphemeral(true).queue();
            return;
        }
        if (event.getChannelType() != ChannelType.GUILD_PRIVATE_THREAD
                || ticketService.getTicketByChannelId(event.getGuildChannel().asThreadChannel().getParentMessageChannel().getIdLong()) == null) {
            event.replyEmbeds(wrongChannel
                            .setFooter(config.getServerName(), config.getServerLogo())
                            .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                            .clearFields()
                            .addField("❌ **Wrong channel**", "You have to use this command in a ticket thread!", false)
                            .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        ThreadChannel thread = event.getGuildChannel().asThreadChannel();
        Member member = event.getOption("staff").getAsMember();

        if (!member.getRoles().contains(jda.getRoleById(config.getStaffId()))) {
            event.replyEmbeds(new EmbedBuilder().setFooter(config.getServerName(), config.getServerLogo())
                    .setColor(Color.RED)
                    .addField("❌ **Adding staff failed**", "The given member is not part of the team", false).build()).setEphemeral(true).queue();
            return;
        }

        if (thread.getMembers().contains(member)) {
            EmbedBuilder builder = new EmbedBuilder().setColor(Color.RED)
                    .setFooter(config.getServerName(), config.getServerLogo())
                    .addField("❌ **Adding staff failed**", "This staff member is already in the ticket thread", false);

            event.replyEmbeds(builder.build()).setEphemeral(true).queue();
            return;
        }

        thread.addThreadMember(member).queue();
        event.replyEmbeds(new EmbedBuilder().setColor(Color.decode(config.getColor()))
                        .setFooter(config.getServerName(), config.getServerLogo())
                        .addField("✅ **Member added**", member.getAsMention() + " got added to the ticket thread", false)
                        .build())
                .setEphemeral(true)
                .queue();
    }
}
