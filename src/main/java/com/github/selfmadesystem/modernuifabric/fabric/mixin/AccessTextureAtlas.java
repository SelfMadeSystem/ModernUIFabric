package com.github.selfmadesystem.modernuifabric.fabric.mixin;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(TextureAtlas.class)
public interface AccessTextureAtlas {
    @Accessor
    Map<ResourceLocation, TextureAtlasSprite> getTexturesByName();
}
