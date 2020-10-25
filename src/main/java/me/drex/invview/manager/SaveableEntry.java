package me.drex.invview.manager;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.Date;

public class SaveableEntry {

    public final PlayerInventory inventory;
    public final EnderChestInventory enderChest;
    public final Date date;
    public final String reason;

    public SaveableEntry(PlayerInventory inventory, EnderChestInventory enderChest, Date date, String reason) {
        this.inventory = new PlayerInventory(null);
        this.inventory.deserialize(inventory.serialize(new ListTag()));
        this.enderChest = new EnderChestInventory();
        this.enderChest.readTags(enderChest.getTags());
        this.date = date;
        this.reason = reason;
    }

    public SaveableEntry(CompoundTag tag) {
        PlayerInventory inventory = new PlayerInventory(null);
        inventory.deserialize((ListTag) tag.get("inventory"));
        this.inventory = inventory;
        EnderChestInventory enderChest = new EnderChestInventory();
        enderChest.readTags((ListTag) tag.get("enderchest"));
        this.enderChest = enderChest;
        this.date = new Date(tag.getLong("date"));
        this.reason = tag.getString("reason");
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("inventory", this.inventory.serialize(new ListTag()));
        tag.put("enderchest", this.enderChest.getTags());
        tag.putLong("date", this.date.getTime());
        tag.putString("reason", this.reason);
        return tag;
    }

}
