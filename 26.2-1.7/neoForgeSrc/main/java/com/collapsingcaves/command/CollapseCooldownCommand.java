package com.collapsingcaves.command;

import com.collapsingcaves.cavein.CaveInManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class CollapseCooldownCommand {
   private CollapseCooldownCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("collapsecooldown").requires(CommandPermissions.operator()))
            .then(Commands.argument("player", EntityArgument.player()).executes(CollapseCooldownCommand::execute))
      );
   }

   private static int execute(CommandContext<CommandSourceStack> context) {
      ServerPlayer player;
      try {
         player = EntityArgument.getPlayer(context, "player");
      } catch (CommandSyntaxException e) {
         ((CommandSourceStack)context.getSource()).sendFailure(Component.literal("The mod cannot find this player."));
         return 0;
      }

      long remainingTicks = CaveInManager.getCooldownRemaining(player);
      String name = player.getName().getString();
      if (remainingTicks <= 0L) {
         ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal(name + " has no cooldown. A cave-in can occur."), false);
      } else {
         long seconds = remainingTicks / 20L;
         long minutes = seconds / 60L;
         String time = minutes > 0L ? minutes + "m " + seconds % 60L + "s" : seconds + "s";
         ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal(name + " has a cooldown of " + time + "."), false);
      }

      return 1;
   }
}
