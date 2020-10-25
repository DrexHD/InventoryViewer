package me.drex.invview.mixin;

import me.drex.invview.InvView;
import me.drex.invview.manager.EntryManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.FileOutputStream;
import java.io.IOException;

@Mixin(LevelStorage.Session.class)
public class LevelStorageSessionMixin {

    @Inject(method = "method_27426", at = @At("HEAD"))
    public void invviews$onsave(DynamicRegistryManager dynamicRegistryManager, SaveProperties saveProperties, CompoundTag compoundTag, CallbackInfo ci) {
        if (EntryManager.instance != null) {
            CompoundTag tag = EntryManager.instance.toNBT();
            try {
                InvView.DATA.createNewFile();
                NbtIo.writeCompressed(tag, new FileOutputStream(InvView.DATA));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
