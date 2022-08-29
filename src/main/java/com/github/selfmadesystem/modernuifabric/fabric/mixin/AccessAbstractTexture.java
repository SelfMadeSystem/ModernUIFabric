package com.github.selfmadesystem.modernuifabric.fabric.mixin;

import net.minecraft.client.renderer.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractTexture.class)
public interface AccessAbstractTexture {
    @Accessor("id")
    int getTextureID();
}
