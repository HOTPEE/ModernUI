/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import icyllis.arc3d.core.*;
import icyllis.modernui.annotation.ColorInt;
import icyllis.modernui.annotation.Size;
import icyllis.modernui.annotation.*;
import icyllis.modernui.core.Core;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.*;
import java.lang.ref.Cleaner;
import java.nio.ByteOrder;
import java.nio.channels.*;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Date;
import java.util.function.LongConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static icyllis.modernui.ModernUI.LOGGER;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Describes a 2D raster image (pixel map), with its pixels in native memory.
 * This is used for CPU side operations, such as decoding or encoding. It is
 * generally uploaded to GPU side {@link Image} for drawing on the screen,
 * or downloaded from GPU side {@link Image} for encoding to streams.
 * <p>
 * This class is not thread safe, but memory safe. It's always recommended
 * to call {@link #close()} explicitly, or within a try-with-resource block.
 *
 * @see BitmapFactory
 */
@SuppressWarnings("unused")
public final class Bitmap implements AutoCloseable {

    public static final Marker MARKER = MarkerManager.getMarker("Bitmap");
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");

    @NonNull
    private final Format mFormat;
    @NonNull
    private final ImageInfo mInfo;

    private volatile SafePixels mPixels;

    Bitmap(@NonNull Format format, @NonNull ImageInfo info, long addr, int rowStride,
           @NonNull LongConsumer freeFn) {
        mFormat = format;
        mInfo = info;
        mPixels = new SafePixels(this, info, addr, rowStride, freeFn);
    }

    /**
     * Creates a mutable bitmap and its allocation, the content are initialized to zeros.
     *
     * @param width  width in pixels, ranged from 1 to 32768
     * @param height height in pixels, ranged from 1 to 32768
     * @param format the number of channels and the bit depth
     * @throws IllegalArgumentException width or height out of range, or allocation size >= 2GB
     * @throws OutOfMemoryError         out of off-heap memory
     */
    @NonNull
    public static Bitmap createBitmap(@Size(min = 1, max = 32768) int width,
                                      @Size(min = 1, max = 32768) int height,
                                      @NonNull Format format) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Image dimensions " + width + "x" + height
                    + " must be positive");
        }
        if (width > 32768 || height > 32768) {
            throw new IllegalArgumentException("Image dimensions " + width + "x" + height
                    + " must be less than or equal to 32768");
        }
        int rowStride = width * format.getBytesPerPixel(); // no overflow
        long size = (long) rowStride * height;
        if (size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Image allocation size " + size
                    + " must be less than or equal to 2GB");
        }
        long address = nmemCalloc(size, 1);
        if (address == NULL) {
            // execute ref.Cleaner
            System.gc();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            address = nmemCalloc(size, 1);
            if (address == NULL) {
                throw new OutOfMemoryError("Failed to allocate " + size + " bytes");
            }
        }
        ColorSpace cs = format.isChannelHDR()
                ? ColorSpace.get(ColorSpace.Named.LINEAR_EXTENDED_SRGB)
                : ColorSpace.get(ColorSpace.Named.SRGB);
        int at = format.hasAlpha() && !format.isChannelHDR()
                ? ImageInfo.AT_UNPREMUL
                : ImageInfo.AT_OPAQUE;
        return new Bitmap(format,
                ImageInfo.make(width, height, format.getColorType(), at, cs),
                address, rowStride, MemoryUtil::nmemFree);
    }

    /**
     * Display an OS file open dialog to select a supported image file.
     * The dialog will block the current thread until method return.
     *
     * @param format             the specified image file format to open,
     *                           or {@code null} to use all supported formats
     * @param title              the dialog title or {@code null} to use OS default
     * @param defaultPathAndFile the default path and/or file or {@code null} to use OS default
     * @return the selected path or {@code null} if selects nothing (dismissed or closed)
     */
    @Nullable
    public static String openDialogGet(@Nullable SaveFormat format,
                                       @Nullable CharSequence title,
                                       @Nullable CharSequence defaultPathAndFile) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = format != null
                    ? format.getFilters(stack)
                    : SaveFormat.getAllFilters(stack);
            return TinyFileDialogs.tinyfd_openFileDialog(title, defaultPathAndFile,
                    filters, format != null
                            ? format.getDescription()
                            : SaveFormat.getAllDescription(), false);
        }
    }

    /**
     * Display an OS file open dialog to select multiple supported image files.
     * The dialog will block the current thread until method return.
     *
     * @param format             the specified image file format to open,
     *                           or {@code null} to use all supported formats
     * @param title              the dialog title or {@code null} to use OS default
     * @param defaultPathAndFile the default path and/or file or {@code null} to use OS default
     * @return the selected paths or {@code null} if selects nothing (dismissed or closed)
     */
    @Nullable
    public static String[] openDialogGets(@Nullable SaveFormat format,
                                          @Nullable CharSequence title,
                                          @Nullable CharSequence defaultPathAndFile) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = format != null
                    ? format.getFilters(stack)
                    : SaveFormat.getAllFilters(stack);
            String s = TinyFileDialogs.tinyfd_openFileDialog(title, defaultPathAndFile,
                    filters, format != null
                            ? format.getDescription()
                            : SaveFormat.getAllDescription(), true);
            return s != null ? s.split("\\|") : null;
        }
    }

    /**
     * Display an OS file save dialog to select the path to save the bitmap.
     * The dialog will block the current thread until method return.
     *
     * @param format the specified image file format to filter,
     *               or {@code null} to filter all supported formats
     * @param title  the dialog title or {@code null} to use OS default
     * @param name   the file name without extension name
     * @return the path or {@code null} if selects nothing
     */
    @Nullable
    public static String saveDialogGet(@Nullable SaveFormat format,
                                       @Nullable CharSequence title,
                                       @Nullable String name) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = format != null
                    ? format.getFilters(stack)
                    : SaveFormat.getAllFilters(stack);
            return TinyFileDialogs.tinyfd_saveFileDialog(title,
                    SaveFormat.getFileName(format, name), filters, format != null
                            ? format.getDescription()
                            : SaveFormat.getAllDescription());
        }
    }

    @ApiStatus.Internal
    public static void flipVertically(@NonNull Bitmap bitmap) {
        final int height = bitmap.getHeight();
        final int rowStride = bitmap.getRowBytes();
        final long temp = nmemAllocChecked(rowStride);
        final long addr = bitmap.getAddress();
        for (int i = 0, lim = height >> 1; i < lim; i++) {
            final int srcOff = i * rowStride;
            final int dstOff = (height - i - 1) * rowStride;
            memCopy(addr + srcOff, temp, rowStride);
            memCopy(addr + dstOff, addr + srcOff, rowStride);
            memCopy(temp, addr + dstOff, rowStride);
        }
        nmemFree(temp);
    }

    @NonNull
    public Format getFormat() {
        return mFormat;
    }

    @NonNull
    public ImageInfo getInfo() {
        return mInfo;
    }

    /**
     * Returns the number of channels.
     */
    public int getChannels() {
        return mFormat.getChannels();
    }

    /**
     * Returns the width of the bitmap.
     */
    public int getWidth() {
        if (mPixels == null) {
            LOGGER.warn(MARKER, "Called getWidth() on a recycle()'d bitmap! This is undefined behavior!");
        }
        return mInfo.width();
    }

    /**
     * Returns the height of the bitmap.
     */
    public int getHeight() {
        if (mPixels == null) {
            LOGGER.warn(MARKER, "Called getHeight() on a recycle()'d bitmap! This is undefined behavior!");
        }
        return mInfo.height();
    }

    @ApiStatus.Internal
    public int getSize() {
        return mFormat.getBytesPerPixel() * getWidth() * getHeight();
    }

    /**
     * The base address of {@code unsigned char *pixels} in native.
     * The address is valid until bitmap closed.
     *
     * @return the pointer of pixel data, or NULL if released
     */
    @ApiStatus.Internal
    public long getAddress() {
        if (mPixels != null) {
            return mPixels.getPixels();
        }
        return NULL;
    }

    /**
     * The distance, in bytes, between the start of one pixel row and the next,
     * including any unused space between them.
     *
     * @return the scanline size in bytes
     */
    public int getRowBytes() {
        // XXX: row stride is always (width * bpp) in Modern UI
        if (mPixels == null) {
            throw new IllegalStateException("Can't call getRowBytes() on a recycled bitmap");
        }
        return mPixels.getRowStride();
    }

    @ApiStatus.Internal
    public int getColorType() {
        return mInfo.colorType();
    }

    @ApiStatus.Internal
    public int getAlphaType() {
        return mInfo.alphaType();
    }

    @Nullable
    public ColorSpace getColorSpace() {
        return mInfo.colorSpace();
    }

    /**
     * Returns true if the bitmap's format supports per-pixel alpha, and
     * if the pixels may contain non-opaque alpha values. For some formats,
     * this is always false (e.g. {@link Format#RGB_888}), since they do
     * not support per-pixel alpha. However, for formats that do, the
     * bitmap may be flagged to be known that all of its pixels are opaque.
     * In this case hasAlpha() will also return false. If a format such as
     * {@link Format#RGBA_8888} is not so flagged, it will return true
     * by default.
     */
    public boolean hasAlpha() {
        assert mPixels != null;
        return !mInfo.isOpaque();
    }

    /**
     * Returns true if the bitmap is marked as immutable.
     */
    public boolean isImmutable() {
        if (mPixels != null) {
            return mPixels.isImmutable();
        }
        assert false;
        return false;
    }

    /**
     * Marks the Bitmap as immutable. Further modifications to this Bitmap are disallowed.
     * After this method is called, this Bitmap cannot be made mutable again.
     */
    public void setImmutable() {
        if (mPixels != null) {
            mPixels.setImmutable();
        }
    }

    /**
     * <p>Indicates whether pixels stored in this bitmaps are stored pre-multiplied.
     * When a pixel is pre-multiplied, the RGB components have been multiplied by
     * the alpha component. For instance, if the original color is a 50%
     * translucent red <code>(128, 255, 0, 0)</code>, the pre-multiplied form is
     * <code>(128, 128, 0, 0)</code>.</p>
     *
     * <p>This method only returns true if {@link #hasAlpha()} returns true.
     * A bitmap with no alpha channel can be used both as a pre-multiplied and
     * as a non pre-multiplied bitmap.</p>
     *
     * @return true if the underlying pixels have been pre-multiplied, false
     * otherwise
     */
    public boolean isPremultiplied() {
        assert mPixels != null;
        // XXX: always false in Modern UI, and will be premul in GPU fragment shaders
        return mInfo.alphaType() == ImageInfo.AT_PREMUL;
    }

    private void checkOutOfBounds(int x, int y) {
        if (x < 0) {
            throw new IllegalArgumentException("x " + x + " must be >= 0");
        }
        if (y < 0) {
            throw new IllegalArgumentException("y " + y + " must be >= 0");
        }
        if (x >= getWidth()) {
            throw new IllegalArgumentException("x " + x + " must be < bitmap.width() " + getWidth());
        }
        if (y >= getHeight()) {
            throw new IllegalArgumentException("y " + y + " must be < bitmap.height() " + getHeight());
        }
    }

    /**
     * Returns the {@link Color} at the specified location. Throws an exception
     * if x or y are out of bounds (negative or >= to the width or height
     * respectively). The returned color is a non-premultiplied ARGB value in
     * the {@link ColorSpace.Named#SRGB sRGB} color space.
     *
     * @param x The x coordinate (0...width-1) of the pixel to return
     * @param y The y coordinate (0...height-1) of the pixel to return
     * @return The argb {@link Color} at the specified coordinate
     * @throws IllegalArgumentException if x, y exceed the bitmap's bounds
     */
    @ColorInt
    public int getPixelARGB(int x, int y) {
        checkReleased();
        checkOutOfBounds(x, y);
        int n32 = MemoryUtil.memGetInt(mPixels.getPixels() +
                (long) y * getRowBytes() +
                (long) x * mFormat.getBytesPerPixel());
        int argb;
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
            argb = switch (mFormat) {
                case GRAY_8 -> {
                    int lum = n32 >>> 24;
                    yield 0xFF000000 | (lum << 16) | (lum << 8) | lum;
                }
                case GRAY_ALPHA_88 -> {
                    int lum = n32 >>> 24;
                    yield ((n32 & 0xFF0000) << 8) | (lum << 16) | (lum << 8) | lum;
                }
                case RGB_888 -> 0xFF000000 | (n32 >>> 8);
                case RGBA_8888 -> ((n32 & 0xFF) << 24) | (n32 >>> 8);
                default -> throw new UnsupportedOperationException();
            };
        } else {
            argb = switch (mFormat) {
                case GRAY_8 -> { // to RRR1
                    int lum = n32 & 0xFF;
                    yield 0xFF000000 | (lum << 16) | (lum << 8) | lum;
                }
                case GRAY_ALPHA_88 -> { // to RRRG
                    int lum = n32 & 0xFF;
                    yield (n32 << 16) | (lum << 8) | lum;
                }
                case RGB_888 -> // to BGR1
                        0xFF000000 | ((n32 & 0xFF) << 16) | (n32 & 0xFF00) | ((n32 >> 16) & 0xFF);
                case RGBA_8888 -> // to BGRA
                        (n32 & 0xFF00FF00) | ((n32 & 0xFF) << 16) | ((n32 >> 16) & 0xFF);
                default -> throw new UnsupportedOperationException();
            };
        }
        // linear to gamma
        if (getColorSpace() != null && !getColorSpace().isSrgb()) {
            float[] v = {Color.red(argb) / 255.0f, Color.green(argb) / 255.0f, Color.blue(argb) / 255.0f};
            ColorSpace.connect(getColorSpace()).transform(v);
            return Color.argb(Color.alpha(argb), v[0], v[1], v[2]);
        }
        return argb;
    }

    /**
     * The ref of current pixel data, which may be shared across instances.
     * Calling this method won't affect the ref cnt. Every bitmap object
     * ref the {@link Pixmap} on create, and unref on {@link #close()}.
     * <p>
     * This method is <b>UNSAFE</b>, use with caution!
     *
     * @return the ref of pixel data, or null if released
     */
    @ApiStatus.Internal
    public Pixmap getPixels() {
        return mPixels;
    }

    /**
     * Save this bitmap to specified path as specified format. This will
     * open a save dialog to select the path, block the current thread until
     * the encoding is done.
     *
     * @param format  the format of the saved image
     * @param quality the compress quality, 0-100, only work for JPEG format.
     * @param name    the file name without extension name
     * @return true if selected a path, otherwise canceled
     * @throws IOException selected a path, but saving is not successful
     */
    @WorkerThread
    public boolean saveDialog(@NonNull SaveFormat format, int quality,
                              @Nullable String name) throws IOException {
        String path = saveDialogGet(format, null, name);
        if (path != null) {
            saveToPath(format, quality, Path.of(path));
            return true;
        }
        return false;
    }

    /**
     * Save this bitmap to specified path with specified format.
     *
     * @param format  the format of the saved image
     * @param quality the compress quality, 0-100, only work for JPEG format.
     * @param file    the image file
     * @throws IOException saving is not successful
     */
    @WorkerThread
    public void saveToFile(@NonNull SaveFormat format, int quality,
                           @NonNull File file) throws IOException {
        checkReleased();
        try (final var stream = new FileOutputStream(file)) {
            saveToChannel(format, quality, stream.getChannel());
        } catch (IOException e) {
            throw new IOException("Failed to save image to path \"" +
                    file.getAbsolutePath() + "\"", e);
        }
    }

    /**
     * Save this bitmap to specified path with specified format.
     *
     * @param format  the format of the saved image
     * @param quality the compress quality, 0-100, only work for JPEG format.
     * @param path    the image path
     * @throws IOException saving is not successful
     */
    @WorkerThread
    public void saveToPath(@NonNull SaveFormat format, int quality,
                           @NonNull Path path) throws IOException {
        checkReleased();
        try (final var channel = FileChannel.open(path,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            saveToChannel(format, quality, channel);
        } catch (IOException e) {
            throw new IOException("Failed to save image to path \"" +
                    path.toAbsolutePath() + "\"", e);
        }
    }

    /**
     * Save this bitmap to specified stream with specified format. The stream
     * will NOT be closed by this method.
     *
     * @param format  the format of the saved image
     * @param quality the compress quality, 0-100, only work for JPEG format
     * @param stream  the stream to write image data
     * @throws IOException saving is not successful
     */
    @WorkerThread
    public void saveToStream(@NonNull SaveFormat format, int quality,
                             @NonNull OutputStream stream) throws IOException {
        saveToChannel(format, quality, Channels.newChannel(stream));
    }

    /**
     * Save this bitmap to specified channel with specified format. The channel
     * will NOT be closed by this method.
     *
     * @param format  the format of the saved image
     * @param quality the compress quality, 0-100, only work for JPEG format
     * @param channel the channel to write image data
     * @throws IOException saving is not successful
     */
    @WorkerThread
    public void saveToChannel(@NonNull SaveFormat format, int quality,
                              @NonNull WritableByteChannel channel) throws IOException {
        checkReleased();
        if (quality < 0 || quality > 100) {
            throw new IllegalArgumentException("Bad quality " + quality + ", must be 0..100");
        }
        if (Core.isOnMainThread() || Core.isOnRenderThread()) {
            LOGGER.warn(MARKER, "Called save() on core thread! This will hang the application!",
                    new Exception().fillInStackTrace());
        }
        assert getRowBytes() == getWidth() * getFormat().getBytesPerPixel();
        final var callback = new STBIWriteCallback() {
            private IOException exception;

            @Override
            public void invoke(long context, long data, int size) {
                try {
                    channel.write(STBIWriteCallback.getData(data, size));
                } catch (IOException e) {
                    exception = e;
                }
            }
        };
        try (callback) {
            final boolean success = format.write(callback, mInfo.width(), mInfo.height(),
                    mFormat, getAddress(), quality);
            if (success) {
                if (callback.exception != null) {
                    throw new IOException("Failed to save image", callback.exception);
                }
            } else {
                throw new IOException("Failed to encode image: " + STBImage.stbi_failure_reason());
            }
        }
    }

    private void checkReleased() {
        if (mPixels == null) {
            throw new IllegalStateException("Cannot operate released bitmap");
        }
    }

    /**
     * Clear the reference to the pixel data. The bitmap is marked as "dead",
     * and then it is an error to try to access its pixels.
     * <p>
     * The system can ensure that the native allocation of a Bitmap to be
     * released when the Bitmap object becomes phantom-reachable. However,
     * it tends to take a very long time to perform automatic cleanup.
     */
    @Override
    public void close() {
        if (mPixels != null) {
            // Cleaner is synchronized
            mPixels.mCleanup.clean();
            mPixels = null;
        }
    }

    /**
     * Same as {@link #close()}.
     */
    public void recycle() {
        close();
    }

    /**
     * Returns true if this bitmap has been closed. If so, then it is an error
     * to try to access its pixels.
     *
     * @return true if the bitmap has been closed
     */
    public boolean isClosed() {
        return mPixels == null;
    }

    /**
     * @return the same as {@link #isClosed()}
     */
    public boolean isRecycled() {
        return mPixels == null;
    }

    @NonNull
    @Override
    public String toString() {
        return "Bitmap{" +
                "mFormat=" + mFormat +
                ", mInfo=" + mInfo +
                ", mRef=" + mPixels +
                '}';
    }

    /**
     * Describes the number of channels and bytes per pixel in memory.
     */
    public enum Format {
        //@formatter:off
        /**
         * Grayscale, one channel, 8-bit per channel.
         */
        GRAY_8         (1, ImageInfo.CT_GRAY_8       ),
        /**
         * Grayscale, with alpha, two channels, 8-bit per channel.
         */
        GRAY_ALPHA_88  (2, ImageInfo.CT_GRAY_ALPHA_88),
        /**
         * RGB, three channels, 8-bit per channel.
         */
        RGB_888        (3, ImageInfo.CT_RGB_888      ),
        /**
         * RGB, with alpha, four channels, 8-bit per channel.
         */
        RGBA_8888      (4, ImageInfo.CT_RGBA_8888    ),
        // U16, SRGB
        @ApiStatus.Internal
        GRAY_16        (1, ImageInfo.CT_UNKNOWN      ),
        @ApiStatus.Internal
        GRAY_ALPHA_1616(2, ImageInfo.CT_UNKNOWN      ),
        @ApiStatus.Internal
        RGB_161616     (3, ImageInfo.CT_UNKNOWN      ),
        RGBA_16161616  (4, ImageInfo.CT_RGBA_16161616),
        // HDR, LINEAR_SRGB
        @ApiStatus.Internal
        GRAY_F32       (1, ImageInfo.CT_UNKNOWN      ),
        @ApiStatus.Internal
        GRAY_ALPHA_F32 (2, ImageInfo.CT_UNKNOWN      ),
        @ApiStatus.Internal
        RGB_F32        (3, ImageInfo.CT_UNKNOWN      ),
        RGBA_F32       (4, ImageInfo.CT_RGBA_F32     );
        //@formatter:on

        private static final Format[] FORMATS = values();

        private final int mChannels;
        private final int mColorType;
        private final int mBytesPerPixel;

        Format(int chs, int ct) {
            mChannels = chs;
            mColorType = ct;
            mBytesPerPixel = ImageInfo.bytesPerPixel(ct);
            assert (ordinal() & 3) == (chs - 1);
        }

        /**
         * Returns the number of channels.
         */
        public int getChannels() {
            return mChannels;
        }

        /**
         * The source (in CPU memory) color type of this format.
         * <p>
         * RGB is special, it's 3 bytes per pixel in CPU memory, but
         * 4 bytes per pixel in GPU memory (implicitly).
         *
         * @see #getBytesPerPixel()
         */
        @ImageInfo.ColorType
        public int getColorType() {
            return mColorType;
        }

        public int getBytesPerPixel() {
            return mBytesPerPixel;
        }

        /**
         * Is this format 8-bit per channel and encoded as unsigned byte?
         */
        public boolean isChannelU8() {
            return ordinal() < 4;
        }

        /**
         * Is this format 16-bit per channel and encoded as unsigned short?
         */
        public boolean isChannelU16() {
            return ordinal() >> 2 == 1;
        }

        /**
         * Is this format 32-bit per channel and encoded as float?
         */
        public boolean isChannelHDR() {
            return ordinal() >> 2 == 2;
        }

        /**
         * Does this format have alpha channel?
         */
        public boolean hasAlpha() {
            return (ordinal() & 1) == 1;
        }

        @NonNull
        public static Format get(int chs, boolean u16, boolean hdr) {
            if (chs < 1 || chs > 4) {
                throw new IllegalArgumentException();
            }
            if (u16 && hdr) {
                throw new IllegalArgumentException();
            }
            return FORMATS[(chs - 1) | (u16 ? 4 : 0) | (hdr ? 8 : 0)];
        }
    }

    /**
     * List of supported formats a bitmap can be saved as (encoding or compressing).
     */
    public enum SaveFormat {
        /**
         * Save as the PNG format. PNG is lossless and compressed, and {@code quality}
         * is ignored.
         * <p>
         * Only supports 8-bit per channel images ({@link Format#isChannelU8()} is true).
         */
        PNG("*.png") {
            @Override
            public boolean write(@NonNull STBIWriteCallbackI func, int width, int height, @NonNull Format format,
                                 long data, int quality) throws IOException {
                if (!format.isChannelU8()) {
                    throw new IOException("Only 8-bit per channel images can be saved as "
                            + this + ", found " + format);
                }
                return STBImageWrite.nstbi_write_png_to_func(func.address(),
                        NULL, width, height, format.getChannels(), data, 0) != 0;
            }
        },

        /**
         * Save as the TGA format. TGA is lossless and compressed (by default), and {@code quality}
         * is ignored.
         * <p>
         * Only supports 8-bit per channel images ({@link Format#isChannelU8()} is true).
         */
        TGA("*.tga", "*.icb", "*.vda", "*.vst") {
            @Override
            public boolean write(@NonNull STBIWriteCallbackI func, int width, int height, @NonNull Format format,
                                 long data, int quality) throws IOException {
                if (!format.isChannelU8()) {
                    throw new IOException("Only 8-bit per channel images can be saved as "
                            + this + ", found " + format);
                }
                return STBImageWrite.nstbi_write_tga_to_func(func.address(),
                        NULL, width, height, format.getChannels(), data) != 0;
            }
        },

        /**
         * Save as the BMP format. BMP is lossless but almost uncompressed, so it takes
         * up a lot of space, and {@code quality} is ignored as well.
         * <p>
         * Only supports 8-bit per channel images ({@link Format#isChannelU8()} is true).
         */
        BMP("*.bmp", "*.dib") {
            @Override
            public boolean write(@NonNull STBIWriteCallbackI func, int width, int height, @NonNull Format format,
                                 long data, int quality) throws IOException {
                if (!format.isChannelU8()) {
                    throw new IOException("Only 8-bit per channel images can be saved as "
                            + this + ", found " + format);
                }
                return STBImageWrite.nstbi_write_bmp_to_func(func.address(),
                        NULL, width, height, format.getChannels(), data) != 0;
            }
        },

        /**
         * Save as the JPEG baseline format. {@code quality} of {@code 1} means
         * compress for the smallest size. {@code 100} means compress for max
         * visual quality. The file extension can be {@code .jpg}.
         * <p>
         * Only supports 8-bit per channel images ({@link Format#isChannelU8()} is true).
         */
        JPEG("*.jpg", "*.jpeg", "*.jpe", "*.jif", "*.jfif", "*.jfi") {
            @Override
            public boolean write(@NonNull STBIWriteCallbackI func, int width, int height, @NonNull Format format,
                                 long data, int quality) throws IOException {
                if (!format.isChannelU8()) {
                    throw new IOException("Only 8-bit per channel images can be saved as "
                            + this + ", found " + format);
                }
                return STBImageWrite.nstbi_write_jpg_to_func(func.address(),
                        NULL, width, height, format.getChannels(), data, quality) != 0;
            }
        },

        /**
         * Save as the Radiance RGBE format. RGBE allows pixels to have the dynamic range
         * and precision of floating-point values, and {@code quality} is ignored.
         * <p>
         * Only supports 32-bit per channel images ({@link Format#isChannelHDR()} is true).
         */
        HDR("*.hdr") {
            @Override
            public boolean write(@NonNull STBIWriteCallbackI func, int width, int height, @NonNull Format format,
                                 long data, int quality) throws IOException {
                if (!format.isChannelHDR()) {
                    throw new IOException("Only 32-bit per channel images can be saved as "
                            + this + ", found " + format);
                }
                return STBImageWrite.nstbi_write_hdr_to_func(func.address(),
                        NULL, width, height, format.getChannels(), data) != 0;
            }
        },

        /**
         * Save as the raw binary data, this is simply a memory dump.
         */
        // this format must be the last enum
        RAW("*.bin") {
            @Override
            public boolean write(@NonNull STBIWriteCallbackI func, int width, int height, @NonNull Format format,
                                 long data, int quality) throws IOException {
                func.invoke(NULL, data, width * height * format.getBytesPerPixel());
                return true;
            }
        };

        private static final SaveFormat[] OPEN_FORMATS;

        static {
            SaveFormat[] values = values();
            // remove the last "raw" format
            OPEN_FORMATS = Arrays.copyOf(values, values.length - 1);
        }

        // read only formats
        private static final String[] EXTRA_FILTERS = {"*.psd", "*.gif", "*.pic", "*.pnm", "*.pgm", "*.ppm"};

        @NonNull
        private final String[] filters;

        SaveFormat(@NonNull String... filters) {
            this.filters = filters;
        }

        public abstract boolean write(@NonNull STBIWriteCallbackI func, int width, int height,
                                      @NonNull Format format, long data, int quality) throws IOException;

        @NonNull
        public static PointerBuffer getAllFilters(@NonNull MemoryStack stack) {
            int length = EXTRA_FILTERS.length;
            for (SaveFormat format : OPEN_FORMATS) {
                length += format.filters.length;
            }
            PointerBuffer buffer = stack.mallocPointer(length);
            for (SaveFormat format : OPEN_FORMATS) {
                for (String filter : format.filters) {
                    stack.nUTF8(filter, true);
                    buffer.put(stack.getPointerAddress());
                }
            }
            for (String filter : EXTRA_FILTERS) {
                stack.nUTF8(filter, true);
                buffer.put(stack.getPointerAddress());
            }
            return buffer.rewind();
        }

        /**
         * Reads: "Image Files (*.png;*.jpg;*.bmp)"
         */
        @NonNull
        public static String getAllDescription() {
            return getAllDescription("Image Files");
        }

        /**
         * Reads: "[header] (*.png;*.jpg;*.bmp)"
         */
        @NonNull
        public static String getAllDescription(@NonNull String header) {
            return header + " (" + Stream.concat(
                            Arrays.stream(OPEN_FORMATS).flatMap(f -> Arrays.stream(f.filters)),
                            Arrays.stream(EXTRA_FILTERS))
                    .sorted().collect(Collectors.joining(";")) + ")";
        }

        @NonNull
        public PointerBuffer getFilters(@NonNull MemoryStack stack) {
            PointerBuffer buffer = stack.mallocPointer(filters.length);
            for (String filter : filters) {
                stack.nUTF8(filter, true);
                buffer.put(stack.getPointerAddress());
            }
            return buffer.rewind();
        }

        @NonNull
        public String getDescription() {
            return name() + " (" + String.join(";", filters) + ")";
        }

        @NonNull
        public static String getFileName(@Nullable SaveFormat format,
                                         @Nullable String name) {
            String s = name != null ? name : "image-" + DATE_FORMAT.format(new Date());
            if (format != null) {
                return s + format.filters[0].substring(1);
            }
            return s;
        }
    }

    // this ensures unref being called when Bitmap become phantom-reachable
    // but never called to close
    private static final class SafePixels extends Pixmap implements Runnable {

        final Cleaner.Cleanable mCleanup;

        private SafePixels(@NonNull Bitmap owner, ImageInfo info,
                           long addr, int rowStride, @NonNull LongConsumer freeFn) {
            super(info, addr, rowStride, freeFn);
            mCleanup = Core.registerCleanup(owner, this);
        }

        @Override
        public void run() {
            unref();
        }
    }
}
