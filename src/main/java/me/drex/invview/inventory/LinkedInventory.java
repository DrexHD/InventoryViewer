package me.drex.invview.inventory;

import me.drex.invview.InventoryManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class LinkedInventory extends PlayerInventory {


    private final ServerPlayerEntity target;
    private final PlayerInventory linkedInventory;

    public LinkedInventory(ServerPlayerEntity target) {
        super(target);
        this.target = target;
        this.linkedInventory = target.getInventory();
    }

    @Override
    public int size() {
        return 45;
    }

    @Override
    public void onClose(PlayerEntity player) {
        InventoryManager.savePlayerData(target);
    }

    @Override
    public ItemStack getStack(int slot) {
        return linkedInventory.getStack(slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        linkedInventory.setStack(slot, stack);
    }

    @Override
    public void markDirty() {
        linkedInventory.markDirty();
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return linkedInventory.removeStack(slot, amount);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

}
