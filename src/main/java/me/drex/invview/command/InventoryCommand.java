package me.drex.invview.command;

import com.google.common.collect.ImmutableList;
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
import me.drex.invview.InventoryManager;
import me.drex.invview.inventory.LinkedEnderchest;
import me.drex.invview.inventory.LinkedInventory;
import me.drex.invview.inventory.SavedEnderchest;
import me.drex.invview.inventory.SavedInventory;
import me.drex.invview.manager.EntryManager;
import me.drex.invview.manager.SaveableEntry;
import me.drex.invview.mixin.PlayerManagerAccessor;
import me.drex.invview.mixin.WorldSaveHandlerMixin;
import me.drex.invview.util.TextPage;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.WorldSaveHandler;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class InventoryCommand {

    public static final SuggestionProvider<ServerCommandSource> INVENTORY_TYPE = (source, builder) -> {
        List<String> names = Arrays.asList("echest", "inv");
        return CommandSource.suggestMatching(names, builder);
    };
    private static TextPage cachedPage = null;
    private static Item cachedItem = null;
    private static int cachedAmount = 0;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        final LiteralArgumentBuilder<ServerCommandSource> inventory = LiteralArgumentBuilder.literal("invview");
        inventory.requires(source -> source.hasPermissionLevel(2));
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
        {
            final LiteralArgumentBuilder<ServerCommandSource> confirm = literal("confirm");
            confirm.executes(ctx -> scan(ctx.getSource(), ItemStackArgumentType.getItemStackArgument(ctx, "item").getItem(), IntegerArgumentType.getInteger(ctx, "amount"), IntegerArgumentType.getInteger(ctx, "page"), true));
            final RequiredArgumentBuilder<ServerCommandSource, Integer> page = RequiredArgumentBuilder.argument("page", IntegerArgumentType.integer(1));
            page.executes(ctx -> scan(ctx.getSource(), ItemStackArgumentType.getItemStackArgument(ctx, "item").getItem(), IntegerArgumentType.getInteger(ctx, "amount"), IntegerArgumentType.getInteger(ctx, "page"), false));
            final RequiredArgumentBuilder<ServerCommandSource, Integer> amount = RequiredArgumentBuilder.argument("amount", IntegerArgumentType.integer(1));
            amount.executes(ctx -> scan(ctx.getSource(), ItemStackArgumentType.getItemStackArgument(ctx, "item").getItem(), IntegerArgumentType.getInteger(ctx, "amount"), 1, false));
            final RequiredArgumentBuilder<ServerCommandSource, ItemStackArgument> item = RequiredArgumentBuilder.argument("item", ItemStackArgumentType.itemStack());
            final LiteralArgumentBuilder<ServerCommandSource> scan = LiteralArgumentBuilder.literal("scan");
            page.then(confirm);
            amount.then(page);
            item.then(amount);
            scan.then(item);
            inventory.then(scan);
        }
        inventory.executes(ctx -> showUsage(ctx.getSource()));
        dispatcher.register(inventory);
    }

    private static int showUsage(ServerCommandSource source) {
        Text usage = new LiteralText("//usage").formatted(Formatting.GRAY);
        source.sendFeedback(usage, false);
        return 1;
    }

    private static int scan(ServerCommandSource source, Item item, int amount, int page, boolean confirm) throws CommandSyntaxException {
        if (confirm || cachedPage == null || (cachedItem != item) || (cachedAmount != amount)) {
            MutableText title = new LiteralText("\n-[").formatted(Formatting.GOLD)
                    .append(new LiteralText("Found Inventories (").formatted(Formatting.GREEN)
                            .append(new TranslatableText(item.getTranslationKey()).formatted(Formatting.GREEN))
                            .append(new LiteralText(")").formatted(Formatting.GREEN))
                            .append(new LiteralText("]-").formatted(Formatting.GOLD)));
            TextPage textPage = new TextPage(title, null, "/invview scan " + Registry.ITEM.getId(item).getPath() + " " + amount + " %s");
            getAllPlayerInventoriesAsync(map -> {
                for (Map.Entry<UUID, Pair<SavedInventory, SavedEnderchest>> entry : map.entrySet()) {
                    GameProfile gameProfile = InvView.getMinecraftServer().getUserCache().getByUuid(entry.getKey());
                    if (gameProfile == null) gameProfile = new GameProfile(entry.getKey(), null);
                    MutableText name;
                    String nameArg;
                    if (gameProfile.getName() == null) {
                        name = new LiteralText(gameProfile.getId().toString()).formatted(Formatting.GOLD, Formatting.ITALIC)
                                .styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, entry.getKey().toString())));
                        nameArg = gameProfile.getId().toString();
                    } else {
                        name = new LiteralText(gameProfile.getName()).formatted(Formatting.GOLD);
                        nameArg = gameProfile.getName();
                    }
                    int invCount = InventoryManager.countAll(entry.getValue().getLeft(), item);
                    int eChestCount = InventoryManager.countAll(entry.getValue().getRight(), item);
                    if (invCount >= amount) {
                        MutableText text = new LiteralText("Found ").formatted(Formatting.YELLOW)
                                .append(new LiteralText(String.valueOf(invCount)).formatted(Formatting.GOLD))
                                .append(new LiteralText(" in ").formatted(Formatting.YELLOW))
                                .append(name)
                                .append(new LiteralText("'s inventory ").formatted(Formatting.YELLOW))
                                .append(new LiteralText("open").formatted(Formatting.AQUA)
                                        .styled(style -> style
                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Click to open this inventory").formatted(Formatting.YELLOW)))
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/invview open inv " + nameArg)).withItalic(true)));
                        textPage.addEntry(text);
                    }
                    if (eChestCount >= amount) {
                        MutableText text = new LiteralText("Found ").formatted(Formatting.YELLOW)
                                .append(new LiteralText(String.valueOf(eChestCount)).formatted(Formatting.GOLD))
                                .append(new LiteralText(" in ").formatted(Formatting.YELLOW))
                                .append(name)
                                .append(new LiteralText("'s enderchest ").formatted(Formatting.YELLOW))
                                .append(new LiteralText("open").formatted(Formatting.AQUA)
                                        .styled(style -> style
                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Click to open this enderchest").formatted(Formatting.YELLOW)))
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/invview open echest " + nameArg)).withItalic(true)));
                        textPage.addEntry(text);
                    }
                }
                cachedPage = textPage;
                cachedItem = item;
                cachedAmount = amount;
                try {
                    textPage.sendPage(source.getPlayer(), page - 1, 5);
                } catch (CommandSyntaxException e) {
                    e.printStackTrace();
                }
            });
        } else {
            cachedPage.sendPage(source.getPlayer(), page - 1, 5);
        }

        return 1;
    }

    private static int save(ServerCommandSource source, Collection<GameProfile> targets) {
        ServerPlayerEntity target = getPlayer(targets);
        SaveableEntry entry = new SaveableEntry(target.getInventory(), target.getEnderChestInventory(), new Date(), "custom", null);
        EntryManager.instance.addEntry(target.getUuid(), entry);
        source.sendFeedback(new LiteralText("Saved inventory for ").formatted(Formatting.YELLOW).append(new LiteralText(target.getEntityName()).formatted(Formatting.GOLD)), false);
        return 1;
    }

    private static int list(ServerCommandSource source, Collection<GameProfile> targets, int page) throws CommandSyntaxException {
        ServerPlayerEntity target = getPlayer(targets);
        List<SaveableEntry> entryList = EntryManager.instance.getEntries(target.getUuid());

        MutableText title = new LiteralText("\n-[").formatted(Formatting.GOLD)
                .append(new LiteralText(target.getEntityName() + "'s saved inventories").formatted(Formatting.GREEN)
                        .append(new LiteralText("]-").formatted(Formatting.GOLD)));
        TextPage textPage = new TextPage(title, null, "/invview list " + target.getEntityName() + " %s");
        int i = 1;
        for (SaveableEntry entry : entryList) {
            DefaultedList<ItemStack> defaultedList = DefaultedList.ofSize(27, ItemStack.EMPTY);
            for (int j = 0; j < 27; j++) {
                defaultedList.set(j, entry.enderChest.getStack(j));
            }
            int finalI = i;
            MutableText text = new LiteralText(new SimpleDateFormat("HH:mm:ss").format(entry.date)).formatted(Formatting.YELLOW)
                    .styled(style -> style
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(new SimpleDateFormat("MMM dd yyyy").format(entry.date)).formatted(Formatting.GOLD))))
                    .append(new LiteralText(" (").formatted(Formatting.YELLOW))
                    .append(new LiteralText(entry.reason.substring(0, 1).toUpperCase() + entry.reason.substring(1)).formatted(Formatting.GOLD)
                            .styled(style -> entry.description.isPresent() ? style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(entry.description.get()).formatted(Formatting.YELLOW))) : style))
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
            textPage.addEntry(text);
            i++;
        }
        textPage.sendPage(source.getPlayer(), page - 1, 5);
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
                    hover.append(((MutableText) itemStack.getName()).formatted(formattings.get(i))).append(new LiteralText("\n"));
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
                new GenericContainerScreenHandler(isInv ? ScreenHandlerType.GENERIC_9X5 : ScreenHandlerType.GENERIC_9X3, syncId, opener.getInventory(), inventory, isInv ? 5 : 3),
                target.getDisplayName()
        ));
        return 1;
    }

    private static int load(ServerCommandSource source, Collection<GameProfile> targets, int id, @Nullable Collection<GameProfile> openers) throws CommandSyntaxException {
        ServerPlayerEntity opener = openers == null ? source.getPlayer() : getOnlinePlayer(openers);
        ServerPlayerEntity target = getPlayer(targets);
        PlayerInventory inventory = (PlayerInventory) getInventory(target, id, true);
        opener.getInventory().deserialize(inventory.serialize(new ListTag()));
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

    public static void getAllPlayerInventoriesAsync$2(Consumer<HashMap<ServerPlayerEntity, Pair<LinkedInventory, LinkedEnderchest>>> consumer) {
        CompletableFuture.supplyAsync(() -> {
            HashMap<ServerPlayerEntity, Pair<LinkedInventory, LinkedEnderchest>> inventories = new HashMap<>();
            PlayerManager playerManager = InvView.getMinecraftServer().getPlayerManager();
            WorldSaveHandler saveHandler = ((PlayerManagerAccessor) playerManager).getWorldSaveHandler();
            String[] ids = saveHandler.getSavedPlayerIds();
            for (String id : ids) {
                if (!id.matches("[\\w]{8}-[\\w]{4}-[\\w]{4}-[\\w]{4}-[\\w]{12}")) continue;
                UUID uuid = UUID.fromString(id);
                GameProfile gameProfile = InvView.getMinecraftServer().getUserCache().getByUuid(uuid);
                if (gameProfile == null) continue;
                ServerPlayerEntity player = getPlayer(Collections.singletonList(gameProfile));
                try {
                    inventories.put(player, new Pair<>((LinkedInventory) getInventory(player, -1, true), (LinkedEnderchest) getInventory(player, -1, false)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            consumer.accept(inventories);
            return inventories;
        });
    }

    public static void getAllPlayerInventoriesAsync(Consumer<HashMap<UUID, Pair<SavedInventory, SavedEnderchest>>> consumer) {
        CompletableFuture.supplyAsync(() -> {
            HashMap<UUID, Pair<SavedInventory, SavedEnderchest>> inventories = new HashMap<>();
            PlayerManager playerManager = InvView.getMinecraftServer().getPlayerManager();
            WorldSaveHandler saveHandler = ((PlayerManagerAccessor) playerManager).getWorldSaveHandler();
            String[] ids = saveHandler.getSavedPlayerIds();
            for (String id : ids) {
                if (!id.matches("[\\w]{8}-[\\w]{4}-[\\w]{4}-[\\w]{4}-[\\w]{12}")) continue;
                UUID uuid = UUID.fromString(id);
                File file = new File(((WorldSaveHandlerMixin) saveHandler).getPlayerDataDir(), id + ".dat");
                if (file.exists() && file.isFile()) {
                    try {
                        CompoundTag tag = NbtIo.readCompressed(file);
                        ListTag listTag = tag.getList("Inventory", 10);
                        SavedInventory inventory = new SavedInventory(listTag);
                        if (tag.contains("EnderItems", 9)) {
                            SavedEnderchest enderchest = new SavedEnderchest(tag.getList("EnderItems", 10));
                            inventories.put(uuid, new Pair<>(inventory, enderchest));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            consumer.accept(inventories);
            return inventories;
        });
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
