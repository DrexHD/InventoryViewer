package me.drex.invview.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtList;

public class SavedInventory extends PlayerInventory {

    public SavedInventory(NbtList NbtList) {
        super(null);
        this.writeNbt(NbtList);
    }

    @Override
    public int size() {
        return 45;
    }

    @Override
    public void onClose(PlayerEntity player) {
        // TODO: Save data
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

}
