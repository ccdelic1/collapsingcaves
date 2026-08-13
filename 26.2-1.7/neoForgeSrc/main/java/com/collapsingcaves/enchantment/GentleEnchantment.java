package com.collapsingcaves.enchantment;

import com.collapsingcaves.CollapsingCaves;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class GentleEnchantment {
   public static final ResourceKey<Enchantment> GENTLE = ResourceKey.create(Registries.ENCHANTMENT, CollapsingCaves.id("gentle"));

   private GentleEnchantment() {
   }

   public static boolean isOn(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }

      ItemEnchantments enchantments = (ItemEnchantments)stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

      for (Holder<Enchantment> holder : enchantments.keySet()) {
         if (holder.is(GENTLE)) {
            return true;
         }
      }

      return false;
   }
}
