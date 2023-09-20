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

import icyllis.arc3d.core.SharedPtr;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import javax.annotation.Nullable;

import static icyllis.arc3d.engine.Engine.*;

/**
 * A factory for creating {@link TextureProxy}-derived objects. This class may be used on
 * the creating thread of {@link RecordingContext}.
 */
public final class ProxyProvider {

    private final RecordingContext mContext;
    private final DirectContext mDirect;

    // This holds the texture proxies that have unique keys. The resourceCache does not get a ref
    // on these proxies, but they must send a message to the resourceCache when they are deleted.
    private final Object2ObjectOpenHashMap<Object, TextureProxy> mUniquelyKeyedProxies;

    ProxyProvider(RecordingContext context) {
        mContext = context;
        if (context instanceof DirectContext) {
            mDirect = (DirectContext) context;
        } else {
            mDirect = null; // deferred
        }

        mUniquelyKeyedProxies = new Object2ObjectOpenHashMap<>();
    }

    /**
     * Assigns a unique key to a proxy. The proxy will be findable via this key using
     * {@link #findProxyByUniqueKey()}. It is an error if an existing proxy already has a key.
     */
    public boolean assignUniqueKeyToProxy(Object key, TextureProxy proxy) {
        assert key != null;
        if (mContext.isDiscarded() || proxy == null) {
            return false;
        }

        // Only the proxyProvider that created a proxy should be assigning unique keys to it.
        assert isDeferredProvider() == ((proxy.mSurfaceFlags & SurfaceFlags.DEFERRED_PROVIDER) != 0);

        // If there is already a Resource with this key then the caller has violated the
        // normal usage pattern of uniquely keyed resources (e.g., they have created one w/o
        // first seeing if it already existed in the cache).
        assert mDirect == null || mDirect.getResourceCache().findAndRefUniqueResource(key) == null;

        // multiple proxies can't get the same key
        assert !mUniquelyKeyedProxies.containsKey(key);
        //TODO set

        mUniquelyKeyedProxies.put(key, proxy);
        return true;
    }

    /**
     * Sets the unique key of the provided proxy to the unique key of the surface. The surface must
     * have a valid unique key.
     */
    public void adoptUniqueKeyFromSurface(TextureProxy proxy, Texture texture) {
        //TODO
    }

    public void processInvalidUniqueKey(Object key, TextureProxy proxy, boolean invalidateResource) {
    }

    /**
     * Create a {@link TextureProxy} without any data.
     *
     * @see TextureProxy
     * @see SurfaceFlags#Budgeted
     * @see SurfaceFlags#LooseFit
     * @see SurfaceFlags#Mipmapped
     * @see SurfaceFlags#Protected
     * @see SurfaceFlags#SKIP_ALLOCATOR
     */
    @Nullable
    @SharedPtr
    public TextureProxy createTextureProxy(BackendFormat format,
                                           int width, int height,
                                           int surfaceFlags) {
        assert mContext.isOwnerThread();
        if (mContext.isDiscarded()) {
            return null;
        }

        if (format.isCompressed()) {
            // Deferred proxies for compressed textures are not supported.
            return null;
        }

        if (!mContext.getCaps().validateSurfaceParams(width, height, format, 1, surfaceFlags)) {
            return null;
        }

        if (isDeferredProvider())
            surfaceFlags |= SurfaceFlags.DEFERRED_PROVIDER;
        else
            assert (surfaceFlags & SurfaceFlags.DEFERRED_PROVIDER) == 0;

        return new TextureProxy(format, width, height, surfaceFlags);
    }

    /**
     * @see SurfaceFlags#Budgeted
     * @see SurfaceFlags#LooseFit
     * @see SurfaceFlags#Mipmapped
     * @see SurfaceFlags#Protected
     * @see SurfaceFlags#SKIP_ALLOCATOR
     */
    @Nullable
    @SharedPtr
    public RenderTextureProxy createRenderTextureProxy(BackendFormat format,
                                                       int width, int height,
                                                       int sampleCount,
                                                       int surfaceFlags) {
        assert mContext.isOwnerThread();
        if (mContext.isDiscarded()) {
            return null;
        }

        if (format.isCompressed()) {
            // Deferred proxies for compressed textures are not supported.
            return null;
        }

        if (!mContext.getCaps().validateSurfaceParams(width, height, format, sampleCount, surfaceFlags)) {
            return null;
        }

        if (isDeferredProvider())
            surfaceFlags |= SurfaceFlags.DEFERRED_PROVIDER;
        else
            assert (surfaceFlags & SurfaceFlags.DEFERRED_PROVIDER) == 0;

        return new RenderTextureProxy(format, width, height, sampleCount, surfaceFlags);
    }

    /**
     * Create a RenderTargetProxy that wraps a backend texture and is both texturable and renderable.
     * <p>
     * The texture must be single sampled and will be used as the color attachment 0 of the non-MSAA
     * render target. If <code>sampleCount</code> is > 1, the underlying API uses separate MSAA render
     * buffers then a MSAA render buffer is created that resolves to the texture.
     */
    @Nullable
    @SharedPtr
    public RenderTextureProxy wrapRenderableBackendTexture(BackendTexture texture,
                                                           int sampleCount,
                                                           boolean ownership,
                                                           boolean cacheable,
                                                           Runnable releaseCallback) {
        if (mContext.isDiscarded()) {
            return null;
        }

        // This is only supported on a direct Context.
        if (mDirect == null) {
            return null;
        }

        sampleCount = mContext.getCaps().getRenderTargetSampleCount(sampleCount, texture.getBackendFormat());
        assert sampleCount > 0;
        //TODO
        return null;
    }

    public boolean isDeferredProvider() {
        return mDirect == null;
    }
}
