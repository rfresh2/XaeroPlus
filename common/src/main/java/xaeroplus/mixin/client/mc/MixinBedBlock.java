package xaeroplus.mixin.client.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaeroplus.XaeroPlus;
import xaeroplus.event.RespawnPointSetEvent;

import static net.minecraft.world.level.block.BedBlock.OCCUPIED;

@Mixin(BedBlock.class)
public abstract class MixinBedBlock {
    @Inject(method = "useWithoutItem", at = @At("HEAD"))
    public void checkBedSpawnPointSet(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult, final CallbackInfoReturnable<InteractionResult> cir) {
        if (player != Minecraft.getInstance().player) return;
        if (level.dimension() != Level.OVERWORLD) return;
        if (state.getValue(OCCUPIED)) return;
        XaeroPlus.EVENT_BUS.call(new RespawnPointSetEvent(pos));
    }
}
