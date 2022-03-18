package me.drex.invview.command;

import com.google.common.collect.ImmutableList;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.drex.invview.command.argument.SingleGameProfileArgumentType;
import me.drex.invview.manager.EntryManager;
import me.drex.invview.manager.SaveableEntry;
import me.drex.invview.util.TextPage;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static me.drex.invview.util.BasicUtil.formatInventory;

public class ListCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> inventory) {
        final RequiredArgumentBuilder<ServerCommandSource, Integer> page = RequiredArgumentBuilder.argument("page", IntegerArgumentType.integer(1));
        page.executes(ctx -> list(ctx.getSource(), SingleGameProfileArgumentType.getProfileArgument(ctx, "target"), IntegerArgumentType.getInteger(ctx, "page")));
        final RequiredArgumentBuilder<ServerCommandSource, SingleGameProfileArgumentType.SingleGameProfileArgument> target = RequiredArgumentBuilder.argument("target", SingleGameProfileArgumentType.gameProfile());
        target.executes(ctx -> list(ctx.getSource(), SingleGameProfileArgumentType.getProfileArgument(ctx, "target"), 0));
        final LiteralArgumentBuilder<ServerCommandSource> list = LiteralArgumentBuilder.literal("list");
        target.then(page);
        list.then(target);
        inventory.then(list);
    }

    private static int list(ServerCommandSource source, GameProfile target, int page) throws CommandSyntaxException {
        List<SaveableEntry> entryList = EntryManager.instance.getEntries(target.getId());

        MutableText title = new LiteralText("\n-[").formatted(Formatting.GOLD)
                .append(new LiteralText(target.getName() + "'s saved inventories").formatted(Formatting.GREEN)
                        .append(new LiteralText("]-").formatted(Formatting.GOLD)));
        TextPage textPage = new TextPage(title, null, "/invview list " + target.getName() + " %s");
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
                            .styled(style -> Objects.nonNull(entry.description) ? style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(entry.description).formatted(Formatting.YELLOW))) : style))
                    .append(new LiteralText(") ").formatted(Formatting.YELLOW))
                    .append(new LiteralText("Inv: (").formatted(Formatting.YELLOW))
                    .append(formatInventory(ImmutableList.of(entry.inventory.main, entry.inventory.armor, entry.inventory.offHand)))
                    .append(new LiteralText(" load ").formatted(Formatting.LIGHT_PURPLE)
                            .styled(style -> style
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Click to load this inventory for ").formatted(Formatting.YELLOW).append(new LiteralText(target.getName()).formatted(Formatting.GOLD))))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/invview load " + target.getName() + " " + (finalI - 1))).withItalic(true)))
                    .append(new LiteralText("open").formatted(Formatting.AQUA)
                            .styled(style -> style
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Click to open this inventory").formatted(Formatting.YELLOW)))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/invview open inventory " + target.getName() + " " + (finalI - 1))).withItalic(true)))
                    .append(new LiteralText(") EC: (").formatted(Formatting.YELLOW))
                    .append(formatInventory(Collections.singletonList(defaultedList)))
                    .append(new LiteralText(" open").formatted(Formatting.AQUA)
                            .styled(style -> style
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Click to open this enderchest").formatted(Formatting.YELLOW)))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/invview open enderchest " + target.getName() + " " + (finalI - 1))).withItalic(true)))
                    .append(new LiteralText(")").formatted(Formatting.YELLOW));
            textPage.addEntry(text);
            i++;
        }
        textPage.sendPage(source.getPlayer(), page - 1, 5);
        return 1;
    }

}
