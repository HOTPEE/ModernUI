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

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.Arrays;

/**
 * Used to build a packed array as the storage or lookup key of a hash map.
 * <p>
 * Note: A {@link #flush()} is expected at the end of key building.
 */
public non-sealed class KeyBuilder extends IntArrayList implements Key {

    private transient int mCurValue = 0;
    private transient int mBitsUsed = 0;  // ... in current value

    public KeyBuilder() {
    }

    public KeyBuilder(KeyBuilder other) {
        super(other);
        assert (other.mCurValue == 0 && other.mBitsUsed == 0);
    }

    /**
     * Resets this key builder to initial state.
     */
    @Override
    public final void clear() {
        assert (mCurValue == 0 && mBitsUsed == 0);
        super.clear();
    }

    /**
     * @return the number of ints
     */
    @Override
    public final int size() {
        assert (mCurValue == 0 && mBitsUsed == 0);
        return super.size();
    }

    /**
     * @return true if this key builder contains no bits
     */
    @Override
    public final boolean isEmpty() {
        assert (mCurValue == 0 && mBitsUsed == 0);
        return super.isEmpty();
    }

    public void addBits(int numBits, int value, String label) {
        assert (numBits > 0 && numBits <= Integer.SIZE);
        assert (numBits == Integer.SIZE || (Integer.SIZE - numBits <= Integer.numberOfLeadingZeros(value)));

        mCurValue |= (value << mBitsUsed);
        mBitsUsed += numBits;

        if (mBitsUsed >= Integer.SIZE) {
            // Overflow, start a new working value
            add(mCurValue);
            int excess = mBitsUsed - Integer.SIZE;
            mCurValue = excess != 0 ? (value >>> (numBits - excess)) : 0;
            mBitsUsed = excess;
        }

        assert (Integer.SIZE - mBitsUsed <= Integer.numberOfLeadingZeros(mCurValue));
    }

    public final void addBool(boolean b, String label) {
        addBits(1, b ? 1 : 0, label);
    }

    public final void addInt32(int v, String label) {
        addBits(Integer.SIZE, v, label);
    }

    public void appendComment(String comment) {
    }

    /**
     * Introduces a word-boundary in the key. Must be called before using the key with any cache,
     * but can also be called to create a break between generic data and backend-specific data.
     */
    public final void flush() {
        if (mBitsUsed != 0) {
            add(mCurValue);
            mCurValue = 0;
            mBitsUsed = 0;
        }
    }

    /**
     * Trims the backing store so that the capacity is equal to the size.
     */
    @Override
    public final void trim() {
        assert (mCurValue == 0 && mBitsUsed == 0);
        super.trim();
    }

    /**
     * @return a copy of packed int array as storage key
     */
    public final StorageKey toStorageKey() {
        assert (mCurValue == 0 && mBitsUsed == 0);
        return new StorageKey(this);
    }

    /**
     * Same as {@link Arrays#hashCode(int[])}.
     */
    @Override
    public final int hashCode() {
        assert (mCurValue == 0 && mBitsUsed == 0); // ensure flushed
        int[] e = elements();
        int h = 1, s = size();
        for (int i = 0; i < s; i++)
            h = 31 * h + e[i];
        return h;
    }

    /**
     * Compares with packed int array (storage key).
     */
    @Override
    public final boolean equals(Object o) {
        assert (mCurValue == 0 && mBitsUsed == 0); // ensure flushed
        return o instanceof StorageKey key && // check for null
                Arrays.equals(elements(), 0, size(), key.data, 0, key.data.length);
    }

    public static class StringKeyBuilder extends KeyBuilder {

        public final StringBuilder mStringBuilder = new StringBuilder();

        public StringKeyBuilder() {
        }

        @Override
        public void addBits(int numBits, int value, String label) {
            super.addBits(numBits, value, label);
            mStringBuilder.append(label)
                    .append(": ")
                    .append(value & 0xFFFFFFFFL) // to unsigned int
                    .append('\n');
        }

        @Override
        public void appendComment(String comment) {
            mStringBuilder.append(comment)
                    .append('\n');
        }

        @Override
        public String toString() {
            return mStringBuilder.toString();
        }
    }
}
