package me.drex.invview;

import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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

    public static MinecraftServer minecraftServer = InvView.getMinecraftServer();
    private static final File SAVE_PATH = new File(InvView.getDirectory() + "/inventories");

    public static void savePlayerData(ServerPlayerEntity player) {
        File playerDataDir = minecraftServer.getSavePath(WorldSavePath.PLAYERDATA).toFile();
        try {
            CompoundTag compoundTag = player.toTag(new CompoundTag());
            File file = File.createTempFile(player.getUuidAsString() + "-", ".dat", playerDataDir);
            NbtIo.writeCompressed(compoundTag, file);
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
            ListTag listTag = null;
            if (playerInventoryFile.exists()) {
                CompoundTag compoundTag = NbtIo.readCompressed(new FileInputStream(playerInventoryFile));
                listTag = (ListTag) compoundTag.get("inventories");
            }
            if (listTag == null) listTag = new ListTag();
            listTag.add(player.getInventory().serialize(new ListTag()));
            CompoundTag newTag = new CompoundTag();
            newTag.put("inventories", listTag);
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
            ItemStack itemStack = inventory.getStack(j);
            Item item1 = itemStack.getItem();
            if (item1.equals(item)) {
                i += itemStack.getCount();
            } else {
                if (item1 instanceof BlockItem) {
                    Block block = ((BlockItem) item1).getBlock();
                    if (block instanceof ShulkerBoxBlock) {
                        if (itemStack.getTag() != null) {
                            DefaultedList<ItemStack> itemStacks = DefaultedList.ofSize(inventory.size(), ItemStack.EMPTY);
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
