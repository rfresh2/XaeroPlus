package xaeroplus.mixin.client.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaeroplus.XaeroPlus;
import xaeroplus.event.RespawnPointSetEvent;

import static net.minecraft.world.level.block.BedBlock.OCCUPIED;

@Mixin(BedBlock.class)
public abstract class MixinBedBlock {
    @Shadow
    public static boolean canSetSpawn(final Level level) {
        return false;
    }

    @Inject(method = "use", at = @At("HEAD"))
    public void checkBedSpawnPointSet(final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hit, final CallbackInfoReturnable<InteractionResult> cir) {
        if (player != Minecraft.getInstance().player) return;
        if (!canSetSpawn(level)) return;
        if (state.getValue(OCCUPIED)) return;
        XaeroPlus.EVENT_BUS.call(new RespawnPointSetEvent(pos));
    }
}
