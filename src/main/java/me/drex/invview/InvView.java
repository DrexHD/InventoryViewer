package me.drex.invview;

import me.drex.invview.command.InventoryCommand;
import me.drex.invview.manager.EntryManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.IOException;

public class InvView implements ModInitializer {
    private static MinecraftServer minecraftServer;
    public static final File DATA = new File(InvView.getDirectory() + "/config/inventories.dat");

    public static MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }

    public static String getDirectory() {
        return System.getProperty("user.dir");
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> InventoryCommand.register(dispatcher));

        ServerLifecycleEvents.SERVER_STARTING.register(this::onLogicalServerStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                EntryManager.load();
            } catch (IOException e) {
                throw new RuntimeException("Could not load inventory data.");
            }
        });
    }

    private void onLogicalServerStarting(MinecraftServer server) {
        minecraftServer = server;
    }



}
