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

package icyllis.akashigi.slang.ir;

/**
 * Abstract supertype of all statements.
 */
public abstract class Statement extends Node {

    protected Statement(int position, int kind) {
        super(position, kind);
        assert (kind >= StatementKind.kFirst && kind <= StatementKind.kLast);
    }

    /**
     * @see Node.StatementKind
     */
    public final int kind() {
        return mKind;
    }

    public boolean isEmpty() {
        return false;
    }
}
