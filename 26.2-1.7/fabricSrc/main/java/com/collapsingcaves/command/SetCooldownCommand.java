package com.collapsingcaves.command;

import com.collapsingcaves.cavein.CaveInManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class SetCooldownCommand {
   private SetCooldownCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("setcooldown").requires(CommandPermissions.operator()))
            .then(
               Commands.argument("seconds", IntegerArgumentType.integer(0, 86400))
                  .executes(SetCooldownCommand::executeSelf)
                  .then(Commands.argument("player", EntityArgument.player()).executes(SetCooldownCommand::executeOther))
            )
      );
   }

   private static int executeSelf(CommandContext<CommandSourceStack> context) {
      if (((CommandSourceStack)context.getSource()).getEntity() instanceof ServerPlayer player) {
         return apply(context, player, "Your");
      } else {
         ((CommandSourceStack)context.getSource())
            .sendFailure(Component.literal("Only a player can use this command without naming a target."));
         return 0;
      }
   }

   private static int executeOther(CommandContext<CommandSourceStack> context) {
      ServerPlayer player;
      try {
         player = EntityArgument.getPlayer(context, "player");
      } catch (CommandSyntaxException e) {
         ((CommandSourceStack)context.getSource()).sendFailure(Component.literal("The mod cannot find this player."));
         return 0;
      }

      return apply(context, player, player.getName().getString() + "'s");
   }

   private static int apply(CommandContext<CommandSourceStack> context, ServerPlayer player, String owner) {
      int seconds = IntegerArgumentType.getInteger(context, "seconds");
      CaveInManager.setCooldownSeconds(player, seconds);
      if (seconds <= 0) {
         ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal(owner + " cooldown is now empty."), false);
      } else {
         ((CommandSourceStack)context.getSource())
            .sendSuccess(() -> Component.literal(owner + " cooldown is now " + seconds + " seconds."), false);
      }

      return 1;
   }
}
