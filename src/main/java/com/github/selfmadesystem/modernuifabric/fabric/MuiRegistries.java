/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

import com.github.selfmadesystem.modernuifabric.testfabric.TestContainerMenu;
import icyllis.modernui.ModernUI;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("unused")
public final class MuiRegistries {

    /**
     * Sounds (Client only, no registration)
     */
    public static final SoundEvent BUTTON_CLICK_1 = new SoundEvent(new ResourceLocation(ModernUI.ID, "button1"));
    public static final SoundEvent BUTTON_CLICK_2 = new SoundEvent(new ResourceLocation(ModernUI.ID, "button2"));

    /**
     * Container Menus (Development only)
     */
    public static final ResourceLocation
            TEST_MENU_KEY = new ResourceLocation(ModernUI.ID, "test");
    public static final MenuType<?> TEST_MENU = Registry.register(Registry.MENU,
            TEST_MENU_KEY, new MenuType<>(TestContainerMenu::new));

    /**
     * Items (Development only)
     */
    public static final ResourceLocation
            PROJECT_BUILDER_ITEM_KEY = new ResourceLocation(ModernUI.ID, "project_builder");
    public static final RegistryObject<Item> PROJECT_BUILDER_ITEM = RegistryObject.createOptional(
            PROJECT_BUILDER_ITEM_KEY, ForgeRegistries.ITEMS.getRegistryKey(), ModernUI.ID);
}
