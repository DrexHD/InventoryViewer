package me.drex.invview.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import jdk.internal.jline.internal.Nullable;
import me.drex.invview.InvView;
import me.drex.invview.manager.EntryManager;
import me.drex.invview.manager.SaveableEntry;
import me.drex.invview.inventory.LinkedEnderchest;
import me.drex.invview.inventory.LinkedInventory;
import me.drex.invview.inventory.SavedEnderchest;
import me.drex.invview.inventory.SavedInventory;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.ListTag;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;

public class InventoryCommand {

    public static final SuggestionProvider<ServerCommandSource> INVENTORY_TYPE = (source, builder) -> {
        List<String> names = Arrays.asList("echest", "inv");
        return CommandSource.suggestMatching(names, builder);
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        final LiteralArgumentBuilder<ServerCommandSource> inventory = LiteralArgumentBuilder.literal("invview");
        {
            final RequiredArgumentBuilder<ServerCommandSource, GameProfileArgumentType.GameProfileArgument> target = RequiredArgumentBuilder.argument("target", GameProfileArgumentType.gameProfile());
            final LiteralArgumentBuilder<ServerCommandSource> save = LiteralArgumentBuilder.literal("save");
            target.executes(ctx -> save(ctx.getSource(), GameProfileArgumentType.getProfileArgument(ctx, "target")));
            save.then(target);
            inventory.then(save);
        }
        {
            final RequiredArgumentBuilder<ServerCommandSource, Integer> page = RequiredArgumentBuilder.argument("page", IntegerArgumentType.integer(1));
            page.executes(ctx -> list(ctx.getSource(), GameProfileArgumentType.getProfileArgument(ctx, "target"), IntegerArgumentType.getInteger(ctx, "page")));
            final RequiredArgumentBuilder<ServerCommandSource, GameProfileArgumentType.GameProfileArgument> target = RequiredArgumentBuilder.argument("target", GameProfileArgumentType.gameProfile());
            target.executes(ctx -> list(ctx.getSource(), GameProfileArgumentType.getProfileArgument(ctx, "target"), 0));
            final LiteralArgumentBuilder<ServerCommandSource> list = LiteralArgumentBuilder.literal("list");
            target.then(page);
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
            final RequiredArgumentBuilder<ServerCommandSource, String> type = typeArgument();
            final LiteralArgumentBuilder<ServerCommandSource> open = LiteralArgumentBuilder.literal("open");
            opener.executes(ctx -> open(ctx.getSource(), GameProfileArgumentType.getProfileArgument(ctx, "target"), getInventoryType(ctx), IntegerArgumentType.getInteger(ctx, "id"), GameProfileArgumentType.getProfileArgument(ctx, "opener")));
            id.executes(ctx -> open(ctx.getSource(), GameProfileArgumentType.getProfileArgument(ctx, "target"), getInventoryType(ctx), IntegerArgumentType.getInteger(ctx, "id"), null));
            target.executes(ctx -> open(ctx.getSource(), GameProfileArgumentType.getProfileArgument(ctx, "target"), getInventoryType(ctx), -1, null));
            id.then(opener);
            target.then(id);
            type.then(target);
            open.then(type);
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

    private static int list(ServerCommandSource source, Collection<GameProfile> targets, int page) {
        page--;
        ServerPlayerEntity target = getPlayer(targets);
        List<SaveableEntry> entryList = EntryManager.instance.getEntries(target.getUuid());
        int entriesPerPage = 5;
        int entries = entryList.size();
        int maxPage = (entries - 1) / entriesPerPage;
        page = page == -1 ? maxPage : page;
        //index
        int from = page * entriesPerPage;
        int to = Math.min(((page + 1) * entriesPerPage), entries);
        MutableText text = new LiteralText("\n-[").formatted(Formatting.GOLD)
                .append(new LiteralText(target.getEntityName() + "'s saved inventories").formatted(Formatting.GREEN)
                        .append(new LiteralText("]-").formatted(Formatting.GOLD)));
        int i = from + 1;
        for (SaveableEntry entry : entryList.subList(from, to)) {
            DefaultedList<ItemStack> defaultedList = DefaultedList.ofSize(27, ItemStack.EMPTY);
            for (int j = 0; j < 27; j++) {
                defaultedList.set(j, entry.enderChest.getStack(j));
            }

            int finalI = i;
            text.append(new LiteralText("\n" + i + ". ").formatted(Formatting.GOLD))
                    .append(new LiteralText(new SimpleDateFormat("HH:mm:ss").format(entry.date)).formatted(Formatting.YELLOW)
                            .styled(style -> style
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(new SimpleDateFormat("MMM dd yyyy").format(entry.date)).formatted(Formatting.GOLD)))))
                    .append(new LiteralText(" (").formatted(Formatting.YELLOW))
                    .append(new LiteralText(entry.reason.substring(0, 1).toUpperCase() + entry.reason.substring(1)).formatted(Formatting.GOLD))
                    .append(new LiteralText(") ").formatted(Formatting.YELLOW))
                    .append(new LiteralText("Inv: (").formatted(Formatting.YELLOW))
                    .append(formatInventory(ImmutableList.of(entry.inventory.main, entry.inventory.armor, entry.inventory.offHand)))
                    .append(new LiteralText(" load ").formatted(Formatting.LIGHT_PURPLE)
                            .styled(style -> style
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Click to load this inventory for ").formatted(Formatting.YELLOW).append(new LiteralText(target.getEntityName()).formatted(Formatting.GOLD))))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/invview load " + target.getEntityName() + " " + (finalI - 1))).withItalic(true)))
                    .append(new LiteralText("open").formatted(Formatting.AQUA)
                            .styled(style -> style
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Click to open this inventory").formatted(Formatting.YELLOW)))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/invview open inv " + target.getEntityName() + " " + (finalI - 1))).withItalic(true)))
                    .append(new LiteralText(") EC: (").formatted(Formatting.YELLOW))
                    .append(formatInventory(Collections.singletonList(defaultedList)))
                    .append(new LiteralText(" open").formatted(Formatting.AQUA)
                            .styled(style -> style
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Click to open this enderchest").formatted(Formatting.YELLOW)))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/invview open echest " + target.getEntityName() + " " + (finalI - 1))).withItalic(true)))
                    .append(new LiteralText(")").formatted(Formatting.YELLOW));
            i++;
        }
        int finalPage = page;
        text.append(new LiteralText("\n<- ").formatted(Formatting.WHITE).styled(style -> style.withBold(true)))
                .append(new LiteralText("Prev " ).formatted(page > 0 ? Formatting.GOLD : Formatting.GRAY)
                    .styled(style -> finalPage > 0 ? style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/invview list " + target.getEntityName() + " " + finalPage)) : style))
                .append(new LiteralText(String.valueOf(finalPage + 1)).formatted(Formatting.GREEN))
                .append(new LiteralText(" / ").formatted(Formatting.GRAY))
                .append(new LiteralText(String.valueOf(maxPage + 1)).formatted(Formatting.GREEN))
                .append(new LiteralText(" Next").formatted(page == maxPage ? Formatting.GRAY : Formatting.GOLD)
                        .styled(style -> finalPage == maxPage ? style : style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/invview list " + target.getEntityName() + " " + (finalPage + 2)))))
                .append(new LiteralText(" ->").formatted(Formatting.WHITE).styled(style -> style.withBold(true)));

        source.sendFeedback(text, false);
        return 1;
    }

    public static Text formatInventory(List<DefaultedList<ItemStack>> itemList) {
        int maxItems = 25;
        List<Formatting> formattings = Arrays.asList(Formatting.AQUA, Formatting.YELLOW, Formatting.RED);
        MutableText hover = new LiteralText("");
        int itemCount = 0;
        int i = 0;
        for (DefaultedList<ItemStack> itemStacks : itemList) {
            for (ItemStack itemStack : itemStacks) {
                if (itemStack == ItemStack.EMPTY) continue;
                if (itemCount < maxItems) {
                    hover.append(((MutableText)itemStack.getName()).formatted(formattings.get(i))).append(new LiteralText("\n"));
                }
                itemCount++;
            }
            i++;
        }
        if (itemCount > maxItems) {
            hover.append(new LiteralText("... and " + (itemCount - maxItems) + " more ...").formatted(Formatting.GRAY));
        }
        return new LiteralText(String.valueOf(itemCount)).formatted(Formatting.GOLD).append(new LiteralText(" items").formatted(Formatting.YELLOW)).styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
    }

    private static int open(ServerCommandSource source, Collection<GameProfile> targets, InventoryType inventoryType, int id, @Nullable Collection<GameProfile> openers) throws CommandSyntaxException {
        ServerPlayerEntity opener = openers == null ? source.getPlayer() : getOnlinePlayer(openers);
        ServerPlayerEntity target = getPlayer(targets);
        boolean isInv = inventoryType == InventoryType.INVENTORY;
        Inventory inventory = getInventory(target, id, isInv);
        opener.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, playerEntity) ->
                new GenericContainerScreenHandler(isInv ? ScreenHandlerType.GENERIC_9X5 : ScreenHandlerType.GENERIC_9X3, syncId, opener.inventory, inventory, isInv ? 5 : 3),
                target.getDisplayName()
        ));
        return 1;
    }

    private static int load(ServerCommandSource source, Collection<GameProfile> targets, int id, @Nullable Collection<GameProfile> openers) throws CommandSyntaxException {
        ServerPlayerEntity opener = openers == null ? source.getPlayer() : getOnlinePlayer(openers);
        ServerPlayerEntity target = getPlayer(targets);
        PlayerInventory inventory = (PlayerInventory) getInventory(target, id, true);
        opener.inventory.deserialize(inventory.serialize(new ListTag()));
        source.sendFeedback(new LiteralText("Loaded ").formatted(Formatting.YELLOW)
                .append(new LiteralText(target.getEntityName() + "'s").formatted(Formatting.GOLD))
                .append(new LiteralText(" inventory for ").formatted(Formatting.YELLOW))
                .append(new LiteralText(opener.getEntityName()).formatted(Formatting.GOLD)), false);
        return 1;
    }

    public static Inventory getInventory(ServerPlayerEntity target, int id, boolean isInv) throws CommandSyntaxException {
        Inventory inventory;
        if (id == -1) {
            inventory = isInv ? new LinkedInventory(target) : new LinkedEnderchest(target);
        } else {
            List<SaveableEntry> entries = EntryManager.instance.getEntries(target.getUuid());
            if (id >= entries.size())
                throw new SimpleCommandExceptionType(new LiteralText("Invalid inventory id")).create();
            inventory = isInv ? new SavedInventory(entries.get(id).inventory.serialize(new ListTag())) : new SavedEnderchest(entries.get(id).enderChest.getTags());
        }
        return inventory;
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

    public static RequiredArgumentBuilder<ServerCommandSource, String> typeArgument() {
        return argument("type", word()).suggests(INVENTORY_TYPE);
    }

    public static InventoryType getInventoryType(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String input = StringArgumentType.getString(ctx, "type");
        InventoryType inventoryType = null;
        if (input.equals("inv")) inventoryType = InventoryType.INVENTORY;
        if (input.equals("echest")) inventoryType = InventoryType.ENDERCHEST;
        if (inventoryType == null) throw new SimpleCommandExceptionType(new LiteralText("Invalid type")).create();
        return inventoryType;
    }

    public enum InventoryType {
        INVENTORY(),
        ENDERCHEST();
    }

}
