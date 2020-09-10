package me.drex.invview.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.drex.invview.InvView;
import me.drex.invview.InventoryManager;
import me.drex.invview.util.inventory.LinkedInventory;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.ListTag;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class InventoryCommand {

//    private static MinecraftServer minecraftServer = InvView.getMinecraftServer();


    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        final LiteralArgumentBuilder<ServerCommandSource> inventory = LiteralArgumentBuilder.literal("inventory2");
        {
            final RequiredArgumentBuilder<ServerCommandSource, GameProfileArgumentType.GameProfileArgument> target = RequiredArgumentBuilder.argument("target", GameProfileArgumentType.gameProfile());
            final LiteralArgumentBuilder<ServerCommandSource> save = LiteralArgumentBuilder.literal("save");
            target.executes(ctx -> save(ctx.getSource(), GameProfileArgumentType.getProfileArgument(ctx, "target")));
            save.then(target);
            inventory.then(save);
        }
        {
            final RequiredArgumentBuilder<ServerCommandSource, GameProfileArgumentType.GameProfileArgument> opener = RequiredArgumentBuilder.argument("opener", GameProfileArgumentType.gameProfile());
            final RequiredArgumentBuilder<ServerCommandSource, Integer> id = RequiredArgumentBuilder.argument("id", IntegerArgumentType.integer(0));
            final RequiredArgumentBuilder<ServerCommandSource, GameProfileArgumentType.GameProfileArgument> target = RequiredArgumentBuilder.argument("target", GameProfileArgumentType.gameProfile());
            final LiteralArgumentBuilder<ServerCommandSource> load = LiteralArgumentBuilder.literal("load");
            opener.executes(ctx -> load(ctx.getSource(), GameProfileArgumentType.getProfileArgument(ctx, "target"), IntegerArgumentType.getInteger(ctx, "id"), GameProfileArgumentType.getProfileArgument(ctx, "opener")));
            id.executes(ctx -> load(ctx.getSource(), GameProfileArgumentType.getProfileArgument(ctx, "target"), IntegerArgumentType.getInteger(ctx, "id"), null));
            target.executes(ctx -> load(ctx.getSource(), GameProfileArgumentType.getProfileArgument(ctx, "target"), -1, null));
            id.then(opener);
            target.then(id);
            load.then(target);
            inventory.then(load);
        }
        {
            final RequiredArgumentBuilder<ServerCommandSource, GameProfileArgumentType.GameProfileArgument> opener = RequiredArgumentBuilder.argument("opener", GameProfileArgumentType.gameProfile());
            final RequiredArgumentBuilder<ServerCommandSource, Integer> id = RequiredArgumentBuilder.argument("id", IntegerArgumentType.integer(0));
            final RequiredArgumentBuilder<ServerCommandSource, GameProfileArgumentType.GameProfileArgument> target = RequiredArgumentBuilder.argument("target", GameProfileArgumentType.gameProfile());
            final LiteralArgumentBuilder<ServerCommandSource> open = LiteralArgumentBuilder.literal("open");
            opener.executes(ctx -> open(ctx.getSource(), GameProfileArgumentType.getProfileArgument(ctx, "target"), IntegerArgumentType.getInteger(ctx, "id"), GameProfileArgumentType.getProfileArgument(ctx, "opener")));
            id.executes(ctx -> open(ctx.getSource(), GameProfileArgumentType.getProfileArgument(ctx, "target"), IntegerArgumentType.getInteger(ctx, "id"), null));
            target.executes(ctx -> open(ctx.getSource(), GameProfileArgumentType.getProfileArgument(ctx, "target"), -1, null));
            id.then(opener);
            target.then(id);
            open.then(target);
            inventory.then(open);
        }
        inventory.executes(ctx -> showUsage(ctx.getSource()));
        dispatcher.register(inventory);
    }

    private static int showUsage(ServerCommandSource source) {
        Text usage = new LiteralText("//usage").formatted(Formatting.GRAY);
        source.sendFeedback(usage, false);
        return 1;
    }

    private static int save(ServerCommandSource source, Collection<GameProfile> targets) {
        ServerPlayerEntity target = getPlayer(targets);
        InventoryManager.saveInventory(target);
        source.sendFeedback(new LiteralText("Saved inventory for ").formatted(Formatting.YELLOW).append(new LiteralText(target.getEntityName()).formatted(Formatting.GOLD)), false);
        return 1;
    }

    private static int open(ServerCommandSource source, Collection<GameProfile> targets, int id, @Nullable Collection<GameProfile> openers) throws CommandSyntaxException {
        ServerPlayerEntity opener = openers == null ? source.getPlayer() : getOnlinePlayer(openers);
        ServerPlayerEntity target = getPlayer(targets);
        Inventory inventory;
        if (id == -1) {
            inventory = new LinkedInventory(target);
        } else {
            inventory = InventoryManager.getInventory(target, id);
        }
        opener.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, playerEntity) ->
                new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X5, syncId, opener.inventory, inventory, 5),
                target.getDisplayName()
        ));
        return 1;
    }

    private static int load(ServerCommandSource source, Collection<GameProfile> targets, int id, @Nullable Collection<GameProfile> openers) throws CommandSyntaxException {
        ServerPlayerEntity opener = openers == null ? source.getPlayer() : getOnlinePlayer(openers);
        ServerPlayerEntity target = getPlayer(targets);
        PlayerInventory inventory;
        if (id == -1) {
            inventory = new LinkedInventory(target);
        } else {
            inventory = InventoryManager.getInventory(target, id);
        }
        opener.inventory.deserialize(inventory.serialize(new ListTag()));
        source.sendFeedback(new LiteralText("Loaded ").formatted(Formatting.YELLOW)
                .append(new LiteralText(target.getEntityName() + "'s").formatted(Formatting.GOLD))
                .append(new LiteralText( " inventory for ").formatted(Formatting.YELLOW))
                .append(new LiteralText(opener.getEntityName()).formatted(Formatting.GOLD)), false);
        return 1;
    }

    public static ServerPlayerEntity getOnlinePlayer(Collection<GameProfile> profiles) throws CommandSyntaxException {
        GameProfile requestedProfile = profiles.iterator().next();
        ServerPlayerEntity requestedPlayer = InvView.getMinecraftServer().getPlayerManager().getPlayer(requestedProfile.getName());
        if (requestedPlayer == null)
            throw new SimpleCommandExceptionType(new TranslatableText("argument.entity.notfound.player")).create();
        return requestedPlayer;
    }

    public static ServerPlayerEntity getPlayer(Collection<GameProfile> profiles) {
        GameProfile profile = profiles.iterator().next();
        ServerPlayerEntity requestedPlayer = InvView.getMinecraftServer().getPlayerManager().getPlayer(profile.getId());
        if (requestedPlayer == null) {
            requestedPlayer = InvView.getMinecraftServer().getPlayerManager().createPlayer(profile);
            InvView.getMinecraftServer().getPlayerManager().loadPlayerData(requestedPlayer);
        }
        return requestedPlayer;
    }

}
