/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.gui.master;

/**
 * Used for a draggable widget, use {@link IFocuser} to focus this
 */
public interface IDraggable {

    /**
     * Called when mouse moved
     * @param mouseX mouse x pos
     * @param mouseY mouse y pos
     * @param deltaX mouse x change
     * @param deltaY mouse y change
     * @return true to cancel event
     */
    default boolean mouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        return false;
    }

    /**
     * Called when mouse released
     * @param mouseX mouse x pos
     * @param mouseY mouse y pos
     */
    default void onStopDragging(double mouseX, double mouseY) {}
}
