package me.drex.invview.command.argument;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.TranslatableText;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SingleGameProfileArgumentType implements ArgumentType<SingleGameProfileArgumentType.SingleGameProfileArgument> {

    private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123");
    public static final SimpleCommandExceptionType UNKNOWN_PLAYER_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("argument.player.unknown"));

    @Override
    public SingleGameProfileArgument parse(StringReader reader) {
        int i = reader.getCursor();

        while (reader.canRead() && reader.peek() != ' ') {
            reader.skip();
        }

        String string = reader.getString().substring(i, reader.getCursor());
        return (source) -> {
            Optional<GameProfile> optional = source.getServer().getUserCache().findByName(string);
            if (optional.isPresent()) {
                return optional.get();
            } else {
                try {
                    UUID uuid = UUID.fromString(string);
                    optional = source.getServer().getUserCache().getByUuid(uuid);
                    return optional.orElseThrow(UNKNOWN_PLAYER_EXCEPTION::create);
                } catch (IllegalArgumentException e) {
                    // Invalid uuid format
                    throw UNKNOWN_PLAYER_EXCEPTION.create();
                }
            }
        };
    }

    public static GameProfile getProfileArgument(CommandContext<ServerCommandSource> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, SingleGameProfileArgumentType.SingleGameProfileArgument.class).getNames(context.getSource());
    }

    public static SingleGameProfileArgumentType gameProfile() {
        return new SingleGameProfileArgumentType();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (context.getSource() instanceof CommandSource) {
            return CommandSource.suggestMatching(((CommandSource) context.getSource()).getPlayerNames(), builder);
        } else {
            return Suggestions.empty();
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    @FunctionalInterface
    public interface SingleGameProfileArgument {
        GameProfile getNames(ServerCommandSource source) throws CommandSyntaxException;
    }

}
