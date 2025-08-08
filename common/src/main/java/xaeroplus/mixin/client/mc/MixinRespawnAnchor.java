package xaeroplus.mixin.client.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaeroplus.XaeroPlus;
import xaeroplus.event.RespawnPointSetEvent;

@Mixin(RespawnAnchorBlock.class)
public abstract class MixinRespawnAnchor {

    @Inject(method = "use", at = @At(
        value = "FIELD",
        opcode = Opcodes.GETSTATIC,
        target = "Lnet/minecraft/world/InteractionResult;CONSUME:Lnet/minecraft/world/InteractionResult;"
    ))
    public void checkRespawnAnchorRespawnPointSet(final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hit, final CallbackInfoReturnable<InteractionResult> cir) {
        if (player != Minecraft.getInstance().player) return;
        XaeroPlus.EVENT_BUS.call(new RespawnPointSetEvent(pos));
    }
}
