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

package icyllis.arcui.engine;

public class ProgramInfo {

    private int                                   mNumSamples;
    private boolean                                  mNeedsStencil;
    private BackendFormat                       mBackendFormat;
    private int                       mOrigin;
    private boolean                                  mTargetHasVkResolveAttachmentWithInput;
    private int                                   mTargetsNumSamples;
    private Pipeline                     mPipeline;
    private UserStencilSettings          mUserStencilSettings;
    private GeometryProcessor            mGeomProc;
    private byte                       mPrimitiveType;
    private int                    mRenderPassXferBarriers;
    private int                              mColorLoadOp;

    public ProgramInfo(Caps caps,
                       SurfaceProxyView targetView,
                       boolean usesMSAASurface,
                       Pipeline pipeline,
                       UserStencilSettings userStencilSettings,
                       GeometryProcessor geomProc,
                       byte primitiveType,
                       int renderPassXferBarriers,
                       int colorLoadOp) {
        mGeomProc = geomProc;
    }

    public GeometryProcessor geomProc() {
        return mGeomProc;
    }
}
