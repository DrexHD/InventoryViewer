package me.drex.invview.manager;

import me.drex.invview.InvView;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class EntryManager extends HashMap<UUID, List<SaveableEntry>> {

    public static EntryManager instance;

    public static void load() throws IOException {
        instance = new EntryManager((InvView.DATA.exists() ? NbtIo.readCompressed(new FileInputStream(InvView.DATA)) : new CompoundTag()));
    }

    public EntryManager (CompoundTag tag) {
        for (String key : tag.getKeys()) {
            ListTag playerEntry = (ListTag) tag.get(key);
            UUID uuid = UUID.fromString(key);
            List<SaveableEntry> entries = new ArrayList<>();
            for (int i = 0; i < playerEntry.size(); i++) {
                entries.add(new SaveableEntry(playerEntry.getCompound(i)));
            }
            this.put(uuid, entries);
        }
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        for (Entry<UUID, List<SaveableEntry>> entry : this.entrySet()) {
            UUID uuid = entry.getKey();
            ListTag playerEntry = new ListTag();
            for (SaveableEntry e : entry.getValue()) {
                playerEntry.add(e.toNBT());
            }
            tag.put(uuid.toString(), playerEntry);
        }
        return tag;
    }

    public List<SaveableEntry> getEntries(UUID uuid) {
        return this.containsKey(uuid) ? this.get(uuid) : new ArrayList<>();
    }

    public void addEntry(UUID uuid, SaveableEntry e) {
        List<SaveableEntry> list = this.containsKey(uuid) ? this.get(uuid) : new ArrayList<>();
        list.add(e);
        this.put(uuid, list);
    }

    public void removeEntry(UUID uuid, SaveableEntry e) {
        List<SaveableEntry> list = this.containsKey(uuid) ? this.get(uuid) : new ArrayList<>();
        list.remove(e);
        this.put(uuid, list);
    }

}
