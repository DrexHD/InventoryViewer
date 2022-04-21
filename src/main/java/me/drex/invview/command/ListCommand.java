package me.drex.invview.command;

import com.google.common.collect.ImmutableList;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.drex.invview.manager.EntryManager;
import me.drex.invview.manager.SaveableEntry;
import me.drex.invview.util.TextPage;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static me.drex.invview.util.BasicUtil.formatInventory;

public class ListCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> inventory) {
        final RequiredArgumentBuilder<ServerCommandSource, Integer> page = RequiredArgumentBuilder.argument("page", IntegerArgumentType.integer(1));
        page.executes(ctx -> list(ctx.getSource(), getGameProfile(ctx, "target"), IntegerArgumentType.getInteger(ctx, "page")));
        final RequiredArgumentBuilder<ServerCommandSource, GameProfileArgumentType.GameProfileArgument> target = RequiredArgumentBuilder.argument("target", GameProfileArgumentType.gameProfile());
        target.executes(ctx -> list(ctx.getSource(), getGameProfile(ctx, "target"), 0));
        final LiteralArgumentBuilder<ServerCommandSource> list = LiteralArgumentBuilder.literal("list");
        target.then(page);
        list.then(target);
        inventory.then(list);
    }

    private static GameProfile getGameProfile(CommandContext<ServerCommandSource> ctx, String name) throws CommandSyntaxException {
        Collection<GameProfile> profileArgument = GameProfileArgumentType.getProfileArgument(ctx, name);
        if (profileArgument.isEmpty()) {
            throw new SimpleCommandExceptionType(Text.of("No player found")).create();
        } else {
            return profileArgument.iterator().next();
        }
    }

    private static int list(ServerCommandSource source, GameProfile target, int page) throws CommandSyntaxException {
        List<SaveableEntry> entryList = EntryManager.instance.getEntries(target.getId());

        MutableText title = Text.literal("\n-[").formatted(Formatting.GOLD)
                .append(Text.literal(target.getName() + "'s saved inventories").formatted(Formatting.GREEN)
                        .append(Text.literal("]-").formatted(Formatting.GOLD)));
        TextPage textPage = new TextPage(title, null, "/invview list " + target.getName() + " %s");
        int i = 1;
        for (SaveableEntry entry : entryList) {
            DefaultedList<ItemStack> defaultedList = DefaultedList.ofSize(27, ItemStack.EMPTY);
            for (int j = 0; j < 27; j++) {
                defaultedList.set(j, entry.enderChest.getStack(j));
            }
            int finalI = i;
            MutableText text = Text.literal(new SimpleDateFormat("HH:mm:ss").format(entry.date)).formatted(Formatting.YELLOW)
                    .styled(style -> style
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(new SimpleDateFormat("MMM dd yyyy").format(entry.date)).formatted(Formatting.GOLD))))
                    .append(Text.literal(" (").formatted(Formatting.YELLOW))
                    .append(Text.literal(entry.reason.substring(0, 1).toUpperCase() + entry.reason.substring(1)).formatted(Formatting.GOLD)
                            .styled(style -> Objects.nonNull(entry.description) ? style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(entry.description).formatted(Formatting.YELLOW))) : style))
                    .append(Text.literal(") ").formatted(Formatting.YELLOW))
                    .append(Text.literal("Inv: (").formatted(Formatting.YELLOW))
                    .append(formatInventory(ImmutableList.of(entry.inventory.main, entry.inventory.armor, entry.inventory.offHand)))
                    .append(Text.literal(" load ").formatted(Formatting.LIGHT_PURPLE)
                            .styled(style -> style
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to load this inventory for ").formatted(Formatting.YELLOW).append(Text.literal(target.getName()).formatted(Formatting.GOLD))))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/invview load " + target.getName() + " " + (finalI - 1))).withItalic(true)))
                    .append(Text.literal("open").formatted(Formatting.AQUA)
                            .styled(style -> style
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to open this inventory").formatted(Formatting.YELLOW)))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/invview open inventory " + target.getName() + " " + (finalI - 1))).withItalic(true)))
                    .append(Text.literal(") EC: (").formatted(Formatting.YELLOW))
                    .append(formatInventory(Collections.singletonList(defaultedList)))
                    .append(Text.literal(" open").formatted(Formatting.AQUA)
                            .styled(style -> style
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to open this enderchest").formatted(Formatting.YELLOW)))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/invview open enderchest " + target.getName() + " " + (finalI - 1))).withItalic(true)))
                    .append(Text.literal(")").formatted(Formatting.YELLOW));
            textPage.addEntry(text);
            i++;
        }
        textPage.sendPage(source.getPlayer(), page - 1, 5);
        return 1;
    }

}
