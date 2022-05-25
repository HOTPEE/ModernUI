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

package icyllis.arcui.core;

import org.lwjgl.system.*;

import java.nio.IntBuffer;

import static org.lwjgl.system.APIUtil.apiGetFunctionAddress;
import static org.lwjgl.system.Checks.*;
import static org.lwjgl.system.JNI.*;
import static org.lwjgl.system.MemoryUtil.memAddressSafe;

/**
 * Native bindings to kernel32.dll.
 */
public class Kernel32 {

    private static final SharedLibrary KERNEL32 = Library.loadNative(Kernel32.class, "org.lwjgl", "kernel32");

    /**
     * Contains the function pointers loaded from the kernel32 {@link SharedLibrary}.
     */
    public static final class Functions {

        private Functions() {
        }

        /**
         * Function address.
         */
        public static final long
                CloseHandle = apiGetFunctionAddress(KERNEL32, "CloseHandle"),
                GetHandleInformation = apiGetFunctionAddress(KERNEL32, "GetHandleInformation"),
                GetLastError = apiGetFunctionAddress(KERNEL32, "GetLastError");

    }

    /**
     * Returns the kernel32 {@link SharedLibrary}.
     */
    public static SharedLibrary getLibrary() {
        return KERNEL32;
    }

    public static final int
            HANDLE_FLAG_INHERIT = 0x00000001,
            HANDLE_FLAG_PROTECT_FROM_CLOSE = 0x00000002;

    /**
     * Closes an open object handle.
     *
     * @param hObject A valid handle to an open object.
     * @return If the function succeeds, the return value is nonzero.
     */
    @NativeType("BOOL")
    public static boolean CloseHandle(@NativeType("HANDLE") long hObject) {
        long __functionAddress = Functions.CloseHandle;
        return callPI(hObject, __functionAddress) != 0;
    }

    /**
     * Retrieves certain properties of an object handle.
     *
     * @param hObject   A handle to an object whose information is to be retrieved.
     * @param lpdwFlags A pointer to a variable that receives a set of bit flags that specify
     *                  properties of the object handle or 0.
     * @return If the function succeeds, the return value is nonzero.
     */
    @NativeType("BOOL")
    public static boolean GetHandleInformation(@NativeType("HANDLE") long hObject,
                                               @NativeType("LPDWORD") IntBuffer lpdwFlags) {
        long __functionAddress = Functions.GetHandleInformation;
        if (CHECKS) {
            checkSafe(lpdwFlags, 1);
        }
        return callPPI(hObject, memAddressSafe(lpdwFlags), __functionAddress) != 0;
    }

    /**
     * Retrieves the calling thread's last-error code value. The last-error code is
     * maintained on a per-thread basis. Multiple threads do not overwrite each other's
     * last-error code.
     *
     * @return The return value is the calling thread's last-error code.
     */
    @NativeType("DWORD")
    public static int GetLastError() {
        long __functionAddress = Functions.GetLastError;
        return callI(__functionAddress);
    }
}
