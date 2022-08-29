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

import com.mojang.blaze3d.platform.Window;
import icyllis.modernui.ModernUI;
import com.github.selfmadesystem.modernuifabric.fabric.ModernUIFabric;
import com.github.selfmadesystem.modernuifabric.fabric.MuiFabricApi;
import icyllis.modernui.math.FMath;
import icyllis.modernui.view.ViewConfiguration;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public abstract class MixinWindow {

    @Shadow
    private double guiScale;

    @Shadow
    public abstract int getWidth();

    @Shadow
    public abstract int getHeight();

    /**
     * @author BloCamLimb
     * @reason Make GUI scale more suitable, and not limited to even numbers when forceUnicode = true
     */
    @Overwrite
    public int calculateScale(int guiScaleIn, boolean forceUnicode) {
        int r = MuiFabricApi.calcGuiScales((Window) (Object) this);
        return guiScaleIn > 0 ? FMath.clamp(guiScaleIn, r >> 8 & 0xf, r & 0xf) : r >> 4 & 0xf;
    }

    @Inject(method = "setGuiScale", at = @At("HEAD"))
    private void onSetGuiScale(double scaleFactor, CallbackInfo ci) {
        int oldScale = (int) guiScale;
        int newScale = (int) scaleFactor;
        if (newScale != scaleFactor) {
            ModernUI.LOGGER.warn(ModernUI.MARKER,
                    "Gui scale {} should be an integer, some mods break this", scaleFactor);
        }
        // See standards
        ViewConfiguration.get().setViewScale(newScale * 0.5f);
        ModernUIFabric.dispatchOnWindowResize(getWidth(), getHeight(), newScale, oldScale);
    }

    @Redirect(method = "<init>",
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V",
                    ordinal = 5),
            remap = false
    )
    private void onInit(int x, int y) {
        GLFWErrorCallback callback = GLFW.glfwSetErrorCallback(null);
        try {
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 6);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
            long window = GLFW.glfwCreateWindow(1, 1, "", 0, 0);
            if (window != 0) {
                GLFW.glfwDestroyWindow(window);
                ModernUI.LOGGER.info(ModernUI.MARKER, "Promoted to OpenGL 4.6 Core Profile");
                return;
            }
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 5);
            window = GLFW.glfwCreateWindow(1, 1, "", 0, 0);
            if (window != 0) {
                GLFW.glfwDestroyWindow(window);
                ModernUI.LOGGER.info(ModernUI.MARKER, "Promoted to OpenGL 4.5 Core Profile");
                return;
            }
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1);
            window = GLFW.glfwCreateWindow(1, 1, "", 0, 0);
            if (window != 0) {
                GLFW.glfwDestroyWindow(window);
                ModernUI.LOGGER.info(ModernUI.MARKER, "Promoted to OpenGL 4.1 Core Profile");
                return;
            }
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
            window = GLFW.glfwCreateWindow(1, 1, "", 0, 0);
            if (window != 0) {
                GLFW.glfwDestroyWindow(window);
                ModernUI.LOGGER.info(ModernUI.MARKER, "Promoted to OpenGL 3.3 Core Profile");
                return;
            }
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
            ModernUI.LOGGER.debug(ModernUI.MARKER, "Fallback to OpenGL 3.2 Core Profile");
        } catch (Exception e) {
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
            ModernUI.LOGGER.debug(ModernUI.MARKER, "Fallback to OpenGL 3.2 Core Profile", e);
        } finally {
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE);
            GLFW.glfwSetErrorCallback(callback);
        }
    }
}
