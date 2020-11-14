package me.drex.invview.inventory;

import me.drex.invview.InventoryManager;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

public class LinkedEnderchest extends EnderChestInventory {
    private ServerPlayerEntity target;
    private EnderChestInventory enderChestInventory;

    public LinkedEnderchest(ServerPlayerEntity target) {
        super();
        this.target = target;
        this.enderChestInventory = target.getEnderChestInventory();
    }

    @Override
    public void onClose(PlayerEntity player) {
        InventoryManager.savePlayerData(target);
    }

    @Override
    public void readTags(ListTag tags) {
        enderChestInventory.readTags(tags);
    }

    @Override
    public ItemStack getStack(int slot) {
        return enderChestInventory.getStack(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return enderChestInventory.removeStack(slot, amount);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        enderChestInventory.setStack(slot, stack);
    }

    public int countAll(Item item) {
        int i = 0;
        for (int j = 0; j < enderChestInventory.size(); ++j) {
            ItemStack itemStack = enderChestInventory.getStack(j);
            Item item1 = itemStack.getItem();
            if (item1.equals(item)) {
                i += itemStack.getCount();
            } else {
                if (item1 instanceof BlockItem) {
                    Block block = ((BlockItem) item1).getBlock();
                    if (block instanceof ShulkerBoxBlock) {
                        if (itemStack.getTag() != null) {
                            DefaultedList<ItemStack> itemStacks = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
                            Inventories.fromTag(itemStack.getTag().getCompound("BlockEntityTag"), itemStacks);
                            for (ItemStack itemStack2 : itemStacks) {
                                if (itemStack2.getItem().equals(item)) {
                                    i += itemStack2.getCount();
                                }
                            }
                        }
                    }
                }
            }
        }
        return i;
    }
}
