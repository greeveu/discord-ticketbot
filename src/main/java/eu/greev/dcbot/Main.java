package eu.greev.dcbot;

import eu.greev.dcbot.ticketsystem.TicketListener;
import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.interactions.TicketClaim;
import eu.greev.dcbot.ticketsystem.interactions.TicketClose;
import eu.greev.dcbot.ticketsystem.interactions.buttons.*;
import eu.greev.dcbot.ticketsystem.interactions.commands.*;
import eu.greev.dcbot.ticketsystem.interactions.modals.*;
import eu.greev.dcbot.ticketsystem.interactions.selections.*;
import eu.greev.dcbot.ticketsystem.service.TicketData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.log4j.PropertyConfigurator;
import org.jdbi.v3.core.Jdbi;
import org.simpleyaml.configuration.file.YamlFile;
import org.sqlite.SQLiteDataSource;

import java.awt.*;
import java.io.*;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class Main extends ListenerAdapter {
    private static Jdbi jdbi;
    public static final HashMap<String, Interaction> INTERACTIONS = new HashMap<>();

    public static void main(String[] args) throws InterruptedException, IOException, URISyntaxException {
        PropertyConfigurator.configure(Main.class.getClassLoader().getResourceAsStream("log4j2.properties"));
        initDatasource();

        YamlFile config = new YamlFile(Main.class.getClassLoader().getResource("token.yml").toURI());
        config.load();

        JDA jda = JDABuilder.create(config.getString("botToken"), List.of(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES))
                .disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS)
                .setActivity(Activity.listening(" ticket commands."))
                .setChunkingFilter(ChunkingFilter.ALL).setMemberCachePolicy(MemberCachePolicy.ALL)
                .setStatus(OnlineStatus.ONLINE)
                .build();
        jda.awaitReady();
        TicketData ticketData = new TicketData(jda, jdbi);
        TicketService ticketService = new TicketService(jda, jdbi, ticketData);
        jda.addEventListener(new TicketListener(ticketService));
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

        EmbedBuilder missingPerm = new EmbedBuilder().setColor(Color.RED).setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO)
                .addField("❌ **Missing permission**", "You are not permitted to use this command!", false);

        EmbedBuilder wrongChannel = new EmbedBuilder().setColor(Color.RED).setFooter(Constants.SERVER_NAME, Constants.GREEV_LOGO)
                .addField("❌ **Wrong channel**", "You have to use this command in a ticket!", false);

        Role STAFF = jda.getRoleById(Constants.TEAM_ID);
        INTERACTIONS.put("claim", new TicketClaim(wrongChannel, missingPerm, STAFF, ticketService));
        INTERACTIONS.put("close", new TicketClose(wrongChannel, missingPerm, STAFF));


        INTERACTIONS.put("ticket-confirm", new TicketConfirm(ticketService));
        INTERACTIONS.put("setup", new Setup(missingPerm, jda));
        INTERACTIONS.put("create", new Create(ticketService, ticketData));
        INTERACTIONS.put("add", new AddMember(STAFF, ticketService, wrongChannel, missingPerm));
        INTERACTIONS.put("remove", new RemoveMember(STAFF, ticketService, wrongChannel, missingPerm));
        INTERACTIONS.put("set-supporter", new SetSupporter(STAFF, jda, ticketService, wrongChannel, missingPerm));
        INTERACTIONS.put("set-owner", new SetOwner(STAFF, ticketService, wrongChannel, missingPerm));
        INTERACTIONS.put("set-waiting", new SetWaiting(STAFF, ticketService, wrongChannel, missingPerm));
        INTERACTIONS.put("set-topic", new SetTopic(STAFF, ticketService, wrongChannel, missingPerm));

        INTERACTIONS.put("complain", new Complain(ticketService, ticketData));
        INTERACTIONS.put("custom", new Custom(ticketService, ticketData));
        INTERACTIONS.put("pardon", new Pardon(ticketService, ticketData));
        INTERACTIONS.put("report", new Report(ticketService, ticketData));
        INTERACTIONS.put("bug", new Bug(ticketService, ticketData));
        INTERACTIONS.put("question", new Question(ticketService, ticketData));

        INTERACTIONS.put("select-complain", new TicketComplain());
        INTERACTIONS.put("select-custom", new TicketCustom());
        INTERACTIONS.put("select-pardon", new TicketPardon());
        INTERACTIONS.put("select-report", new TicketReport());
        INTERACTIONS.put("select-bug", new TicketBug());
        INTERACTIONS.put("select-question", new TicketQuestion());

        log.info("Started: " + OffsetDateTime.now(ZoneId.systemDefault()));
    }

    //just a temp test method: will be removed after testing
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