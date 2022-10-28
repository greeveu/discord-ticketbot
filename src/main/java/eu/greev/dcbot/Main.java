package eu.greev.dcbot;

import eu.greev.dcbot.ticketsystem.TicketListener;
import eu.greev.dcbot.ticketsystem.interactions.Interaction;
import eu.greev.dcbot.ticketsystem.interactions.TicketClaim;
import eu.greev.dcbot.ticketsystem.interactions.TicketClose;
import eu.greev.dcbot.ticketsystem.interactions.buttons.GetTranscript;
import eu.greev.dcbot.ticketsystem.interactions.buttons.TicketConfirm;
import eu.greev.dcbot.ticketsystem.interactions.buttons.TicketNevermind;
import eu.greev.dcbot.ticketsystem.interactions.commands.*;
import eu.greev.dcbot.ticketsystem.interactions.modals.Application;
import eu.greev.dcbot.ticketsystem.interactions.modals.Bug;
import eu.greev.dcbot.ticketsystem.interactions.modals.Custom;
import eu.greev.dcbot.ticketsystem.interactions.modals.Pardon;
import eu.greev.dcbot.ticketsystem.interactions.selections.TicketApplication;
import eu.greev.dcbot.ticketsystem.interactions.selections.TicketBug;
import eu.greev.dcbot.ticketsystem.interactions.selections.TicketCustom;
import eu.greev.dcbot.ticketsystem.interactions.selections.TicketPardon;
import eu.greev.dcbot.ticketsystem.service.TicketData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.log4j.PropertyConfigurator;
import org.apache.logging.log4j.util.Strings;
import org.jdbi.v3.core.Jdbi;
import org.sqlite.SQLiteDataSource;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.awt.*;
import java.io.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class Main extends ListenerAdapter {
    private static Jdbi jdbi;
    public static final Map<String, Interaction> INTERACTIONS = new HashMap<>();

    public static void main(String[] args) throws InterruptedException, IOException {
        PropertyConfigurator.configure(Main.class.getClassLoader().getResourceAsStream("log4j2.properties"));
        JDA jda = null;

        File file = new File("./Tickets/config.yml");
        if (!file.exists())
            file.createNewFile();

        Constructor constructor = new Constructor(Config.class);
        Yaml yaml = new Yaml(constructor);
        Config config = yaml.load(new FileInputStream(file));
        if (config == null)
            config = new Config();

        if (Strings.isEmpty(config.getToken())) {
            log.error("No valid token provided!1");
            System.exit(1);
        }

        try {
            jda = JDABuilder.create(config.getToken(),
                            List.of(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES))
                    .disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS)
                    .setActivity(Activity.listening(" ticket commands."))
                    .setChunkingFilter(ChunkingFilter.ALL).setMemberCachePolicy(MemberCachePolicy.ALL)
                    .setStatus(OnlineStatus.ONLINE)
                    .build();
        } catch (InvalidTokenException e) {
            log.error("Bot could not be initialized");
            System.exit(1);
        }
        jda.awaitReady();

        initDatasource();

        TicketData ticketData = new TicketData(jda, jdbi);
        TicketService ticketService = new TicketService(jda, config, jdbi, ticketData);
        jda.addEventListener(new TicketListener(ticketService, config));

        jda.updateCommands().addCommands(Commands.slash("ticket", "Manage the ticket system")
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
                .addSubcommands(new SubcommandData("transfer", "Sets the new supporter")
                        .addOption(OptionType.USER, "staff", "The staff member who should be the supporter", true))
                .addSubcommands(new SubcommandData("set-topic", "Set the topic of the ticket")
                        .addOption(OptionType.STRING, "topic", "The new topic", true))
                .addSubcommands(new SubcommandData("info", "Returns info about a ticket")
                        .addOption(OptionType.INTEGER, "ticket-id", "The id of the ticket", true))
                .addSubcommands(new SubcommandData("get-tickets", "Get all ticket ids by member")
                        .addOption(OptionType.USER, "member", "The owner of the tickets", true))
                .addSubcommands(new SubcommandData("setup", "Setup the System")
                        .addOption(OptionType.CHANNEL, "base-channel","The channel where the ticket select menu should be", true)
                        .addOption(OptionType.CHANNEL, "support-category","The category where the tickets should create", true)
                        .addOption(OptionType.ROLE, "staff","The role which is the team role", true)
                        .addOption(OptionType.STRING, "color", "The color of the ticket embeds (HEX-Code)", false)))
                .queue();

        EmbedBuilder missingPerm = new EmbedBuilder().setColor(Color.RED)
                .addField("❌ **Missing permission**", "You are not permitted to use this command!", false);

        EmbedBuilder wrongChannel = new EmbedBuilder().setColor(Color.RED)
                .addField("❌ **Wrong channel**", "You have to use this command in a ticket!", false);

        registerInteraction("claim", new TicketClaim(jda, config, wrongChannel, missingPerm, ticketService));
        registerInteraction("close", new TicketClose(jda, config, wrongChannel, missingPerm, ticketService));

        registerInteraction("ticket-confirm", new TicketConfirm(ticketService));
        registerInteraction("setup", new Setup(config, missingPerm, jda));
        registerInteraction("info", new LoadTicket(jda, config, missingPerm, ticketService));
        registerInteraction("get-tickets", new GetTickets(jda, config, missingPerm, ticketService));
        registerInteraction("create", new Create(config, ticketService, ticketData));
        registerInteraction("add", new AddMember(jda, config, ticketService, wrongChannel, missingPerm));
        registerInteraction("remove", new RemoveMember(jda, config, ticketService, wrongChannel, missingPerm));
        registerInteraction("transfer", new SetSupporter(jda, config, ticketService, wrongChannel, missingPerm));
        registerInteraction("set-owner", new SetOwner(jda, config, ticketService, wrongChannel, missingPerm));
        registerInteraction("set-waiting", new SetWaiting(jda, config, ticketService, wrongChannel, missingPerm));
        registerInteraction("set-topic", new SetTopic(jda, config, ticketService, wrongChannel, missingPerm));

        registerInteraction("nevermind", new TicketNevermind(ticketService, config));
        registerInteraction("application", new Application(ticketService, ticketData, config));
        registerInteraction("custom", new Custom(ticketService, ticketData, config));
        registerInteraction("pardon", new Pardon(ticketService, ticketData, config));
        registerInteraction("bug", new Bug(ticketService, ticketData, config));
        registerInteraction("transcript", new GetTranscript(config, ticketService));

        registerInteraction("select-application", new TicketApplication());
        registerInteraction("select-custom", new TicketCustom());
        registerInteraction("select-pardon", new TicketPardon());
        registerInteraction("select-bug", new TicketBug());

        log.info("Started: " + OffsetDateTime.now(ZoneId.systemDefault()));
    }

    private static void initDatasource() {
        new File("./Tickets").mkdirs();
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:./Tickets/tickets.db");
        jdbi = Jdbi.create(ds);

        String setup = "";
        try (InputStream in = Main.class.getClassLoader().getResourceAsStream("dbsetup.sql")) {
            setup = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            log.error("Could not read db setup file", e);
            System.exit(1);
        }
        Arrays.stream(setup.split(";")).toList().forEach(query -> jdbi.withHandle(h -> h.createUpdate(query).execute()));
    }

    private static void registerInteraction(String identifier, Interaction interaction) {
        INTERACTIONS.put(identifier, interaction);
    }

    private static File getResourceAsFile(String resourcePath) {
        try {
            InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(resourcePath);
            if (in == null) return null;

            File tempFile = File.createTempFile(String.valueOf(in.hashCode()), ".tmp");
            tempFile.deleteOnExit();
            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1)
                    out.write(buffer, 0, bytesRead);
            }
            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}