package me.drex.invview;

import me.drex.invview.command.InventoryCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import java.io.File;

public class InvView implements ModInitializer {
    private static MinecraftServer minecraftServer;

    public static MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }
    private static final File playerInventoryPath = new File(getDirectory() + "/world/inventories");

    public static String getDirectory() {
        return System.getProperty("user.dir");
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            InventoryCommand.register(dispatcher);
        });

        ServerLifecycleEvents.SERVER_STARTING.register(this::onLogicalServerStarting);
    }

    private void onLogicalServerStarting(MinecraftServer server) {
        minecraftServer = server;
    }



}
