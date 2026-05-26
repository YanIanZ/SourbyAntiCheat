package dev.yanianz.sourbyanticheat.command;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.command.commands.*;
import dev.yanianz.sourbyanticheat.command.handler.SacCommandFailureHandler;
import dev.yanianz.sourbyanticheat.platform.api.command.CommandService;
import dev.yanianz.sourbyanticheat.platform.api.manager.cloud.CloudCommandAdapter;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import dev.yanianz.sourbyanticheat.utils.anticheat.MessageUtil;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.exception.InvalidSyntaxException;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.processors.requirements.RequirementApplicable;
import org.incendo.cloud.processors.requirements.RequirementApplicable.RequirementApplicableFactory;
import org.incendo.cloud.processors.requirements.RequirementPostprocessor;
import org.incendo.cloud.processors.requirements.Requirements;

import java.util.function.Function;
import java.util.function.Supplier;

public class CloudCommandService implements CommandService {

    public static final CloudKey<Requirements<Sender, SenderRequirement>> REQUIREMENT_KEY
            = CloudKey.of("requirements", new TypeToken<>() {});

    public static final RequirementApplicableFactory<Sender, SenderRequirement> REQUIREMENT_FACTORY
            = RequirementApplicable.factory(REQUIREMENT_KEY);

    private boolean commandsRegistered = false;

    private final Supplier<CommandManager<Sender>> commandManagerSupplier;
    private final CloudCommandAdapter commandAdapter;

    public CloudCommandService(Supplier<CommandManager<Sender>> commandManagerSupplier, CloudCommandAdapter commandAdapter) {
        this.commandManagerSupplier = commandManagerSupplier;
        this.commandAdapter = commandAdapter;
    }

    public void registerCommands() {
        if (commandsRegistered) return;
        CommandManager<Sender> commandManager = commandManagerSupplier.get();

        commandManager.command(
                commandManager.commandBuilder("sac")
                        .handler(context -> SacHelp.renderTo(context.sender()))
        );

        boolean legacyEnabled = SacAPI.INSTANCE.getConfigManager().getConfig()
                .getBooleanElse("commands.legacy-enabled", false);

        java.util.function.BiConsumer<String, dev.yanianz.sourbyanticheat.command.BuildableCommand> reg =
                (name, cmd) -> {
                    if (legacyEnabled || !dev.yanianz.sourbyanticheat.command.CommandCatalog.isLegacy(name)) {
                        cmd.register(commandManager, commandAdapter);
                    }
                };

        // --- core ---
        reg.accept("alerts", new SacAlerts());
        reg.accept("profile", new SacProfile());
        reg.accept("help", new SacHelp());
        reg.accept("history", new SacHistory());
        reg.accept("reload", new SacReload());
        reg.accept("spectate", new SacSpectate());
        reg.accept("stopspectating", new SacStopSpectating());
        reg.accept("verbose", new SacVerbose());
        reg.accept("version", new SacVersion());
        reg.accept("brands", new SacBrands());
        reg.accept("list", new SacList());
        reg.accept("status", new SacStatus());
        reg.accept("toggle", new SacToggle());
        reg.accept("reset", new SacReset());
        reg.accept("gui", new SacGUICommand());
        reg.accept("info", new SacInfo());
        reg.accept("note", new SacNote());
        reg.accept("top", new SacTop());
        reg.accept("exempt", new SacExempt());
        // standalone player-facing report commands — always registered
        reg.accept("report", new ReportCommand());
        reg.accept("reports", new ReportsCommand());

        // --- legacy (registered only when commands.legacy-enabled: true) ---
        reg.accept("perf", new SacPerf());
        reg.accept("debug", new SacDebug());
        reg.accept("sendalert", new SacSendAlert());
        reg.accept("history-migrate", new SacHistoryMigrate());
        reg.accept("history-copy", new SacHistoryCopy());
        reg.accept("log", new SacLog());
        reg.accept("dump", new SacDump());
        reg.accept("testwebhook", new SacTestWebhook());
        reg.accept("spartan", new SacSpartan());
        reg.accept("summary", new SacSummary());
        reg.accept("checks", new SacChecks());

        final RequirementPostprocessor<Sender, SenderRequirement>
                senderRequirementPostprocessor = RequirementPostprocessor.of(
                REQUIREMENT_KEY,
                new SacCommandFailureHandler()
        );
        commandManager.registerCommandPostProcessor(senderRequirementPostprocessor);
        registerExceptionHandler(commandManager, InvalidSyntaxException.class, e -> MessageUtil.miniMessage(e.correctSyntax()));
        commandsRegistered = true;
    }

    protected <E extends Exception> void registerExceptionHandler(CommandManager<Sender> commandManager, Class<E> ex, Function<E, ComponentLike> toComponent) {
        commandManager.exceptionController().registerHandler(ex,
                (c) -> c.context().sender().sendMessage(toComponent.apply(c.exception()).asComponent().colorIfAbsent(NamedTextColor.RED))
        );
    }
}
