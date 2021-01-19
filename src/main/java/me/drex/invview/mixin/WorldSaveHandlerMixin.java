package me.drex.invview.mixin;

import net.minecraft.world.WorldSaveHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.io.File;

@Mixin(WorldSaveHandler.class)
public interface WorldSaveHandlerMixin {

    @Accessor("playerDataDir")
    File getPlayerDataDir();

}
