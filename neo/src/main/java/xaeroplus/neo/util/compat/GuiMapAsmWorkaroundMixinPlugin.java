package xaeroplus.neo.util.compat;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

import java.util.List;
import java.util.Set;

/**
 * workaround ASM 9.11 throwing when visiting GuiMap's massive extractRenderState method
 */
public class GuiMapAsmWorkaroundMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(final String mixinPackage) {
        try {
            ClassWriter.class.getMethod("setComputeLimits", int.class, long.class);
        } catch (NoSuchMethodException ignored) {
            return;
        }
        try {
            var transformer = (IMixinTransformer) MixinEnvironment.getCurrentEnvironment().getActiveTransformer();
            ((Extensions) transformer.getExtensions()).add(new ComputeLimitsExtension());
        } catch (Exception e) {
            System.err.println("Failed registering GuiMapAsmWorkaroundMixinPlugin transformer extension");
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(final Set<String> myTargets, final Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(final String targetClassName, final ClassNode targetClass, final String mixinClassName, final IMixinInfo mixinInfo) {}

    @Override
    public void postApply(final String targetClassName, final ClassNode targetClass, final String mixinClassName, final IMixinInfo mixinInfo) {}

    private static class ComputeLimitsExtension implements IExtension {
        @Override
        public boolean checkActive(final MixinEnvironment environment) {
            return true;
        }

        @Override
        public void preApply(final ITargetClassContext context) {}

        @Override
        public void postApply(final ITargetClassContext context) {}

        @Override
        public void export(final MixinEnvironment environment, final String name, final boolean force, final ClassNode node) {
            if (!name.equals("xaero.map.gui.GuiMap")) return;
            for (var i = 0; i < node.methods.size(); i++) {
                var method = node.methods.get(i);
                if (!method.name.equals("extractRenderState") || method instanceof ComputeLimitsMethodNode) continue;
                var replacement = new ComputeLimitsMethodNode(method);
                method.accept(replacement);
                node.methods.set(i, replacement);
            }
        }
    }

    private static class ComputeLimitsMethodNode extends MethodNode {
        private static final int MAX_BYTES = 10 * 1024 * 1024;
        // default limits are 100m, and xaero's method is about 102m
        private static final long MAX_OPERATIONS = 200_000_000L;

        private ComputeLimitsMethodNode(final MethodNode method) {
            super(Opcodes.ASM9, method.access, method.name, method.desc, method.signature, method.exceptions.toArray(String[]::new));
        }

        @Override
        public void accept(final ClassVisitor visitor) {
            if (visitor instanceof ClassWriter writer) {
                writer.setComputeLimits(MAX_BYTES, MAX_OPERATIONS);
            }
            super.accept(visitor);
        }
    }
}
