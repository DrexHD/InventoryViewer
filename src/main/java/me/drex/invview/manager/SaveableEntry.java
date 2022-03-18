package me.drex.invview.manager;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.Objects;

public class SaveableEntry {

    public final PlayerInventory inventory;
    public final EnderChestInventory enderChest;
    public final Date date;
    public final String reason;
    public final String description;

    public SaveableEntry(PlayerInventory inventory, EnderChestInventory enderChest, Date date, String reason, @Nullable String description) {
        this.description = description;
        this.inventory = new PlayerInventory(null);
        this.inventory.readNbt(inventory.writeNbt(new NbtList()));
        this.enderChest = new EnderChestInventory();
        this.enderChest.readNbtList(enderChest.toNbtList());
        this.date = date;
        this.reason = reason;
    }

    public SaveableEntry(NbtCompound tag) {
        PlayerInventory inventory = new PlayerInventory(null);
        inventory.writeNbt((NbtList) tag.get("inventory"));
        this.inventory = inventory;
        EnderChestInventory enderChest = new EnderChestInventory();
        enderChest.readNbtList((NbtList) tag.get("enderchest"));
        this.enderChest = enderChest;
        this.date = new Date(tag.getLong("date"));
        this.reason = tag.getString("reason");
        this.description = tag.contains("description") ? tag.getString("description") : null;
    }

    public NbtCompound toNBT() {
        NbtCompound tag = new NbtCompound();
        tag.put("inventory", this.inventory.writeNbt(new NbtList()));
        tag.put("enderchest", this.enderChest.toNbtList());
        tag.putLong("date", this.date.getTime());
        tag.putString("reason", this.reason);
        if (Objects.nonNull(this.description)) tag.putString("description", this.description);
        return tag;
    }

}
