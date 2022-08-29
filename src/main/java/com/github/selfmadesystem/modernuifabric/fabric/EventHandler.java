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

import icyllis.modernui.ModernUI;
import icyllis.modernui.core.Core;
import com.github.selfmadesystem.modernuifabric.testfabric.TestContainerMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;

@Environment(EnvType.CLIENT)
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;

/**
 * Handles game server or client events from Forge event bus
 */
@Mod.EventBusSubscriber(modid = ModernUI.ID)
final class EventHandler {

    @SubscribeEvent
    static void onRightClickItem(@Nonnull PlayerInteractEvent.RightClickItem event) {
        if (ModernUIFabric.sDevelopment) {
            final boolean diamond;
            if (event.getSide().isServer() && ((diamond = event.getItemStack().is(Items.DIAMOND))
                    || event.getItemStack().is(Items.EMERALD))) {
                MuiFabricApi.openMenu(event.getEntity(), TestContainerMenu::new, buf -> buf.writeBoolean(diamond));
            }
        }
    }

    /*@SubscribeEvent
    static void onRightClickBlock(@Nonnull PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide().isServer() && event.getHand() == InteractionHand.MAIN_HAND &&
                event.getPlayer().isShiftKeyDown() &&
                event.getWorld().getBlockState(event.getPos()).getBlock() == Blocks.GRASS_BLOCK) {
            event.getPlayer().addItem(new ItemStack(Blocks.STONE));
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }*/

    /*@SubscribeEvent
    static void onContainerClosed(PlayerContainerEvent.Close event) {

    }*/

    @Environment(EnvType.CLIENT)
    static class ClientDebug {

        /*@SubscribeEvent
        static void onRenderLevelLast(@Nonnull RenderLevelLastEvent event) {
            if (Screen.hasAltDown() &&
                    InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_KP_7)) {
                LOGGER.info("Capture from RenderLevelLastEvent");
                LOGGER.info("PoseStack.last().pose(): {}", event.getPoseStack().last().pose());
                LOGGER.info("ProjectionMatrix: {}", event.getProjectionMatrix());
            }
        }*/
    }

    /**
     * Handles game client events from Forge event bus
     */
    @Environment(EnvType.CLIENT)
    @Mod.EventBusSubscriber(modid = ModernUI.ID, value = Dist.CLIENT)
    static class Client {

        //static OptionInstance<Integer> sNewGuiScale;

        /*@Nullable
        private static Screen sCapturedVideoSettingsScreen;*/

        /*@SubscribeEvent
        static void onPlayerLogin(@Nonnull ClientPlayerNetworkEvent.LoggedInEvent event) {
            if (ModernUIForge.isDeveloperMode()) {
                LocalPlayer player = event.getPlayer();
                if (player != null && RenderCore.glCapabilitiesErrors > 0) {
                    player.sendMessage(new TextComponent("[Modern UI] There are " + RenderCore.glCapabilitiesErrors +
                            " GL capabilities that are not supported by your GPU, see debug.log for detailed info")
                            .withStyle(ChatFormatting.RED), Util.NIL_UUID);
                }
            }
        }*/

        /*@SubscribeEvent(priority = EventPriority.HIGH)
        static void onGuiOpenH(@Nonnull ScreenEvent.Opening event) {
            // TipTheScales is not good, and it also not compatible with OptiFine
            if (ModernUIForge.sInterceptTipTheScales) {
                if (event.getNewScreen() instanceof VideoSettingsScreen) {
                    sCapturedVideoSettingsScreen = event.getNewScreen();
                }
            }
        }*/

        /*@SubscribeEvent(priority = EventPriority.LOW)
        static void onGuiOpenL(@Nonnull ScreenEvent.Opening event) {
            // This event should not be cancelled
            if (sCapturedVideoSettingsScreen != null) {
                event.setNewScreen(sCapturedVideoSettingsScreen);
                sCapturedVideoSettingsScreen = null;
            }
        }*/

        /*@SubscribeEvent
        static void onGuiInit(@Nonnull ScreenEvent.Init event) {
            if (event.getScreen() instanceof VideoSettingsScreen && sNewGuiScale != null) {
                sNewGuiScale.setMaxValue(MuiForgeApi.calcGuiScales() & 0xf);
            }
        }*/

        @SubscribeEvent
        static void onRenderTick(@Nonnull TickEvent.RenderTickEvent event) {
            Core.flushMainCalls();
            Core.flushRenderCalls();
        }

        /*@SubscribeEvent(receiveCanceled = true)
        static void onGuiOpen(@Nonnull GuiOpenEvent event) {

        }

        @SubscribeEvent
        static void onRenderTick(@Nonnull TickEvent.RenderTickEvent event) {

        }

        @SubscribeEvent
        static void onClientTick(@Nonnull TickEvent.ClientTickEvent event) {

        }

        @SubscribeEvent
        public static void onKeyInput(InputEvent.KeyInputEvent event) {

        }

        @SubscribeEvent
        static void onGuiInit(GuiScreenEvent.InitGuiEvent event) {

        }

        @SubscribeEvent
        public static void onScreenStartMouseClicked(@Nonnull GuiScreenEvent.MouseClickedEvent.Pre event) {

        }

        @SubscribeEvent
        public static void onScreenStartMouseReleased(@Nonnull GuiScreenEvent.MouseReleasedEvent.Pre event) {

        }

        @SubscribeEvent
        public static void onScreenStartMouseDragged(@Nonnull GuiScreenEvent.MouseDragEvent.Pre event) {

        }*/
    }

    /*@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEventHandler {

        @SubscribeEvent
        public static void setupCommon(FMLCommonSetupEvent event) {

        }

        @Environment(EnvType.CLIENT)
        @SubscribeEvent
        public static void setupClient(FMLClientSetupEvent event) {

        }

        @Environment(EnvType.CLIENT)
        @SubscribeEvent
        public static void registerSounds(@Nonnull RegistryEvent.Register<SoundEvent> event) {

        }

        @SubscribeEvent
        public static void registerContainers(@Nonnull RegistryEvent.Register<ContainerType<?>> event) {

        }

        @SubscribeEvent
        public static void onConfigChange(@Nonnull ModConfig.ModConfigEvent event) {

        }

        @SubscribeEvent
        public static void onLoadComplete(FMLLoadCompleteEvent event) {

        }
    }*/
}
