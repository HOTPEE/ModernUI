/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.granite;

import icyllis.arc3d.core.*;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.*;

/**
 * Build a uniform data block without reordering, use std140 or std430 layout.
 * To reduce padding, we only support memory efficient types.
 */
public class UniformDataGatherer implements AutoCloseable {

    public static final int Std140Layout = 1;
    public static final int Std430Layout = 2;

    // std140 and std430 only differ in non vec4 arrays and mat2
    // we avoid using them, then 'mLayout' is ignored at the moment
    private int mLayout;
    private int mRequiredAlignment = 0;

    // pointer to native memory
    private long mStorage;
    private int mCapacity;
    private int mPosition = 0;

    private ByteBuffer mCachedView = null;

    public UniformDataGatherer(int layout) {
        mLayout = layout;
        // uniform block is usually smaller than 256 bytes
        mStorage = nmemAlloc(256);
        if (mStorage == NULL) {
            // always do explicit check
            throw new OutOfMemoryError();
        }
        mCapacity = 256;
    }

    @Override
    public void close() {
        if (mStorage != NULL) {
            nmemFree(mStorage);
        }
        mStorage = NULL;
    }

    /**
     * Reset the builder.
     */
    public void reset() {
        reset(mLayout);
    }

    /**
     * Reset the builder with new layout.
     */
    public void reset(int layout) {
        mLayout = layout;
        mRequiredAlignment = 0;
        mPosition = 0;
    }

    /**
     * Finishes the builder and returns a memory view which implements
     * {@link ByteBuffer#hashCode()} and {@link ByteBuffer#equals(Object)}.
     * The memory is managed by this object, it is valid until next reset.
     */
    public ByteBuffer finish() {
        // add tail padding
        // the max required alignment is 16, and 'mCapacity' is 16 byte aligned
        // so there will be no reallocation
        append(mRequiredAlignment, 0);
        if (mCachedView == null) {
            mCachedView = memByteBuffer(mStorage, mCapacity);
        }
        // 'mSize' is aligned here
        return mCachedView.limit(mPosition).rewind();
    }

    public void write1i(int v0) {
        long dst = append(4, 4);
        memPutInt(dst, v0);
    }

    public void write1f(float v0) {
        long dst = append(4, 4);
        memPutFloat(dst, v0);
    }

    public void write2i(int v0, int v1) {
        long dst = append(8, 8);
        memPutInt(dst, v0);
        memPutInt(dst + 4, v1);
    }

    public void write2f(float v0, float v1) {
        long dst = append(8, 8);
        memPutFloat(dst, v0);
        memPutFloat(dst + 4, v1);
    }

    public void write3f(float v0, float v1, float v2) {
        long dst = append(16, 12);
        memPutFloat(dst, v0);
        memPutFloat(dst + 4, v1);
        memPutFloat(dst + 8, v2);
    }

    public void write4i(int v0, int v1, int v2, int v3) {
        long dst = append(16, 16);
        memPutInt(dst, v0);
        memPutInt(dst + 4, v1);
        memPutInt(dst + 8, v2);
        memPutInt(dst + 12, v3);
    }

    public void write4f(float v0, float v1, float v2, float v3) {
        long dst = append(16, 16);
        memPutFloat(dst, v0);
        memPutFloat(dst + 4, v1);
        memPutFloat(dst + 8, v2);
        memPutFloat(dst + 12, v3);
    }

    /**
     * @param offset the start index in the array
     * @param count the number of float4
     */
    public void write4fv(int offset, int count, float[] value) {
        assert (count > 0);
        long dst = append(16, 16 * count);
        for (int i = 0, e = count * 4; i < e; i++) {
            memPutFloat(dst, value[offset++]);
            dst += 4;
        }
    }

    public void writeMatrix3f(Matrixc matrix) {
        long dst = append(16, 48);
        matrix.storeAligned(dst);
    }

    public void writeMatrix3f(Matrix3 matrix) {
        long dst = append(16, 48);
        matrix.storeAligned(dst);
    }

    public void writeMatrix4f(Matrix4 matrix) {
        long dst = append(16, 64);
        matrix.store(dst);
    }

    private long append(int alignment, int size) {
        int offset = mPosition;
        int padding = MathUtil.alignTo(offset, alignment) - offset;
        int count = size + padding;
        if (count == 0) {
            // this is used by finish() to ensure the padding is added
            assert size == 0;
            return NULL;
        }

        if (mCapacity - mPosition >= count) {
            mPosition += count;
        } else {
            int newSize = mPosition + count;
            int newCapacity = newSize + 4 + ((newSize + 4) >> 2);
            newCapacity = MathUtil.alignTo(newCapacity, 16);
            assert (newCapacity > 0); // uniform block won't be too large, no overflow

            mStorage = nmemRealloc(mStorage, newCapacity);
            if (mStorage == NULL) {
                // always do explicit check
                throw new OutOfMemoryError();
            }
            mCapacity = newCapacity;
            mPosition = newSize;
            mCachedView = null;
        }

        long dst = mStorage + mPosition - count;
        if (padding > 0) {
            memSet(dst, 0, padding);
            dst += padding;
        }
        assert dst == mStorage + mPosition - size;

        mRequiredAlignment = Math.max(mRequiredAlignment, alignment);
        return dst;
    }
}
