package eu.greev.dcbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.List;

public class Main extends ListenerAdapter {
    public static void main(String[] args) throws InterruptedException, LoginException {
        JDA jda = JDABuilder.createLight("", GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_EMOJIS_AND_STICKERS, GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(new Main())
                .setActivity(Activity.listening(" to ticket commands."))
                .setStatus(OnlineStatus.ONLINE)
                .build();
        jda.awaitReady();

        List<CommandData> commands = new ArrayList<>();
        createCommands(commands);
        jda.getGuildById(301471248393830413L).updateCommands().addCommands(commands).queue();
    }

    private static void createCommands(@NotNull List<CommandData> commands) {
        //Temporary
        commands.add(Commands.slash("ticket", "Manage the ticket system")
                .addSubcommands(new SubcommandData("setup", "Setup the System"))
                .addSubcommands(new SubcommandData("add", "Add a User to this ticket")
                        .addOption(OptionType.USER,"member", "The user adding to the current ticket", true))
                .addSubcommands(new SubcommandData("remove", "Remove a User from this ticket")
                        .addOption(OptionType.USER,"member", "The user removing from the current ticket", true))
                .addSubcommands(new SubcommandData("create", "Create a new Ticket for you"))
                .addSubcommands(new SubcommandData("close", "Close this ticket")
                        .addOption(OptionType.STRING, "reason", "The reason the ticket was closed", false))
                .addSubcommands(new SubcommandData("claim", "Claim this ticket"))
                .addSubcommands(new SubcommandData("clear-config", "Clear the config for the TicketManager"))
        );
    }
}
