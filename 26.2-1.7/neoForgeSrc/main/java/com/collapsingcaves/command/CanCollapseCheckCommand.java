package com.collapsingcaves.command;

import com.collapsingcaves.cavein.CaveInManager;
import com.collapsingcaves.cavein.CaveInManager.CollapseBlocker;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class CanCollapseCheckCommand {
   // Vanilla creative reach. The per-player interaction range only became readable as an
   // attribute in 1.20.5, so a fixed distance keeps this one file identical on every
   // version rather than forking it for 1.20.1.
   private static final double LOOK_RANGE = 5.0;
   private static final String NO_BLOCK = "No block being looked at, block conditions cannot be listed for a null block.";

   private CanCollapseCheckCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         Commands.literal("cancollapsecheck").requires(CommandPermissions.operator()).executes(CanCollapseCheckCommand::execute)
      );
   }

   // Returns 1 for true and 0 for false so the answer can be read by a command block or
   // by 'execute store success' as well as from chat.
   private static int execute(CommandContext<CommandSourceStack> context) {
      CommandSourceStack source = (CommandSourceStack)context.getSource();
      if (!(source.getEntity() instanceof ServerPlayer player)) {
         source.sendFailure(Component.literal("Only a player can use this command."));
         return 0;
      }

      if (!(player.level() instanceof ServerLevel serverLevel)) {
         source.sendFailure(Component.literal("This command needs a server level."));
         return 0;
      }

      BlockPos lookedAt = lookedAtBlock(player);
      List<CollapseBlocker> blockers = CaveInManager.get(serverLevel).collapseBlockers(player, lookedAt);
      if (!blockers.isEmpty()) {
         source.sendSuccess(() -> Component.literal("Cave-In Check: False").withStyle(ChatFormatting.RED), false);

         for (CollapseBlocker blocker : blockers) {
            source.sendSuccess(() -> blocker.line(true), false);
         }

         reportMissingBlock(source, lookedAt);
         return 0;
      }

      source.sendSuccess(() -> Component.literal("Cave-In Check: True").withStyle(ChatFormatting.GREEN), false);
      reportMissingBlock(source, lookedAt);
      if (CaveInManager.hasForcedCaveIn(player)) {
         source.sendSuccess(
            () -> Component.literal("The next block you break that a cave-in can use starts one, because a size was forced for you."), false
         );
      } else {
         source.sendSuccess(() -> Component.literal("The next block you break that a cave-in can use has a chance to start one."), false);
      }

      return 1;
   }

   // Looking at nothing is not a blocker, so it never changes the verdict. It is still
   // said out loud, because a whole category going unchecked is exactly the sort of
   // silence that makes a diagnostic misleading.
   private static void reportMissingBlock(CommandSourceStack source, BlockPos lookedAt) {
      if (lookedAt == null) {
         source.sendSuccess(() -> new CollapseBlocker(true, NO_BLOCK, NO_BLOCK).line(true), false);
      }
   }

   private static BlockPos lookedAtBlock(ServerPlayer player) {
      HitResult hit = player.pick(LOOK_RANGE, 0.0F, false);
      return hit.getType() == HitResult.Type.BLOCK ? ((BlockHitResult)hit).getBlockPos() : null;
   }
}
