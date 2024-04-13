/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine.task;

import icyllis.arc3d.engine.*;

public final class RenderPassTask extends Task {

    DrawPass mDrawPass;
    RenderPassInfo mRenderPassInfo;
    RenderTargetProxy mTarget;

    public static RenderPassTask make(DrawPass pass,
                                      RenderPassInfo renderPassInfo,
                                      RenderTargetProxy target) {
        return null;
    }

    @Override
    public boolean prepare(ResourceProvider resourceProvider) {
        return false;
    }

    @Override
    public boolean execute(DirectContext context, CommandBuffer cmdBuffer) {
        return false;
    }
}
