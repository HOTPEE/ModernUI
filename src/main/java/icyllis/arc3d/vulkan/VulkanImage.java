/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.vulkan;

import icyllis.arc3d.engine.BackendFormat;
import icyllis.arc3d.engine.GpuImageBase;

import javax.annotation.Nonnull;

/**
 * Represents Vulkan images, can be used as textures or attachments.
 */
public sealed class VulkanImage extends GpuImageBase permits VulkanTexture {

    private VulkanImageInfo mInfo;

    public VulkanImage(VulkanDevice device,
                       int width, int height,
                       VulkanImageInfo info,
                       BackendFormat format,
                       int flags) {
        super(device, width, height);
        mInfo = info;
    }

    @Override
    public long getMemorySize() {
        return 0;
    }

    @Override
    protected void onRelease() {
    }

    @Override
    protected void onDiscard() {

    }

    @Override
    public int getSampleCount() {
        return mInfo.mSampleCount;
    }

    @Override
    public int getMipLevelCount() {
        return mInfo.mLevelCount;
    }

    @Nonnull
    @Override
    public BackendFormat getBackendFormat() {
        return null;
    }
}
