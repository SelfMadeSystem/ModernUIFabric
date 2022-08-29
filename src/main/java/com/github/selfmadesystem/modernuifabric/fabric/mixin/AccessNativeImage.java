package com.github.selfmadesystem.modernuifabric.fabric.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NativeImage.class)
public interface AccessNativeImage {
    @Accessor
    long getPixels();
}
