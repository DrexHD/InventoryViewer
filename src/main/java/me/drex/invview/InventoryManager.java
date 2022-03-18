package me.drex.invview;

import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.collection.DefaultedList;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.FileInputStream;

public class InventoryManager {

    private static final File SAVE_PATH = new File(InvView.getDirectory() + "/inventories");
    public static MinecraftServer minecraftServer = InvView.getMinecraftServer();

    public static void savePlayerData(ServerPlayerEntity player) {
        File playerDataDir = minecraftServer.getSavePath(WorldSavePath.PLAYERDATA).toFile();
        try {
            NbtCompound NbtCompound = player.writeNbt(new NbtCompound());
            File file = File.createTempFile(player.getUuidAsString() + "-", ".dat", playerDataDir);
            NbtIo.writeCompressed(NbtCompound, file);
            File file2 = new File(playerDataDir, player.getUuidAsString() + ".dat");
            File file3 = new File(playerDataDir, player.getUuidAsString() + ".dat_old");
            Util.backupAndReplace(file2, file, file3);
        } catch (Exception var6) {
            LogManager.getLogger().warn("Failed to save player data for {}", player.getName().getString());
        }
    }

    public static void saveInventory(ServerPlayerEntity player) {
        File playerInventoryFile = SAVE_PATH.toPath().resolve(player.getUuidAsString() + ".dat").toFile();
        try {
            SAVE_PATH.mkdirs();
            NbtList NbtList = null;
            if (playerInventoryFile.exists()) {
                NbtCompound NbtCompound = NbtIo.readCompressed(new FileInputStream(playerInventoryFile));
                NbtList = (NbtList) NbtCompound.get("inventories");
            }
            if (NbtList == null) NbtList = new NbtList();
            NbtList.add(player.getInventory().writeNbt(new NbtList()));
            NbtCompound newTag = new NbtCompound();
            newTag.put("inventories", NbtList);
            File file = File.createTempFile(player.getUuidAsString() + "-", ".dat", SAVE_PATH);
            NbtIo.writeCompressed(newTag, file);
            File file2 = new File(SAVE_PATH, player.getUuidAsString() + ".dat");
            File file3 = new File(SAVE_PATH, player.getUuidAsString() + ".dat_old");
            Util.backupAndReplace(file2, file, file3);
        } catch (Exception e) {
            LogManager.getLogger().error("Failed to save inventory data for {}", player.getName().getString(), e);
        }
    }

    public static int countAll(Inventory inventory, Item item) {
        int i = 0;
        for (int j = 0; j < inventory.size(); ++j) {
            i += count(inventory.getStack(j), item);
        }
        return i;
    }

    public static int count(ItemStack itemStack, Item lookingFor) {
        int count = 0;
        NbtCompound NbtCompound = itemStack.getNbt();
        if (itemStack.getItem().equals(lookingFor)) count += itemStack.getCount();
        if (NbtCompound == null) return count;
        NbtCompound items;
        if (NbtCompound.contains("BlockEntityTag")) {
            items = NbtCompound.getCompound("BlockEntityTag");
        } else if (NbtCompound.contains("Items")) {
            items = NbtCompound;
        } else {
            return count;
        }
        DefaultedList<ItemStack> itemList = DefaultedList.ofSize(items.getList("Items", 10).size(), ItemStack.EMPTY);
        Inventories.readNbt(items, itemList);
        for (ItemStack item : itemList) {
            count += count(item, lookingFor);
        }
        return count;

    }

}
