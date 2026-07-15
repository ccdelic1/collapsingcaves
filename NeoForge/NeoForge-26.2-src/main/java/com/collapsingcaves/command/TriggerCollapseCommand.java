package com.collapsingcaves.command;

import com.collapsingcaves.cavein.CaveInManager;
import com.collapsingcaves.cavein.CaveInTier;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;

public class TriggerCollapseCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("triggercollapse")
                        .requires(source -> source.getServer() != null
                                && source.getServer().isSingleplayer()
                                || source.getEntity() instanceof ServerPlayer player
                                && source.getServer() != null
                                && source.getServer().getPlayerList().isOp(player.nameAndId()))
                        .then(Commands.argument("size", StringArgumentType.word())
                                .suggests(TriggerCollapseCommand::suggestTiers)
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(TriggerCollapseCommand::execute)))
        );
    }

    private static CompletableFuture<Suggestions> suggestTiers(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        for (CaveInTier tier : CaveInTier.values()) {
            if (tier.id.startsWith(builder.getRemaining().toLowerCase())) {
                builder.suggest(tier.id);
            }
        }
        return builder.buildFuture();
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        String sizeArg = StringArgumentType.getString(context, "size").toLowerCase();
        CaveInTier foundTier = null;
        for (CaveInTier t : CaveInTier.values()) {
            if (t.id.equals(sizeArg)) {
                foundTier = t;
                break;
            }
        }

        if (foundTier == null) {
            context.getSource().sendFailure(Component.literal("Unknown cave-in size: " + sizeArg
                    + ". Valid sizes: small, medium, large, enormous, gargantuan"));
            return 0;
        }

        final CaveInTier tier = foundTier;

        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            CaveInManager.setForcedCaveIn(player, tier);
            context.getSource().sendSuccess(
                    () -> Component.literal("Queued " + tier.id + " cave-in for " + player.getName().getString()
                            + " on their next applicable block break."),
                    true
            );
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Could not find player."));
            return 0;
        }
    }
}
