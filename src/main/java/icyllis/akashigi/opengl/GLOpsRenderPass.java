/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.opengl;

import icyllis.akashigi.core.*;
import icyllis.akashigi.engine.*;

import static icyllis.akashigi.engine.Engine.*;
import static icyllis.akashigi.opengl.GLCore.*;

public final class GLOpsRenderPass extends OpsRenderPass {

    private final GLServer mServer;

    private GLCommandBuffer mCmdBuffer;
    private GLPipelineState mPipelineState;

    private int mColorLoadOp;
    private int mColorStoreOp;
    private int mStencilLoadOp;
    private int mStencilStoreOp;
    private float[] mClearColor;

    @SharedPtr
    private GBuffer mActiveIndexBuffer;
    @SharedPtr
    private GBuffer mActiveVertexBuffer;
    @SharedPtr
    private GBuffer mActiveInstanceBuffer;

    private int mPrimitiveType;

    public GLOpsRenderPass(GLServer server) {
        mServer = server;
    }

    @Override
    protected GLServer getServer() {
        return mServer;
    }

    public GLOpsRenderPass set(RenderTarget rt,
                               Rect2i bounds, int origin,
                               int colorLoadOp, int colorStoreOp,
                               int stencilLoadOp, int stencilStoreOp,
                               float[] clearColor) {
        set(rt, origin);
        mColorLoadOp = colorLoadOp;
        mColorStoreOp = colorStoreOp;
        mStencilLoadOp = stencilLoadOp;
        mStencilStoreOp = stencilStoreOp;
        mClearColor = clearColor;
        return this;
    }

    @Override
    public void begin() {
        super.begin();
        GLRenderTarget glRenderTarget = (GLRenderTarget) mRenderTarget;
        mCmdBuffer = mServer.beginRenderPass(glRenderTarget, mColorLoadOp, mStencilLoadOp, mClearColor);
    }

    @Override
    public void end() {
        mActiveIndexBuffer = Resource.move(mActiveIndexBuffer);
        mActiveVertexBuffer = Resource.move(mActiveVertexBuffer);
        mActiveInstanceBuffer = Resource.move(mActiveInstanceBuffer);
        GLRenderTarget glRenderTarget = (GLRenderTarget) mRenderTarget;
        mServer.endRenderPass(glRenderTarget, mColorStoreOp, mStencilStoreOp);
        super.end();
    }

    @Override
    protected boolean onBindPipeline(PipelineInfo pipelineInfo, Rect2f drawBounds) {
        mActiveIndexBuffer = Resource.move(mActiveIndexBuffer);
        mActiveVertexBuffer = Resource.move(mActiveVertexBuffer);
        mActiveInstanceBuffer = Resource.move(mActiveInstanceBuffer);

        mPipelineState = mServer.getPipelineBuilder().findOrCreatePipelineState(pipelineInfo);
        if (mPipelineState == null) {
            return false;
        }
        mPrimitiveType = switch (pipelineInfo.primitiveType()) {
            case PrimitiveType_Triangles -> GL_TRIANGLES;
            case PrimitiveType_TriangleStrip -> GL_TRIANGLE_STRIP;
            case PrimitiveType_Points -> GL_POINTS;
            case PrimitiveType_Lines -> GL_LINES;
            case PrimitiveType_LineStrip -> GL_LINE_STRIP;
            default -> throw new IllegalStateException();
        };

        //TODO flush RT again?
        mPipelineState.bindPipeline(mCmdBuffer);
        return true;
    }

    @Override
    public void clearColor(int left, int top, int right, int bottom,
                           float red, float green, float blue, float alpha) {
        super.clearColor(left, top, right, bottom,
                red, green, blue, alpha);
    }

    @Override
    public void clearStencil(int left, int top, int right, int bottom, boolean insideMask) {
        super.clearStencil(left, top, right, bottom, insideMask);
    }

    @Override
    protected void onBindBuffers(@SharedPtr GBuffer indexBuffer,
                                 @SharedPtr GBuffer vertexBuffer,
                                 @SharedPtr GBuffer instanceBuffer) {
        assert (mPipelineState != null);
        mPipelineState.bindBuffers(indexBuffer, vertexBuffer, instanceBuffer);
        mActiveIndexBuffer = Resource.move(mActiveIndexBuffer, indexBuffer);
        mActiveVertexBuffer = Resource.move(mActiveVertexBuffer, vertexBuffer);
        mActiveInstanceBuffer = Resource.move(mActiveInstanceBuffer, instanceBuffer);
    }

    @Override
    protected void onDraw(int vertexCount, int baseVertex) {
        glDrawArrays(mPrimitiveType, baseVertex, vertexCount);
    }

    @Override
    protected void onDrawIndexed(int indexCount, int baseIndex,
                                 int baseVertex) {
        nglDrawElementsBaseVertex(mPrimitiveType, indexCount,
                GL_UNSIGNED_SHORT, baseIndex, baseVertex);
    }

    @Override
    protected void onDrawInstanced(int instanceCount, int baseInstance,
                                   int vertexCount, int baseVertex) {
        glDrawArraysInstancedBaseInstance(mPrimitiveType, baseVertex, vertexCount,
                instanceCount, baseInstance);
    }

    @Override
    protected void onDrawIndexedInstanced(int indexCount, int baseIndex,
                                          int instanceCount, int baseInstance,
                                          int baseVertex) {
        nglDrawElementsInstancedBaseVertexBaseInstance(mPrimitiveType, indexCount,
                GL_UNSIGNED_SHORT, baseIndex, instanceCount, baseVertex, baseInstance);
    }
}
