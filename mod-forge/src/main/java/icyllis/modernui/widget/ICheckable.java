/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.widget;

/**
 * Defines an extension for views that make them checkable.
 */
public interface ICheckable {

    /**
     * Change the checked state of the view
     *
     * @param checked The new checked state
     */
    void setChecked(boolean checked);

    /**
     * Get the current checked state of the view
     *
     * @return The current checked state of the view
     */
    boolean isChecked();

    /**
     * Change the checked state of the view to the inverse of its current state
     */
    void toggle();
}
