package me.drex.invview.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.ListTag;

import java.util.function.Predicate;

public class SavedInventory extends PlayerInventory {

    public SavedInventory(ListTag listTag) {
        super(null);
        this.deserialize(listTag);
    }

    @Override
    public int size() {
        return 45;
    }

    public int remove(Predicate<ItemStack> shouldRemove, int maxCount, Inventory craftingInventory) {
        return 0;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

}
