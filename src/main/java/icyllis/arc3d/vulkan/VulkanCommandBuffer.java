/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.vulkan;

import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.DrawCommandList;
import icyllis.arc3d.granite.DrawPass;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static icyllis.arc3d.vulkan.VKCore.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public abstract class VulkanCommandBuffer extends CommandBuffer {

    protected final VkCommandBuffer mCommandBuffer;
    protected boolean mIsRecording = false;

    public VulkanCommandBuffer(VkDevice device, long handle) {
        mCommandBuffer = new VkCommandBuffer(handle, device);
    }

    @Override
    public void begin() {
        try (var stack = MemoryStack.stackPush()) {
            var beginInfo = VkCommandBufferBeginInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
                    .pInheritanceInfo(null);
            _CHECK_ERROR_(vkBeginCommandBuffer(mCommandBuffer, beginInfo));
        }
    }

    @Override
    public void end() {
        vkEndCommandBuffer(mCommandBuffer);
    }

    @Override
    public void draw(int vertexCount, int baseVertex) {
        drawInstanced(1, 0, vertexCount, baseVertex);
    }

    @Override
    public void drawIndexed(int indexCount, int baseIndex, int baseVertex) {
        drawIndexedInstanced(indexCount, baseIndex, 1, 0, baseVertex);
    }

    @Override
    public void drawInstanced(int instanceCount, int baseInstance,
                              int vertexCount, int baseVertex) {
        vkCmdDraw(mCommandBuffer,
                vertexCount,
                instanceCount,
                baseVertex,
                baseInstance);
    }

    @Override
    public void drawIndexedInstanced(int indexCount, int baseIndex,
                                     int instanceCount, int baseInstance,
                                     int baseVertex) {
        vkCmdDrawIndexed(mCommandBuffer,
                indexCount,
                instanceCount,
                baseIndex,
                baseVertex,
                baseInstance);
    }

    public void bindVertexBuffers() {
        //vkCmdBindVertexBuffers();
    }

    public boolean bindGraphicsPipeline(GraphicsPipeline graphicsPipeline) {
        //TODO
        return false;
    }

    @Override
    public void setScissor(int left, int top, int right, int bottom) {

    }

    @Override
    public void bindIndexBuffer(int indexType, Buffer buffer, long offset) {
        //vkCmdBindIndexBuffer();
    }

    @Override
    public void bindVertexBuffer(int binding, Buffer buffer, long offset) {
        // record each binding here, and bindVertexBuffers() together
    }

    @Override
    public void bindUniformBuffer(int binding, Buffer buffer, long offset, long size) {

    }

    public boolean isRecording() {
        return mIsRecording;
    }
}
