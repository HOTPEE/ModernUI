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

package icyllis.arc3d.engine;

import icyllis.arc3d.core.Matrix4;
import icyllis.arc3d.core.Rect2i;

/**
 * GPU hierarchical clipping using stencil test.
 */
public class ClipStack {

    /**
     * Clip states.
     */
    public static final byte
            STATE_EMPTY = 0,
            STATE_WIDE_OPEN = 1,
            STATE_DEVICE_RECT = 2,
            STATE_DEVICE_ROUND_RECT = 3,
            STATE_COMPLEX = 4;

    public static final class Clip {

        final Rect2i mShape = new Rect2i();

        // model view matrix
        final Matrix4 mMatrix = Matrix4.identity();
    }
}
