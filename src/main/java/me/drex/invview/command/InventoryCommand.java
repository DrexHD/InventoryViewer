package me.drex.invview.command;

import com.google.common.collect.ImmutableList;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import jdk.internal.jline.internal.Nullable;
import me.drex.invview.InvView;
import me.drex.invview.manager.EntryManager;
import me.drex.invview.manager.SaveableEntry;
import me.drex.invview.util.inventory.LinkedInventory;
import me.drex.invview.util.inventory.SavedInventory;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.ListTag;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class InventoryCommand {

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
            final RequiredArgumentBuilder<ServerCommandSource, GameProfileArgumentType.GameProfileArgument> target = RequiredArgumentBuilder.argument("target", GameProfileArgumentType.gameProfile());
            final LiteralArgumentBuilder<ServerCommandSource> list = LiteralArgumentBuilder.literal("list");
            target.executes(ctx -> list(ctx.getSource(), GameProfileArgumentType.getProfileArgument(ctx, "target")));
            list.then(target);
            inventory.then(list);
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
        SaveableEntry entry = new SaveableEntry(target.inventory, target.getEnderChestInventory(), new Date(), "custom");
        EntryManager.instance.addEntry(target.getUuid(), entry);
        source.sendFeedback(new LiteralText("Saved inventory for ").formatted(Formatting.YELLOW).append(new LiteralText(target.getEntityName()).formatted(Formatting.GOLD)), false);
        return 1;
    }

    private static int list(ServerCommandSource source, Collection<GameProfile> targets) {
        ServerPlayerEntity target = getPlayer(targets);
        List<SaveableEntry> entryList = EntryManager.instance.getEntries(target.getUuid());
        MutableText text = new LiteralText("-[").formatted(Formatting.GOLD)
                .append(new LiteralText(target.getEntityName() + "'s saved inventories").formatted(Formatting.YELLOW)
                .append(new LiteralText("]-").formatted(Formatting.GOLD)));
        int maxItems = 20;
        int i = 1;
        for (SaveableEntry entry : entryList) {
            MutableText items = new LiteralText("");
            int itemCount = 0;
            List<DefaultedList<ItemStack>> combined = ImmutableList.of(entry.inventory.main, entry.inventory.armor, entry.inventory.offHand);
            for (DefaultedList<ItemStack> itemStacks : combined) {
                for (ItemStack itemStack : itemStacks) {
                    if (itemStack == ItemStack.EMPTY) continue;
                    if (itemCount < maxItems) {
                        items.append(itemStack.getName()).append(new LiteralText("\n"));
                    }
                    itemCount++;
                }
            }
            if (itemCount > maxItems) {
                items.append(new LiteralText("... and " + (itemCount - maxItems) + " more ...").formatted(Formatting.GRAY));
            }

            int finalI = i;
            text.append(new LiteralText("\n" + i + ". ").formatted(Formatting.GOLD))
                .append(new LiteralText( new SimpleDateFormat("HH:mm:ss").format(entry.date)).formatted(Formatting.YELLOW)
                    .styled(style -> style
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(new SimpleDateFormat("MMM dd yyyy").format(entry.date)).formatted(Formatting.GOLD)))))
                .append(new LiteralText(" (").formatted(Formatting.YELLOW))
                .append(new LiteralText(entry.reason.substring(0, 1).toUpperCase() + entry.reason.substring(1)).formatted(Formatting.GOLD))
                .append(new LiteralText(") ").formatted(Formatting.YELLOW))
                .append(new LiteralText(String.valueOf(itemCount)).formatted(Formatting.GOLD).append(new LiteralText(" items ").formatted(Formatting.YELLOW)).styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, items))))
                .append(new LiteralText("[").formatted(Formatting.WHITE))
                .append(new LiteralText("L").formatted(Formatting.GREEN)
                        .styled(style -> style
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Click to load this inventory for ").formatted(Formatting.YELLOW).append(new LiteralText(target.getEntityName()).formatted(Formatting.GOLD))))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/inventory2 load " + target.getEntityName() + " " + (finalI - 1)))))
                .append(new LiteralText("] ").formatted(Formatting.WHITE))
                .append(new LiteralText("[").formatted(Formatting.WHITE))
                .append(new LiteralText("O").formatted(Formatting.AQUA)
                        .styled(style -> style
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Click to open this inventory").formatted(Formatting.YELLOW)))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/inventory2 open " + target.getEntityName() + " " + (finalI - 1)))))
                .append(new LiteralText("]").formatted(Formatting.WHITE));
            i++;
        }

        source.sendFeedback(text, false);
        return 1;
    }

    private static int open(ServerCommandSource source, Collection<GameProfile> targets, int id, @Nullable Collection<GameProfile> openers) throws CommandSyntaxException {
        ServerPlayerEntity opener = openers == null ? source.getPlayer() : getOnlinePlayer(openers);
        ServerPlayerEntity target = getPlayer(targets);
        Inventory inventory;
        if (id == -1) {
            inventory = new LinkedInventory(target);
        } else {
            inventory = new SavedInventory(EntryManager.instance.getEntries(target.getUuid()).get(id).inventory.serialize(new ListTag()));
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
            inventory = EntryManager.instance.getEntries(target.getUuid()).get(id).inventory;
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
