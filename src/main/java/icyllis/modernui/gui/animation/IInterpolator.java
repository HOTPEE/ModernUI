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

package icyllis.modernui.gui.animation;

@FunctionalInterface
public interface IInterpolator {

    IInterpolator LINEAR = in -> in;

    IInterpolator ACCELERATE = in -> in * in;

    IInterpolator DECELERATE = in -> 1.0f - (1.0f - in) * (1.0f - in);

    IInterpolator ACC_DEC = in -> (float) (Math.cos((in + 1) * Math.PI) / 2.0f) + 0.5f;

    IInterpolator SINE = in -> (float) Math.sin(Math.PI / 2 * in);

    /**
     * Get interpolation value
     * @param progress [0-1], determined by time
     * @return new value
     */
    float getInterpolation(float progress);
}
