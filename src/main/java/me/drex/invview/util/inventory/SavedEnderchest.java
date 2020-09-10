package me.drex.invview.util.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.nbt.ListTag;

public class SavedEnderchest extends EnderChestInventory {

    public SavedEnderchest(ListTag listTag) {
        super();
        this.readTags(listTag);
    }

}
