/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.selfmadesystem.modernuifabric.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.github.selfmadesystem.modernuifabric.fabric.MuiFabricApi;
import com.github.selfmadesystem.modernuifabric.fabric.ScrollController;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.gui.widget.ScrollPanel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;

@Mixin(ScrollPanel.class)
public abstract class MixinScrollPanel implements ScrollController.IListener {

    @Shadow(remap = false)
    protected float scrollDistance;

    @Shadow(remap = false)
    protected abstract void applyScrollLimits();

    @Shadow(remap = false)
    protected abstract int getScrollAmount();

    @Shadow(remap = false)
    protected abstract int getMaxScroll();

    @Shadow(remap = false)
    private boolean scrolling;

    @Shadow(remap = false)
    @Final
    protected int height;

    @Shadow(remap = false)
    protected abstract int getBarHeight();

    @Shadow(remap = false)
    @Final
    private Minecraft client;

    private final ScrollController mScrollController = new ScrollController(this);

    /**
     * @author BloCamLimb
     * @reason Smoothing scrolling
     */
    @Overwrite
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (scrollY != 0) {
            mScrollController.setMaxScroll(getMaxScroll());
            mScrollController.scrollBy(Math.round(-scrollY * getScrollAmount()));
            return true;
        }
        return false;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void preRender(PoseStack matrix, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        mScrollController.update(MuiFabricApi.getElapsedTime());
    }

    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.BEFORE, target = "Lnet/minecraftforge" +
            "/client/gui/widget/ScrollPanel;drawPanel(Lcom/mojang/blaze3d/vertex/PoseStack;" +
            "IILcom/mojang/blaze3d/vertex/Tesselator;II)V"), remap = false)
    private void preDrawPanel(@Nonnull PoseStack ps, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        ps.pushPose();
        ps.translate(0,
                ((int) (((int) scrollDistance - scrollDistance) * client.getWindow().getGuiScale())) / client.getWindow().getGuiScale(), 0);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraftforge" +
            "/client/gui/widget/ScrollPanel;drawPanel(Lcom/mojang/blaze3d/vertex/PoseStack;" +
            "IILcom/mojang/blaze3d/vertex/Tesselator;II)V"), remap = false)
    private void postDrawPanel(@Nonnull PoseStack ps, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        ps.popPose();
    }

    @Override
    public void onScrollAmountUpdated(ScrollController controller, float amount) {
        scrollDistance = amount;
        applyScrollLimits();
    }

    /**
     * @author BloCamLimb
     * @reason Smooth scrolling
     */
    @Overwrite
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (scrolling) {
            int maxScroll = height - getBarHeight();
            float moved = (float) (deltaY / maxScroll);
            mScrollController.setMaxScroll(getMaxScroll());
            mScrollController.scrollBy(getMaxScroll() * moved);
            mScrollController.abortAnimation();
            return true;
        }
        return false;
    }
}
