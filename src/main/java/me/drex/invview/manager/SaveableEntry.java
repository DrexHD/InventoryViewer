package me.drex.invview.manager;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.Optional;

public class SaveableEntry {

    public final PlayerInventory inventory;
    public final EnderChestInventory enderChest;
    public final Date date;
    public final String reason;
    public final Optional<String> description;

    public SaveableEntry(PlayerInventory inventory, EnderChestInventory enderChest, Date date, String reason, @Nullable String description) {
        this.description = Optional.ofNullable(description);
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
        this.description = Optional.ofNullable(tag.contains("description") ? tag.getString("description") : null);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("inventory", this.inventory.serialize(new ListTag()));
        tag.put("enderchest", this.enderChest.getTags());
        tag.putLong("date", this.date.getTime());
        tag.putString("reason", this.reason);
        this.description.ifPresent(s -> tag.putString("description", s));
        return tag;
    }

}
