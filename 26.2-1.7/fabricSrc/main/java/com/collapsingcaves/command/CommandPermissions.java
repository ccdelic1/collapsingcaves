package com.collapsingcaves.command;

import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.permissions.PermissionCheck.Require;

public final class CommandPermissions {
   private CommandPermissions() {
   }

   public static Predicate<CommandSourceStack> operator() {
      return Commands.hasPermission(new Require(Permissions.COMMANDS_GAMEMASTER));
   }
}
