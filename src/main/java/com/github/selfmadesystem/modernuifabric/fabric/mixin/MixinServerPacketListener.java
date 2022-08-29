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

import com.github.selfmadesystem.modernuifabric.fabric.NetworkHandler;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinServerPacketListener {

    @Shadow
    public ServerPlayer player;

    @Shadow
    public abstract Connection getConnection();

    private final Supplier<ServerPlayer> mPlayerSupplier = () -> getConnection().isConnected() ? player : null;

    @Inject(method = "handleCustomPayload", at = @At("HEAD"))
    private void tunnelCustomPayload(@Nonnull ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        NetworkHandler.onCustomPayload(packet, mPlayerSupplier);
    }
}
