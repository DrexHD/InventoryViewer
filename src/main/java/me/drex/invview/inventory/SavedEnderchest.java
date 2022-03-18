package me.drex.invview.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.nbt.NbtList;

public class SavedEnderchest extends EnderChestInventory {

    public SavedEnderchest(NbtList NbtList) {
        super();
        this.readNbtList(NbtList);
    }

    @Override
    public void onClose(PlayerEntity player) {
        // TODO: Save data
    }
}
