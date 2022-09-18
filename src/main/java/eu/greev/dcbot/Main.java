package eu.greev.dcbot;

import eu.greev.dcbot.ticketsystem.TicketListener;
import eu.greev.dcbot.ticketsystem.service.TicketData;
import eu.greev.dcbot.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.apache.log4j.PropertyConfigurator;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.simpleyaml.configuration.file.YamlFile;
import org.sqlite.SQLiteDataSource;

import java.io.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class Main extends ListenerAdapter {
    private static Jdbi jdbi;

    public static void main(String[] args) throws InterruptedException, IOException {
        PropertyConfigurator.configure("./GreevTickets/log4j.properties");
        initDatasource();

        YamlFile config = new YamlFile("./GreevTickets/token.yml");
        config.load();
        JDA jda;
        jda = JDABuilder.create(config.getString("botToken"), List.of(GatewayIntent.values()))
                .setActivity(Activity.listening(" ticket commands."))
                .setEnabledIntents(List.of(GatewayIntent.values()))
                .setChunkingFilter(ChunkingFilter.ALL).setMemberCachePolicy(MemberCachePolicy.ALL)
                .setStatus(OnlineStatus.ONLINE)
                .build();
        jda.awaitReady();
        jda.addEventListener(new Main(), new TicketListener(jda, jdbi));
        jda.getGuildById(Constants.SERVER_ID).updateCommands().addCommands(Commands.slash("ticket", "Manage the ticket system")
                .addSubcommands(new SubcommandData("setup", "Setup the System"))
                .addSubcommands(new SubcommandData("add", "Add a User to this ticket")
                        .addOption(OptionType.USER,"member", "The user adding to the current ticket", true))
                .addSubcommands(new SubcommandData("remove", "Remove a User from this ticket")
                        .addOption(OptionType.USER,"member", "The user removing from the current ticket", true))
                .addSubcommands(new SubcommandData("create", "Create a new Ticket for you")
                        .addOption(OptionType.STRING, "topic", "The topic of the ticket", false))
                .addSubcommands(new SubcommandData("close", "Close this ticket"))
                .addSubcommands(new SubcommandData("claim", "Claim this ticket"))
                .addSubcommands(new SubcommandData("set-owner", "Set the new owner of the ticket")
                        .addOption(OptionType.USER, "member", "The new owner"))
                .addSubcommands(new SubcommandData("set-waiting", "Set the ticket in waiting mode"))
                .addSubcommands(new SubcommandData("set-supporter", "Sets the new supporter")
                        .addOption(OptionType.USER, "staff", "The staff member who should be the supporter", true))
                .addSubcommands(new SubcommandData("set-topic", "Set the topic of the ticket")
                        .addOption(OptionType.STRING, "topic", "The new topic", true))).queue();
        new TicketData(jda, jdbi);
        log.info("Started: " + OffsetDateTime.now(ZoneId.systemDefault()));
    }

    private static void initDatasource() {
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:./GreevTickets/tickets.db");
        jdbi = Jdbi.create(ds);

        String setup = "";
        try (InputStream in = Main.class.getClassLoader().getResourceAsStream("dbsetup.sql")) {
            setup = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            log.error("Could not read db setup file", e);
            System.exit(1);
        }
        String[] queries = setup.split(";");
        for (String query : queries) {
            jdbi.withHandle(h -> h.createUpdate(query).execute());
        }
    }
}
