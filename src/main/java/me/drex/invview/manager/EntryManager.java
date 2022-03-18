package me.drex.invview.manager;

import me.drex.invview.InvView;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class EntryManager extends HashMap<UUID, List<SaveableEntry>> {

    private static final Map<UUID, List<SaveableEntry>> data = new HashMap<>();
    private static final Path PATH = InvView.DATA_DIRECTORY.toPath();


    public static EntryManager instance;

    public EntryManager(NbtCompound tag) {
        for (String key : tag.getKeys()) {
            NbtList playerEntry = (NbtList) tag.get(key);
            UUID uuid = UUID.fromString(key);
            List<SaveableEntry> entries = new ArrayList<>();
            for (int i = 0; i < playerEntry.size(); i++) {
                entries.add(new SaveableEntry(playerEntry.getCompound(i)));
            }
            this.put(uuid, entries);
        }
    }

    public EntryManager() {
    }

    public static void load() throws IOException {
        instance = new EntryManager();
    }

    public NbtCompound toNBT() {
        NbtCompound tag = new NbtCompound();
        for (Entry<UUID, List<SaveableEntry>> entry : this.entrySet()) {
            UUID uuid = entry.getKey();
            NbtList playerEntry = new NbtList();
            for (SaveableEntry e : entry.getValue()) {
                playerEntry.add(e.toNBT());
            }
            tag.put(uuid.toString(), playerEntry);
        }
        return tag;
    }

    public static void saveData() {
        try {
            PATH.toFile().mkdirs();
            for (UUID uuid : data.keySet()) {
                saveEntries(uuid);
            }
        } catch (IOException e) {
            InvView.LOGGER.error("Failed to save inventory data!", e);
        }
    }

    private static void saveEntries(UUID uuid) throws IOException {
        NbtCompound tag = new NbtCompound();
        if (data.containsKey(uuid)) {
            NbtList playerEntry = new NbtList();
            for (SaveableEntry entry : data.get(uuid)) {
                playerEntry.add(entry.toNBT());
            }
            tag.put("saved_inventories", playerEntry);
            File file = PATH.resolve(uuid.toString() + ".dat").toFile();
            file.createNewFile();
            NbtIo.writeCompressed(tag, new FileOutputStream(file));
        }
    }

    public static List<SaveableEntry> getEntries(UUID uuid) {
        if (data.containsKey(uuid)) {
            return data.get(uuid);
        } else {
            File file = InvView.DATA_DIRECTORY.toPath().resolve(uuid.toString() + ".dat").toFile();
            List<SaveableEntry> entries = new ArrayList<>();
            if (file.exists()) {
                NbtCompound tag;
                try {
                    tag = NbtIo.readCompressed(new FileInputStream(file));
                } catch (IOException e) {
                    InvView.LOGGER.error("Failed to load inventory data for " + uuid, e);
                    return new ArrayList<>();
                }
                NbtList playerEntry = (NbtList) tag.get("saved_inventories");
                if (playerEntry != null) {
                    for (int i = 0; i < playerEntry.size(); i++) {
                        entries.add(new SaveableEntry(playerEntry.getCompound(i)));
                    }
                }
            }
            data.put(uuid, entries);
            return entries;
        }
    }

    public static void addEntry(UUID uuid, SaveableEntry e) {
        List<SaveableEntry> list = data.containsKey(uuid) ? data.get(uuid) : new ArrayList<>();
        list.add(e);
        data.put(uuid, list);
    }

    public static void removeEntry(UUID uuid, SaveableEntry e) {
        List<SaveableEntry> list = data.containsKey(uuid) ? data.get(uuid) : new ArrayList<>();
        list.remove(e);
        data.put(uuid, list);
    }

}
