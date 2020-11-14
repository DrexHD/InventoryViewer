package me.drex.invview.util;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class TextPage {

    public final MutableText title;
    public List<Text> entries;
    private final String command;
    private String number = "%s. ";
    private Formatting numberFormat = Formatting.YELLOW;

    public TextPage(MutableText title, List<Text> entries, String command) {
        this.title = title;
        this.entries = entries == null ? new ArrayList<>() : entries;
        this.command = command;
    }

    public void addEntry(Text text) {
        entries.add(text);
    }

    public void setNumberFormatting(String number, Formatting numberFormat) {
        this.number = number;
        this.numberFormat = numberFormat;
    }

    /**
     * @param player who will receive the message
     * @param page to be sent (first = 0), -1 will use the last page
     * @param entriesPerPage per page
    * */
    public void sendPage(ServerPlayerEntity player, int page, int entriesPerPage) {
        int entries = this.entries.size();
        int maxPage = (entries - 1) / entriesPerPage;
        page = page == -1 ? maxPage : page;
        //index
        int from = page * entriesPerPage;
        int to = Math.min(((page + 1) * entriesPerPage), entries);
        MutableText message = new LiteralText("").append(title);
        int currentIndex = from + 1;
        for (Text text : this.entries.subList(from, to)) {
            message.append(new LiteralText("\n" + String.format(this.number, currentIndex)).formatted(numberFormat)).append(text);
            currentIndex++;
        }
        int finalPage = page;
        message.append(new LiteralText("\n<- ").formatted(Formatting.WHITE).styled(style -> style.withBold(true)))
                .append(new LiteralText("Prev ").formatted(page > 0 ? Formatting.GOLD : Formatting.GRAY)
                        .styled(style -> finalPage > 0 ? style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format(this.command, finalPage))) : style))
                .append(new LiteralText(String.valueOf(finalPage + 1)).formatted(Formatting.GREEN))
                .append(new LiteralText(" / ").formatted(Formatting.GRAY))
                .append(new LiteralText(String.valueOf(maxPage + 1)).formatted(Formatting.GREEN))
                .append(new LiteralText(" Next").formatted(page == maxPage ? Formatting.GRAY : Formatting.GOLD)
                        .styled(style -> finalPage == maxPage ? style : style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format(this.command, finalPage + 2)))))
                .append(new LiteralText(" ->").formatted(Formatting.WHITE).styled(style -> style.withBold(true)));
        player.sendMessage(message, false);
    }



}
