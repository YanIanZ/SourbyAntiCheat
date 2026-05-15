package dev.yanianz.sourbyanticheat.command;

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
        new SacPerf().register(commandManager, commandAdapter);
        new SacDebug().register(commandManager, commandAdapter);
        new SacAlerts().register(commandManager, commandAdapter);
        new SacProfile().register(commandManager, commandAdapter);
        new SacSendAlert().register(commandManager, commandAdapter);
        new SacHelp().register(commandManager, commandAdapter);
        new SacHistory().register(commandManager, commandAdapter);
        new SacHistoryMigrate().register(commandManager, commandAdapter);
        new SacHistoryCopy().register(commandManager, commandAdapter);
        new SacReload().register(commandManager, commandAdapter);
        new SacSpectate().register(commandManager, commandAdapter);
        new SacStopSpectating().register(commandManager, commandAdapter);
        new SacLog().register(commandManager, commandAdapter);
        new SacVerbose().register(commandManager, commandAdapter);
        new SacVersion().register(commandManager, commandAdapter);
        new SacDump().register(commandManager, commandAdapter);
        new SacBrands().register(commandManager, commandAdapter);
        new SacList().register(commandManager, commandAdapter);
        new SacTestWebhook().register(commandManager, commandAdapter);
        new SacSpartan().register(commandManager, commandAdapter);
        new SacStatus().register(commandManager, commandAdapter);
        new SacToggle().register(commandManager, commandAdapter);
        new SacReset().register(commandManager, commandAdapter);
        new SacGUICommand().register(commandManager, commandAdapter);

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
