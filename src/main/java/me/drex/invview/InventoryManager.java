package me.drex.invview;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.drex.invview.util.inventory.SavedEnderchest;
import me.drex.invview.util.inventory.SavedInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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
            listTag.add(player.inventory.serialize(new ListTag()));
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

    public static void saveEnderchest(ServerPlayerEntity player) {
        File FILE = SAVE_PATH.toPath().resolve(player.getUuidAsString() + ".dat").toFile();
        try {
            SAVE_PATH.mkdirs();
            ListTag listTag = null;
            if (FILE.exists()) {
                CompoundTag compoundTag = NbtIo.readCompressed(new FileInputStream(FILE));
                listTag = (ListTag) compoundTag.get("enderchests");
            }
            if (listTag == null) listTag = new ListTag();
            listTag.add(player.getEnderChestInventory().getTags());
            CompoundTag newTag = new CompoundTag();
            newTag.put("enderchests", listTag);
            File file = File.createTempFile(player.getUuidAsString() + "-", ".dat", SAVE_PATH);
            NbtIo.writeCompressed(newTag, file);
            File file2 = new File(SAVE_PATH, player.getUuidAsString() + ".dat");
            File file3 = new File(SAVE_PATH, player.getUuidAsString() + ".dat_old");
            Util.backupAndReplace(file2, file, file3);
        } catch (IOException e) {
            LogManager.getLogger().error("Failed to save inventory data for {}", player.getName().getString(), e);
        }
    }

    public static SavedInventory getInventory(ServerPlayerEntity player, int id) throws CommandSyntaxException {
        File FILE = SAVE_PATH.toPath().resolve(player.getUuidAsString() + ".dat").toFile();
        try {
            SAVE_PATH.mkdirs();
            if (!FILE.exists()) throw new SimpleCommandExceptionType(new LiteralText("No saved inventories for " + player.getEntityName())).create();
            CompoundTag compoundTag = NbtIo.readCompressed(new FileInputStream(FILE));
            if (compoundTag.contains("inventories")) {
                ListTag listTag = (ListTag) compoundTag.get("inventories");
                if (id >= listTag.size()) {
                    throw new SimpleCommandExceptionType(new LiteralText("Invalid id.")).create();
                } else {
                    ListTag inventory = (ListTag) listTag.get(id);
                    SavedInventory savedInventory = new SavedInventory(inventory);
                    return savedInventory;
                }
            } else {
                throw new SimpleCommandExceptionType(new LiteralText("No saved inventories for " + player.getEntityName())).create();
            }
        } catch (IOException e) {
            LogManager.getLogger().error("Failed to load inventory data for {}", player.getName().getString(), e);
            throw new SimpleCommandExceptionType(new LiteralText("Error loading data: " + e.getMessage())).create();
        }
    }

    public static SavedEnderchest getEnderchest(ServerPlayerEntity player, int id) throws CommandSyntaxException {
        File FILE = SAVE_PATH.toPath().resolve(player.getUuidAsString() + ".dat").toFile();
        try {
            SAVE_PATH.mkdirs();
            if (!FILE.exists()) throw new SimpleCommandExceptionType(new LiteralText("No saved enderchests for " + player.getEntityName())).create();
            CompoundTag compoundTag = NbtIo.readCompressed(new FileInputStream(FILE));
            if (compoundTag.contains("inventories")) {
                ListTag listTag = (ListTag) compoundTag.get("inventories");
                if (id > listTag.size()) {
                    throw new SimpleCommandExceptionType(new LiteralText("Invalid id.")).create();
                } else {
                    ListTag enderchest = (ListTag) listTag.get(id);
                    SavedEnderchest savedEnderchest = new SavedEnderchest(enderchest);
                    return savedEnderchest;
                }
            } else {
                throw new SimpleCommandExceptionType(new LiteralText("No saved enderchests for " + player.getEntityName())).create();
            }
        } catch (IOException e) {
            LogManager.getLogger().error("Failed to load inventory data for {}", player.getName().getString(), e);
            throw new SimpleCommandExceptionType(new LiteralText("Error loading data: " + e.getMessage())).create();
        }
    }

}
