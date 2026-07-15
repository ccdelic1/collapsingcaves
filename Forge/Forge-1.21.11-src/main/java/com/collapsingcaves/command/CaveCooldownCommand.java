package com.collapsingcaves.command;

import com.collapsingcaves.cavein.CaveInManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class CaveCooldownCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("cavecooldown")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(CaveCooldownCommand::execute))
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            long remainingTicks = CaveInManager.getCooldownRemaining(player);

            if (remainingTicks <= 0) {
                context.getSource().sendSuccess(
                        () -> Component.literal(player.getName().getString() + " has no active cave-in cooldown."),
                        false
                );
            } else {
                long seconds = remainingTicks / 20;
                long minutes = seconds / 60;
                long remainingSeconds = seconds % 60;
                String timeStr = minutes > 0
                        ? minutes + "m " + remainingSeconds + "s"
                        : remainingSeconds + "s";
                context.getSource().sendSuccess(
                        () -> Component.literal(player.getName().getString() + " has " + timeStr + " remaining on cave-in cooldown."),
                        false
                );
            }
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Could not find player."));
            return 0;
        }
    }
}
