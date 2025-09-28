package eu.greev.dcbot;

import eu.greev.dcbot.scheduler.HourlyScheduler;
import eu.greev.dcbot.ticketsystem.TicketListener;
import eu.greev.dcbot.ticketsystem.categories.*;
import eu.greev.dcbot.ticketsystem.interactions.*;
import eu.greev.dcbot.ticketsystem.interactions.buttons.*;
import eu.greev.dcbot.ticketsystem.interactions.commands.*;
import eu.greev.dcbot.ticketsystem.interactions.modals.TicketConfirmMessageModal;
import eu.greev.dcbot.ticketsystem.interactions.modals.TicketModal;
import eu.greev.dcbot.ticketsystem.service.TicketData;
import eu.greev.dcbot.ticketsystem.service.TicketService;
import eu.greev.dcbot.utils.Config;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
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
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class Main {
    public static final Map<String, Interaction> INTERACTIONS = new HashMap<>();
    public static final List<ICategory> CATEGORIES = new ArrayList<>();
    @Getter
    private static String createCommandId;
    @Getter
    private static String getTicketCommandId;
    private static Jdbi jdbi;

    public static void main(String[] args) throws InterruptedException, IOException {
        PropertyConfigurator.configure(Main.class.getClassLoader().getResourceAsStream("log4j2.properties"));
        JDA jda = null;

        File file = new File("./Tickets/config.yml");
        new File("./Tickets").mkdirs();
        if (!file.exists()) file.createNewFile();

        Constructor constructor = new Constructor(Config.class);
        Yaml yaml = new Yaml(constructor);
        Config config = yaml.load(new FileInputStream(file));
        if (config == null) {
            config = new Config();
        }

        if (Strings.isEmpty(config.getToken())) {
            log.error("No valid token provided! Add your bot token into `./Tickets/config.yml` with the key `token`");
            System.exit(1);
        }

        try {
            jda = JDABuilder.create(config.getToken(),
                            List.of(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_PRESENCES))
                    .disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS, CacheFlag.SCHEDULED_EVENTS)
                    .setActivity(Activity.listening(" ticket commands."))
                    .setChunkingFilter(ChunkingFilter.ALL).setMemberCachePolicy(MemberCachePolicy.ALL)
                    .setStatus(OnlineStatus.ONLINE)
                    .build();
        } catch (InvalidTokenException e) {
            log.error("Bot could not be initialized", e);
            System.exit(1);
        }
        jda.awaitReady();

        initDatasource();

        TicketData ticketData = new TicketData(jda, jdbi);
        TicketService ticketService = new TicketService(jda, config, jdbi, ticketData);
        jda.addEventListener(new TicketListener(ticketService, config));

        registerCategory(new General(), config, ticketService, ticketData);
        registerCategory(new Report(), config, ticketService, ticketData);
        registerCategory(new Creator(), config, ticketService, ticketData);
        registerCategory(new Bug(), config, ticketService, ticketData);
        registerCategory(new Payment(), config, ticketService, ticketData);
        registerCategory(new Security(), config, ticketService, ticketData);

        jda.updateCommands().addCommands(Commands.slash("ticket", "Manage the ticket system")
                .addSubcommands(new SubcommandData("add", "Add a User to this ticket")
                        .addOption(OptionType.USER, "member", "The user adding to the current ticket", true))
                .addSubcommands(new SubcommandData("remove", "Remove a User from this ticket")
                        .addOption(OptionType.USER, "member", "The user removing from the current ticket", true))
                .addSubcommands(new SubcommandData("close", "Close this ticket"))
                .addSubcommands(new SubcommandData("claim", "Claim this ticket"))
                .addSubcommands(new SubcommandData("set-owner", "Set the new owner of the ticket")
                        .addOption(OptionType.USER, "member", "The new owner"))
                .addSubcommands(new SubcommandData("set-waiting", "Set the ticket in waiting mode"))
                .addSubcommands(new SubcommandData("transfer", "Sets the new supporter")
                        .addOption(OptionType.USER, "staff", "The staff member who should be the supporter", true))
                .addSubcommands(new SubcommandData("info", "Returns info about a ticket")
                        .addOption(OptionType.INTEGER, "ticket-id", "The id of the ticket", true))
                .addSubcommands(new SubcommandData("get-tickets", "Get all ticket ids by member")
                        .addOption(OptionType.USER, "member", "The owner of the tickets", true))
                .addSubcommands(new SubcommandData("stats", "Show general ticket statistics"))
                .addSubcommands(new SubcommandData("setup", "Setup the System")
                        .addOption(OptionType.CHANNEL, "base-channel", "The channel where the ticket select menu should be", true)
                        .addOption(OptionType.CHANNEL, "unclaimed-category", "The category where the tickets should create", true)
                        .addOption(OptionType.ROLE, "staff", "The role which is the team role", true)
                        .addOption(OptionType.STRING, "color", "The color of the ticket embeds (HEX-Code)", false))
                .addSubcommands(new SubcommandData("set-claim-emoji", "Set your personal claim emoji")
                        .addOption(OptionType.STRING, "emoji", "The emoji you want to set", true))
                .addSubcommandGroups(new SubcommandGroupData("thread", "Manages the ticket thread")
                        .addSubcommands(new SubcommandData("add", "Add a staff member to the ticket thread")
                                .addOption(OptionType.USER, "staff", "Staff member to add", true))
                        .addSubcommands(new SubcommandData("join", "Join the ticket thread")))
        ).queue(s -> s.get(0).getSubcommands().forEach(c -> {
                    if (c.getName().equals("get-tickets")) {
                        getTicketCommandId = c.getId();
                    } else if (c.getName().equals("create")) {
                        createCommandId = c.getId();
                    }
                })
        );

        new HourlyScheduler(config, ticketService, ticketData, jda).start();

        EmbedBuilder missingPerm = new EmbedBuilder().setColor(Color.RED)
                .addField("❌ **Missing permission**", "You are not permitted to use this command!", false);

        EmbedBuilder wrongChannel = new EmbedBuilder().setColor(Color.RED)
                .addField("❌ **Wrong channel**", "You have to use this command in a ticket!", false);

        registerInteraction("claim", new TicketClaim(jda, config, wrongChannel, missingPerm, ticketService));
        registerInteraction("close", new TicketClose(jda, config, wrongChannel, missingPerm, ticketService));

        registerInteraction("ticket-confirm", new TicketConfirm(ticketService));
        registerInteraction("ticket-confirm-message", new TicketConfirmMessage());
        registerInteraction("ticket-confirm-message-modal", new TicketConfirmMessageModal(ticketService));
        registerInteraction("setup", new Setup(config, ticketService, missingPerm, jda));
        registerInteraction("info", new LoadTicket(config, ticketService, missingPerm, jda));
        registerInteraction("get-tickets", new GetTickets(config, ticketService, missingPerm, jda));
        registerInteraction("stats", new Stats(config, ticketService, missingPerm, jda));
        registerInteraction("add", new AddMember(config, jda, ticketService, wrongChannel, missingPerm));
        registerInteraction("remove", new RemoveMember(config, ticketService, missingPerm, wrongChannel, jda));
        registerInteraction("transfer", new Transfer(config, ticketService, missingPerm, wrongChannel, jda));
        registerInteraction("set-owner", new SetOwner(config, ticketService, missingPerm, wrongChannel, jda));
        registerInteraction("set-waiting", new SetWaiting(config, ticketService, missingPerm, wrongChannel, jda));

        registerInteraction("nevermind", new TicketNevermind(ticketService, config));
        registerInteraction("transcript", new GetTranscript(config, ticketService));

        registerInteraction("thread add", new ThreadAdd(config, ticketService, wrongChannel, missingPerm, jda));
        registerInteraction("thread join", new ThreadJoin(config, ticketService, wrongChannel, missingPerm, jda));

        registerInteraction("tickets-forwards", new TicketsForward(ticketService));
        registerInteraction("tickets-backwards", new TicketsBackwards(ticketService));

        registerInteraction("set-claim-emoji", new SetClaimEmoji(config, ticketService, missingPerm, jda));

        log.info("Started: {}", OffsetDateTime.now(ZoneId.systemDefault()));


    }

    private static void initDatasource() {
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

    private static void registerCategory(ICategory category, Config config, TicketService ticketService, TicketData ticketData) {
        registerInteraction("select-" + category.getId(), new CategorySelection(category));
        registerInteraction(category.getId(), new TicketModal(category, config, ticketService, ticketData));
        CATEGORIES.add(category);
    }
}