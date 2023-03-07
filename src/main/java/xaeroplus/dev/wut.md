During `runClient` the ASM transformer classes are slightly different than what Xaero's mods are expecting.

The class names for the transformers have "$wrapper." [appended to them for some Forge reason](https://github.com/MinecraftForge/FML/blob/1d627656b890ee9ae530687c16a2c288570b4386/src/main/java/net/minecraftforge/fml/common/asm/ASMTransformerWrapper.java#L178)

Xaero's transformers hardcode the class name and do a simple string comparison which fails due to this. 

But if I copy and load the plugins directly from XaeroPlus's codebase it works for some reason.

Why does he not use mixin? I don't know. Reduced dependencies? Easier to maintain compatibility with multiple mod version?

If there's another solution where I don't need to copy his classes please show/tell me. 
