package me.drex.invview.util;

import net.minecraft.item.ItemStack;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;

import java.util.Arrays;
import java.util.List;

public class BasicUtil {

    public static Text formatInventory(List<DefaultedList<ItemStack>> itemList) {
        int maxItems = 25;
        List<Formatting> formatting = Arrays.asList(Formatting.AQUA, Formatting.YELLOW, Formatting.RED);
        MutableText hover = Text.literal("");
        int itemCount = 0;
        int i = 0;
        for (DefaultedList<ItemStack> itemStacks : itemList) {
            for (ItemStack itemStack : itemStacks) {
                if (itemStack == ItemStack.EMPTY) continue;
                if (itemCount < maxItems) {
                    hover.append(((MutableText) itemStack.getName()).formatted(formatting.get(i))).append(Text.literal("\n"));
                }
                itemCount++;
            }
            i++;
        }
        if (itemCount > maxItems) {
            hover.append(Text.literal("... and " + (itemCount - maxItems) + " more ...").formatted(Formatting.GRAY));
        }
        return Text.literal(String.valueOf(itemCount)).formatted(Formatting.GOLD).append(Text.literal(" items").formatted(Formatting.YELLOW)).styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
    }

}
