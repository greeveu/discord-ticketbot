package eu.greev.dcbot;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
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
import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class Main extends ListenerAdapter {
    private static DataSource dataSource;

    public static void main(String[] args) throws InterruptedException, LoginException, IOException, SQLException {
        String log4jConfPath = "./src/main/resources/log4j.properties";
        PropertyConfigurator.configure(log4jConfPath);

        initDatasource();

        JDA jda;
        jda = JDABuilder.createLight(new Data().botToken, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_EMOJIS_AND_STICKERS, GatewayIntent.GUILD_MEMBERS)
                .setActivity(Activity.listening(" ticket commands."))
                .setStatus(OnlineStatus.ONLINE)
                .build();
        jda.addEventListener(new Main(), new TicketListener(jda, dataSource));
        jda.awaitReady();

        List<CommandData> commands = new ArrayList<>();
        createCommands(commands);
        jda.upsertCommand(Commands.slash("ping", "Returns the ping of the bot")).queue();
        jda.getGuildById(new Data().testID).updateCommands().addCommands(commands).queue();

        String day = new SimpleDateFormat("dd-MM-yy hh:mm aa ").format(new Date(System.currentTimeMillis()));
        LogManager.getLogger(Main.class).log(Level.INFO, "Started: " + day);
    }

    private static void createCommands(List<CommandData> commands) {
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
                .addSubcommands(new SubcommandData("owner", "Set the new owner of the ticket")
                        .addOption(OptionType.USER, "member", "The new owner"))
                .addSubcommands(new SubcommandData("waiting", "Set the ticket in waiting mode"))
                .addSubcommands(new SubcommandData("supporter", "Sets the new supporter")
                        .addOption(OptionType.USER, "staff", "The staff member who should be the supporter", true))
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

    public static void initDatasource() throws SQLException, IOException {
        new File("./GreevTickets").mkdirs();
        Data data = new Data();
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:./GreevTickets/tickets.db");
        HikariDataSource dataSource = new HikariDataSource(config);
        Main.dataSource = dataSource;
        testDataSource(dataSource);
        initDb();
    }

    private static void initDb() throws SQLException, IOException {
        String setup;
        try (InputStream in = Main.class.getClassLoader().getResourceAsStream("dbsetup.sql")) {
            setup = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            System.err.println("Could not read db setup file.");
            e.printStackTrace();
            throw e;
        }
        String[] queries = setup.split(";");
        for (String query : queries) {
            if (query.isBlank()) continue;
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.execute();
            }
        }
        System.out.println("Database setup complete.");
    }

    private static void testDataSource(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(1000)) {
                throw new SQLException("Could not establish database connection.");
            }
        }
    }
}
