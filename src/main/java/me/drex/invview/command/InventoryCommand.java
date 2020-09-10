package me.drex.invview.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public class InventoryCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        final LiteralArgumentBuilder<ServerCommandSource> inventory = LiteralArgumentBuilder.literal("inventory");
        {
            final RequiredArgumentBuilder<ServerCommandSource, GameProfileArgumentType.GameProfileArgument> profile = RequiredArgumentBuilder.argument("profile", GameProfileArgumentType.gameProfile());
            final LiteralArgumentBuilder<ServerCommandSource> save = LiteralArgumentBuilder.literal("save");
            profile.executes(InventoryCommand::save);
            save.then(profile);
            inventory.then(save);
        }
        {
            final LiteralArgumentBuilder<ServerCommandSource> load = LiteralArgumentBuilder.literal("load");
            inventory.then(load);
        }
        {
            final LiteralArgumentBuilder<ServerCommandSource> open = LiteralArgumentBuilder.literal("open");
            inventory.then(open);
        }
        inventory.executes(InventoryCommand::showUsage);
        dispatcher.register(inventory);
    }

    private static int showUsage(CommandContext<ServerCommandSource> ctx) {
        Text usage = new LiteralText("");
        ctx.getSource().sendFeedback(usage, false);
        return 1;
    }

    private static int save(CommandContext<ServerCommandSource> ctx) {
        return 1;
    }

}
