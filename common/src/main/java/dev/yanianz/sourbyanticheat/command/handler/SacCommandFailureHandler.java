package dev.yanianz.sourbyanticheat.command.handler;

import dev.yanianz.sourbyanticheat.command.SenderRequirement;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.processors.requirements.RequirementFailureHandler;
import org.jetbrains.annotations.NotNull;

public class SacCommandFailureHandler implements RequirementFailureHandler<Sender, SenderRequirement> {
    @Override
    public void handleFailure(@NotNull CommandContext<Sender> context, @NotNull SenderRequirement requirement) {
        context.sender().sendMessage(requirement.errorMessage(context.sender()));
    }
}
