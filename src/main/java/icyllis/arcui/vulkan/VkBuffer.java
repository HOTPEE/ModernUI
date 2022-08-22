/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.vulkan;

import icyllis.arcui.engine.GpuBuffer;

//TODO
public final class VkBuffer extends GpuBuffer {

    public VkBuffer(VkServer server) {
        super(server, 0, 0, 0);
    }

    @Override
    public long getMemorySize() {
        return 0;
    }

    @Override
    protected void onFree() {

    }

    @Override
    protected void onDrop() {

    }

    @Override
    protected void onMap() {

    }

    @Override
    protected void onUnmap() {

    }

    @Override
    protected boolean onUpdateData(long data, int offset, int size) {
        return false;
    }
}
