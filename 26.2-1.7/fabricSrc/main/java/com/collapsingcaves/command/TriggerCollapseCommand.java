package com.collapsingcaves.command;

import com.collapsingcaves.cavein.CaveInManager;
import com.collapsingcaves.cavein.CaveInTier;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TriggerCollapseCommand {
   private TriggerCollapseCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("triggercollapse").requires(CommandPermissions.operator()))
            .then(
               Commands.argument("size", StringArgumentType.word())
                  .suggests(TriggerCollapseCommand::suggestSizes)
                  .then(Commands.argument("player", EntityArgument.player()).executes(TriggerCollapseCommand::execute))
            )
      );
   }

   private static CompletableFuture<Suggestions> suggestSizes(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
      String start = builder.getRemaining().toLowerCase(Locale.ROOT);

      for (CaveInTier tier : CaveInTier.values()) {
         if (tier.id.startsWith(start)) {
            builder.suggest(tier.id);
         }
      }

      return builder.buildFuture();
   }

   private static int execute(CommandContext<CommandSourceStack> context) {
      String sizeArgument = StringArgumentType.getString(context, "size");
      CaveInTier tier = CaveInTier.byId(sizeArgument);
      if (tier == null) {
         ((CommandSourceStack)context.getSource()).sendFailure(Component.literal("Unknown cave-in size: " + sizeArgument + ". Use small, medium or large."));
         return 0;
      }

      ServerPlayer player;
      try {
         player = EntityArgument.getPlayer(context, "player");
      } catch (CommandSyntaxException e) {
         ((CommandSourceStack)context.getSource()).sendFailure(Component.literal("The mod cannot find this player."));
         return 0;
      }

      UUID issuer = ((CommandSourceStack)context.getSource()).getEntity() instanceof ServerPlayer sourcePlayer
         ? sourcePlayer.getUUID()
         : null;
      CaveInManager.setForcedCaveIn(player, tier, issuer);
      // Clear any cooldown the target is sitting on. A forced cave-in ignores the cooldown
      // when it fires, but leaving one in place means the very next natural break is still
      // blocked, which makes the command awkward to test with.
      CaveInManager.setCooldownSeconds(player, 0);
      ((CommandSourceStack)context.getSource())
         .sendSuccess(
            () -> Component.literal("The next applicable block that " + player.getName().getString() + " breaks starts a " + tier.id + " cave-in."), true
         );
      return 1;
   }
}
