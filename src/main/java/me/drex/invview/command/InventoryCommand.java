package me.drex.invview.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import me.drex.invview.InvView;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class InventoryCommand {

    private static MinecraftServer minecraftServer = InvView.getMinecraftServer();


    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        final LiteralArgumentBuilder<ServerCommandSource> inventory = LiteralArgumentBuilder.literal("inventory");
        {
            final RequiredArgumentBuilder<ServerCommandSource, GameProfileArgumentType.GameProfileArgument> profile = RequiredArgumentBuilder.argument("profile", GameProfileArgumentType.gameProfile());
            final LiteralArgumentBuilder<ServerCommandSource> save = LiteralArgumentBuilder.literal("save");
            profile.executes(ctx -> save(ctx.getSource()));
            save.then(profile);
            inventory.then(save);
        }
        {
            final RequiredArgumentBuilder<ServerCommandSource, GameProfileArgumentType.GameProfileArgument> target = RequiredArgumentBuilder.argument("target", GameProfileArgumentType.gameProfile());
            final RequiredArgumentBuilder<ServerCommandSource, Integer> id = RequiredArgumentBuilder.argument("id", IntegerArgumentType.integer(0));
            final RequiredArgumentBuilder<ServerCommandSource, GameProfileArgumentType.GameProfileArgument> profile = RequiredArgumentBuilder.argument("profile", GameProfileArgumentType.gameProfile());
            final LiteralArgumentBuilder<ServerCommandSource> load = LiteralArgumentBuilder.literal("load");
            id.then(target);
            profile.then(id);
            load.then(profile);
            inventory.then(load);
        }
        {
            final RequiredArgumentBuilder<ServerCommandSource, GameProfileArgumentType.GameProfileArgument> target = RequiredArgumentBuilder.argument("target", GameProfileArgumentType.gameProfile());
            final RequiredArgumentBuilder<ServerCommandSource, Integer> id = RequiredArgumentBuilder.argument("id", IntegerArgumentType.integer(0));
            final RequiredArgumentBuilder<ServerCommandSource, GameProfileArgumentType.GameProfileArgument> profile = RequiredArgumentBuilder.argument("profile", GameProfileArgumentType.gameProfile());
            final LiteralArgumentBuilder<ServerCommandSource> open = LiteralArgumentBuilder.literal("open");
            target.executes(ctx -> open(ctx.getSource(), GameProfileArgumentType.getProfileArgument(ctx, "profile"), IntegerArgumentType.getInteger(ctx, "id"), GameProfileArgumentType.getProfileArgument(ctx, "profile")));
            id.executes(ctx -> open(ctx.getSource(), GameProfileArgumentType.getProfileArgument(ctx, "profile"), IntegerArgumentType.getInteger(ctx, "id"), null));
            profile.executes(ctx -> open(ctx.getSource(), GameProfileArgumentType.getProfileArgument(ctx, "profile"), -1, null));
            id.then(target);
            profile.then(id);
            open.then(profile);
            inventory.then(open);
        }
        inventory.executes(ctx -> save(ctx.getSource()));
        dispatcher.register(inventory);
    }

    private static int showUsage(ServerCommandSource source) {
        Text usage = new LiteralText("");
        source.sendFeedback(usage, false);
        return 1;
    }

    private static int save(ServerCommandSource source) {
        return 1;
    }

    private static int open(ServerCommandSource source, Collection<GameProfile> profile, int id, @Nullable Collection<GameProfile> target) {



        return 1;
    }

    public static ServerPlayerEntity getPlayer(Collection<GameProfile> profiles) {
        GameProfile requestedProfile = profiles.iterator().next();
        ServerPlayerEntity requestedPlayer = minecraftServer.getPlayerManager().getPlayer(requestedProfile.getName());
        if (requestedPlayer == null) {
            requestedPlayer = minecraftServer.getPlayerManager().createPlayer(requestedProfile);
            minecraftServer.getPlayerManager().loadPlayerData(requestedPlayer);
        }

        return requestedPlayer;
    }

}
