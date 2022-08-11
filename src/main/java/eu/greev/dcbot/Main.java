package eu.greev.dcbot;

import eu.greev.dcbot.ticketsystem.TicketListener;
import eu.greev.dcbot.utils.data.Data;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Main extends ListenerAdapter {

    public static void main(String[] args) throws InterruptedException, LoginException {
        String log4jConfPath = "./src/main/resources/log4j.properties";
        PropertyConfigurator.configure(log4jConfPath);

        JDA jda;
        jda = JDABuilder.createLight(new Data().botToken, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_EMOJIS_AND_STICKERS, GatewayIntent.GUILD_MEMBERS)
                .setActivity(Activity.listening(" ticket commands."))
                .setStatus(OnlineStatus.ONLINE)
                .build();
        jda.addEventListener(new Main(), new TicketListener(jda));
        jda.awaitReady();

        List<CommandData> commands = new ArrayList<>();
        createCommands(commands);
        jda.upsertCommand(Commands.slash("ping", "Returns the ping of the bot")).queue();
        jda.getGuildById(new Data().testID).updateCommands().addCommands(commands).queue();

        String day = new SimpleDateFormat("dd-MM-yy hh:mm aa ").format(new Date(System.currentTimeMillis()));
        LogManager.getLogger(Main.class).log(Level.INFO, "Started: " + day);
    }

    private static void createCommands(List<CommandData> commands) {
        /*OptionData help = new OptionData(OptionType.STRING, "commands", "Possible commands", false);
        help.addChoices(
                new Command.Choice("add", "add"),
                new Command.Choice("remove", "remove"),
                new Command.Choice("create", "create"),
                new Command.Choice("close", "close"),
                new Command.Choice("claim", "claim"),
                new Command.Choice("set-handler", "handler")
                );*/

        commands.add(Commands.slash("ticket", "Manage the ticket system")
                .addSubcommands(new SubcommandData("setup", "Setup the System"))
                .addSubcommands(new SubcommandData("add", "Add a User to this ticket")
                        .addOption(OptionType.USER,"member", "The user adding to the current ticket", true))
                .addSubcommands(new SubcommandData("remove", "Remove a User from this ticket")
                        .addOption(OptionType.USER,"member", "The user removing from the current ticket", true))
                .addSubcommands(new SubcommandData("create", "Create a new Ticket for you")
                        .addOption(OptionType.STRING, "topic", "The topic of the ticket", false))
                .addSubcommands(new SubcommandData("close", "Close this ticket")
                        .addOption(OptionType.STRING, "reason", "The reason the ticket was closed", false))
                .addSubcommands(new SubcommandData("claim", "Claim this ticket"))
                .addSubcommands(new SubcommandData("help", "Display help menu"))
                        //.addOptions(help))
                .addSubcommands(new SubcommandData("supporter", "Sets the new supporter")
                        .addOption(OptionType.USER, "staff", "The staff member who should be the supporter", true))

                //.addSubcommands(new SubcommandData("clear-config", "Clear the config for the TicketManager"))
        );
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("ping")) {
            long time = System.currentTimeMillis();
            event.reply("Pong!").setEphemeral(true)
                    .flatMap(v ->
                            event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time)
                    ).queue();
        }
    }
}
