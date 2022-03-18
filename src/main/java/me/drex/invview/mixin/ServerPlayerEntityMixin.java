package me.drex.invview.mixin;

import com.mojang.authlib.GameProfile;
import me.drex.invview.manager.EntryManager;
import me.drex.invview.manager.SaveableEntry;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Date;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }

    @Inject(
            method = "onDeath",
            at = @At("HEAD")
    )
    public void onDeath(DamageSource source, CallbackInfo ci) {
        SaveableEntry entry = new SaveableEntry(this.getInventory(), this.getEnderChestInventory(), new Date(), "death", source.getDeathMessage((ServerPlayerEntity) (Object) this).asString());
        EntryManager.addEntry(this.getUuid(), entry);
    }

}
