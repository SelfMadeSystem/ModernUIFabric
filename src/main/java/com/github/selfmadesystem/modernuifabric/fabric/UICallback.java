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

package com.github.selfmadesystem.modernuifabric.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import icyllis.modernui.annotation.MainThread;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.annotation.UiThread;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.view.KeyEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuConstructor;

import javax.annotation.Nonnull;

/**
 * Callback of a screen. Methods will be invoked from different threads.
 * Null represents using default values.
 *
 * @see MuiFabricApi#openGui(Fragment, UICallback)
 * @see OpenMenuEvent#set(Fragment, UICallback)
 */
@Environment(EnvType.CLIENT)
public class UICallback {

    /**
     * Determine whether the key event is considered as a back key.
     * A back key will perform back stack operation and close the screen.
     * <p>
     * Call Frequency: a key pressed at least.
     * <p>
     * Default value: Escape for {@link MuiFabricApi#openGui(Fragment)}, Escape and Inventory Key (default is E)
     * for {@link MuiFabricApi#openMenu(Player, MenuConstructor)}.
     *
     * @param keyCode the key code, like {@link KeyEvent#KEY_E} (equivalent to GLFW)
     * @param event   the key event
     * @return whether the key event is considered as a back key
     */
    @UiThread
    public boolean isBackKey(int keyCode, @Nonnull KeyEvent event) {
        if (keyCode == KeyEvent.KEY_ESCAPE)
            return true;
        InputConstants.Key key = InputConstants.getKey(keyCode, event.getScanCode());
//        return Minecraft.getInstance().options.keyInventory.isActiveAndMatches(key);
        var keyInv = Minecraft.getInstance().options.keyInventory;
        return keyInv.matches(key.getValue(), key.getValue()); // ??? Is this correct?
    }

    /**
     * Should the screen be closed by the user. Otherwise, it can only be closed programmatically.
     * <p>
     * Default value: true
     *
     * @return whether the screen should close
     */
    @MainThread
    public boolean shouldClose() {
        return true;
    }

    /**
     * Determine whether the screen should pause the game. This only works in a single-player
     * world without opening to LAN. Menu screen never pause game.
     * <p>
     * Call Frequency: each tick.
     * <p>
     * Default value: true for {@link MuiFabricApi#openGui(Fragment)}, false for
     * {@link MuiFabricApi#openMenu(Player, MenuConstructor)}.
     *
     * @return whether to pause game
     */
    @MainThread
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Determine whether the screen should draw a default background. The background
     * can be configured according to user preference.
     * <p>
     * Call Frequency: each frame.
     * <p>
     * Default value: true
     *
     * @return whether to draw a default background
     */
    @RenderThread
    public boolean hasDefaultBackground() {
        return true;
    }

    /**
     * Determine whether the screen should blur the game scene when opened. The blur
     * can be configured according to user preference.
     * <p>
     * Call Frequency: the screen is showing.
     * <p>
     * Default value: true
     *
     * @return whether the game world should be blurred
     */
    @RenderThread
    public boolean shouldBlurBackground() {
        return true;
    }
}
