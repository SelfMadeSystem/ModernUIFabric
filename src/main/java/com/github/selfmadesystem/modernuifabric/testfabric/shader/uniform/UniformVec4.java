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

package com.github.selfmadesystem.modernuifabric.testfabric.shader.uniform;

import com.mojang.math.Vector4f;
import com.github.selfmadesystem.modernuifabric.testfabric.shader.ShaderUniform;
import org.lwjgl.opengl.GL20;

@Deprecated
public class UniformVec4 extends ShaderUniform<Vector4f> {

    public UniformVec4(int location) {
        super(location);
    }

    @Override
    public void load(Vector4f data) {
        if (location != -1) {
            GL20.glUniform4f(location, data.x(), data.y(), data.z(), data.w());
        }
    }
}
