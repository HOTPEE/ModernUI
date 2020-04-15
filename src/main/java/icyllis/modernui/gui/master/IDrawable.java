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
 * This is an really basic interface that represents a drawable element in gui
 * But also can be used for update animations. A.K.A Frame Event Listener
 */
public interface IDrawable {

    /**
     * Draw content you want, called every frame
     * You have to do animations update at the top of lines
     *
     * @param canvas The canvas provided by module, used to draw everything
     * @param time elapsed time from a gui open
     *                    unit: floating point ticks, 20.0 ticks = 1 second
     */
    void draw(Canvas canvas, float time);

}
