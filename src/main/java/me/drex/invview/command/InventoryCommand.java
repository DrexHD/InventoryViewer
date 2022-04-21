package me.drex.invview.command;

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
import me.drex.invview.InvView;
import me.drex.invview.InventoryManager;
import me.drex.invview.inventory.*;
import me.drex.invview.manager.EntryManager;
import me.drex.invview.manager.SaveableEntry;
import me.drex.invview.mixin.PlayerManagerAccessor;
import me.drex.invview.mixin.WorldSaveHandlerAccessor;
import me.drex.invview.util.TextPage;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.WorldSaveHandler;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class InventoryCommand {

    public static final SuggestionProvider<ServerCommandSource> INVENTORY_TYPE = (source, builder) -> {
        List<String> names = Arrays.asList("enderchest", "inventory");
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
            ListCommand.register(inventory);
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
            final RequiredArgumentBuilder<ServerCommandSource, ItemStackArgument> item = RequiredArgumentBuilder.argument("item", ItemStackArgumentType.itemStack(new CommandRegistryAccess(DynamicRegistryManager.BUILTIN.get())));
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
        Text usage = Text.literal("//usage").formatted(Formatting.GRAY);
        source.sendFeedback(usage, false);
        return 1;
    }

    private static int scan(ServerCommandSource source, Item item, int amount, int page, boolean confirm) throws CommandSyntaxException {
        if (confirm || cachedPage == null || (cachedItem != item) || (cachedAmount != amount)) {
            MutableText title = Text.literal("\n-[").formatted(Formatting.GOLD)
                    .append(Text.literal("Found Inventories (").formatted(Formatting.GREEN)
                            .append(Text.translatable(item.getTranslationKey()).formatted(Formatting.GREEN))
                            .append(Text.literal(")").formatted(Formatting.GREEN))
                            .append(Text.literal("]-").formatted(Formatting.GOLD)));
            TextPage textPage = new TextPage(title, null, "/invview scan " + Registry.ITEM.getId(item).getPath() + " " + amount + " %s");
            loadPlayerInventories().thenAccept(savedPlayerData -> {
                for (SavedPlayerData playerData : savedPlayerData) {
                    // TODO: Rework
                    Optional<GameProfile> optional = InvView.getMinecraftServer().getUserCache().getByUuid(playerData.uuid());
                    GameProfile gameProfile = null;
                    if (optional.isPresent()) gameProfile = optional.get();
                    if (gameProfile == null) gameProfile = new GameProfile(playerData.uuid(), null);
                    MutableText name;
                    String nameArg;
                    if (gameProfile.getName() == null) {
                        name = Text.literal(gameProfile.getId().toString()).formatted(Formatting.GOLD, Formatting.ITALIC)
                                .styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, playerData.uuid().toString())));
                        nameArg = gameProfile.getId().toString();
                    } else {
                        name = Text.literal(gameProfile.getName()).formatted(Formatting.GOLD);
                        nameArg = gameProfile.getName();
                    }
                    int invCount = InventoryManager.countAll(playerData.savedInventory(), item);
                    int eChestCount = InventoryManager.countAll(playerData.savedEnderchest(), item);
                    if (invCount >= amount) {
                        MutableText text = Text.literal("Found ").formatted(Formatting.YELLOW)
                                .append(Text.literal(String.valueOf(invCount)).formatted(Formatting.GOLD))
                                .append(Text.literal(" in ").formatted(Formatting.YELLOW))
                                .append(name)
                                .append(Text.literal("'s inventory ").formatted(Formatting.YELLOW))
                                .append(Text.literal("open").formatted(Formatting.AQUA)
                                        .styled(style -> style
                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to open this inventory").formatted(Formatting.YELLOW)))
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/invview open inventory " + nameArg)).withItalic(true)));
                        textPage.addEntry(text);
                    }
                    if (eChestCount >= amount) {
                        MutableText text = Text.literal("Found ").formatted(Formatting.YELLOW)
                                .append(Text.literal(String.valueOf(eChestCount)).formatted(Formatting.GOLD))
                                .append(Text.literal(" in ").formatted(Formatting.YELLOW))
                                .append(name)
                                .append(Text.literal("'s enderchest ").formatted(Formatting.YELLOW))
                                .append(Text.literal("open").formatted(Formatting.AQUA)
                                        .styled(style -> style
                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to open this enderchest").formatted(Formatting.YELLOW)))
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/invview open enderchest " + nameArg)).withItalic(true)));
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
        EntryManager.addEntry(target.getUuid(), entry);
        source.sendFeedback(Text.literal("Saved inventory for ").formatted(Formatting.YELLOW).append(Text.literal(target.getEntityName()).formatted(Formatting.GOLD)), false);
        return 1;
    }

    private static int open(ServerCommandSource source, Collection<GameProfile> targets, InventoryType inventoryType, int id, @Nullable Collection<GameProfile> openers) throws CommandSyntaxException {
        ServerPlayerEntity opener = openers == null ? source.getPlayer() : getOnlinePlayer(openers);
        ServerPlayerEntity target = getPlayer(targets);
        Inventory inventory = getInventory(target, id, inventoryType);
        opener.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, playerEntity) ->
                new GenericContainerScreenHandler(inventoryType.type, syncId, opener.getInventory(), inventory, inventoryType.rows),
                target.getDisplayName()
        ));
        return 1;
    }

    private static int load(ServerCommandSource source, Collection<GameProfile> targets, int id, @Nullable Collection<GameProfile> openers) throws CommandSyntaxException {
        ServerPlayerEntity opener = openers == null ? source.getPlayer() : getOnlinePlayer(openers);
        ServerPlayerEntity target = getPlayer(targets);
        PlayerInventory inventory = (PlayerInventory) getInventory(target, id, InventoryType.INVENTORY);
        opener.getInventory().readNbt(inventory.writeNbt(new NbtList()));
        source.sendFeedback(Text.literal("Loaded ").formatted(Formatting.YELLOW)
                .append(Text.literal(target.getEntityName() + "'s").formatted(Formatting.GOLD))
                .append(Text.literal(" inventory for ").formatted(Formatting.YELLOW))
                .append(Text.literal(opener.getEntityName()).formatted(Formatting.GOLD)), false);
        return 1;
    }

    public static Inventory getInventory(ServerPlayerEntity target, int id, InventoryType inventoryType) throws CommandSyntaxException {
        Inventory inventory;
        if (id < 0) {
            inventory = inventoryType.linkedInventory.apply(target);
        } else {
            List<SaveableEntry> entries = EntryManager.instance.getEntries(target.getUuid());
            if (id >= entries.size()) {
                throw new SimpleCommandExceptionType(Text.literal("Invalid inventory id")).create();
            } else {
                // TODO: Get rid of inventoryType == InventoryType.INVENTORY ?
                inventory = inventoryType == InventoryType.INVENTORY ? new SavedInventory(entries.get(id).inventory.writeNbt(new NbtList())) : new SavedEnderchest(entries.get(id).enderChest.toNbtList());
            }
        }
        return inventory;
    }

    public static ServerPlayerEntity getOnlinePlayer(Collection<GameProfile> profiles) throws CommandSyntaxException {
        GameProfile requestedProfile = profiles.iterator().next();
        ServerPlayerEntity requestedPlayer = InvView.getMinecraftServer().getPlayerManager().getPlayer(requestedProfile.getName());
        if (requestedPlayer == null)
            throw new SimpleCommandExceptionType(Text.translatable("argument.entity.notfound.player")).create();
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

    private static CompletableFuture<List<SavedPlayerData>> loadPlayerInventories() {
        PlayerManager playerManager = InvView.getMinecraftServer().getPlayerManager();
        WorldSaveHandler saveHandler = ((PlayerManagerAccessor) playerManager).getWorldSaveHandler();
        String[] ids = saveHandler.getSavedPlayerIds();
        final List<CompletableFuture<SavedPlayerData>> futures = Arrays.stream(ids)
                .map(id -> {
                    try {
                        UUID uuid = UUID.fromString(id);
                        return loadPlayerInventory(uuid);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull).toList();

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(unused -> futures
                .stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    private static CompletableFuture<SavedPlayerData> loadPlayerInventory(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerManager playerManager = InvView.getMinecraftServer().getPlayerManager();
            WorldSaveHandler saveHandler = ((PlayerManagerAccessor) playerManager).getWorldSaveHandler();
            File file = new File(((WorldSaveHandlerAccessor) saveHandler).getPlayerDataDir(), uuid + ".dat");
            if (file.exists() && file.isFile()) {
                try {
                    NbtCompound tag = NbtIo.readCompressed(file);
                    SavedInventory inventory = new SavedInventory(tag.getList("Inventory", 10));
                    SavedEnderchest enderchest = new SavedEnderchest(tag.getList("EnderItems", 10));
                    return new SavedPlayerData(uuid, inventory, enderchest);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
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
                File file = new File(((WorldSaveHandlerAccessor) saveHandler).getPlayerDataDir(), id + ".dat");
                if (file.exists() && file.isFile()) {
                    try {
                        NbtCompound tag = NbtIo.readCompressed(file);
                        NbtList NbtList = tag.getList("Inventory", 10);
                        SavedInventory inventory = new SavedInventory(NbtList);
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
        InventoryType inventoryType = InventoryType.byId(input);
        if (inventoryType == null)
            throw new SimpleCommandExceptionType(Text.literal("Unknown inventory type")).create();
        return inventoryType;
    }

    public enum InventoryType {
        INVENTORY("inventory", ScreenHandlerType.GENERIC_9X5, 5, LinkedInventory::new, SavedInventory::new),
        ENDER_CHEST("enderchest", ScreenHandlerType.GENERIC_9X3, 3, LinkedEnderchest::new, SavedEnderchest::new);

        private final String id;
        private final ScreenHandlerType<GenericContainerScreenHandler> type;
        private final int rows;
        private final Function<ServerPlayerEntity, Inventory> linkedInventory;
        private final Function<NbtList, Inventory> savedInventory;

        InventoryType(String id, ScreenHandlerType<GenericContainerScreenHandler> type, int rows, Function<ServerPlayerEntity, Inventory> linkedInventory, Function<NbtList, Inventory> savedInventory) {
            this.id = id;
            this.type = type;
            this.rows = rows;
            this.linkedInventory = linkedInventory;
            this.savedInventory = savedInventory;
        }

        @Nullable
        public static InventoryType byId(String id) {
            for (InventoryType type : InventoryType.values()) {
                if (type.id.equals(id)) return type;
            }
            return null;
        }

    }

}
