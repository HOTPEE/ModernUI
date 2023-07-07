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

package icyllis.modernui.graphics.text;

import icyllis.modernui.graphics.MathUtil;
import icyllis.modernui.graphics.font.FontCollection.Run;
import icyllis.modernui.graphics.font.FontFamily;
import icyllis.modernui.graphics.font.FontMetricsInt;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.function.Function;

/**
 * The layout of a styled text run, including text shaping results, glyph metrics and
 * their positions.
 *
 * @see LayoutCache
 * @since 2.6
 */
public final class LayoutPiece {

    private static final Graphics2D[] sGraphics = new Graphics2D[4];

    static {
        var image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < 4; i++) {
            var graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    (i & FontPaint.RENDER_FLAG_ANTI_ALIAS) != 0
                            ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                            : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                    (i & FontPaint.RENDER_FLAG_LINEAR_METRICS) != 0
                            ? RenderingHints.VALUE_FRACTIONALMETRICS_ON
                            : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            sGraphics[i] = graphics;
        }
    }

    // all laid-out glyphs, the order is visually left-to-right
    private final int[] mGlyphs;

    // x0 y0 x1 y1... for positioning glyphs
    private final float[] mPositions;

    private final byte[] mFontIndices;
    private final FontFamily[] mFonts;

    // the size and order are relative to the text buf (char array)
    // only grapheme cluster bounds have advances, others are zeros
    // eg: [13.57, 0, 14.26, 0, 0]
    private final float[] mAdvances;

    // total font metrics
    private final int mAscent;
    private final int mDescent;

    // total advance
    private final float mAdvance;

    /**
     * Creates the glyph layout of a piece. No reference to the buffer will be held.
     *
     * @param buf   text buffer, cannot be null or empty
     * @param start start char offset
     * @param end   end char index
     * @param isRtl whether to layout in right-to-left
     * @param paint the font paint affecting measurement
     */
    LayoutPiece(@Nonnull char[] buf, int contextStart, int contextEnd, int start, int end,
                boolean isRtl, FontPaint paint) {
        int count = end - start;
        mAdvances = new float[count];
        final FontMetricsInt extent = new FontMetricsInt();

        // preserve memory
        final var fontIndices = new ByteArrayList(count);
        final var glyphs = new IntArrayList(count);
        final var positions = new FloatArrayList(count);

        final var fonts = new ArrayList<FontFamily>();

        HashMap<FontFamily, Byte> fontMap = new HashMap<>();

        float size = paint.getFontSize();
        float advance = 0;

        final List<Run> items = paint.mFont.itemize(buf, start, end);
        int runIndex, runEnd, runDir;
        if (isRtl) {
            runIndex = items.size() - 1;
            runEnd = -1;
            runDir = -1;
        } else {
            runIndex = 0;
            runEnd = items.size();
            runDir = 1;
        }
        Function<FontFamily, Byte> idGen = family -> {
            fonts.add(family);
            return (byte) fontMap.size();
        };
        // this is visually left-to-right
        for (; runIndex != runEnd; runIndex += runDir) {
            Run run = items.get(runIndex);

            byte fontIdx = fontMap.computeIfAbsent(run.family(), idGen);

            var g = sGraphics[paint.getRenderFlags()];
            Font font = run.family().getClosestMatch(paint.getFontStyle()).deriveFont(size);
            extent.extendBy(g.getFontMetrics(font));
            int layoutFlags = isRtl ? Font.LAYOUT_RIGHT_TO_LEFT : Font.LAYOUT_LEFT_TO_RIGHT;
            if (run.start() == contextStart) {
                layoutFlags |= Font.LAYOUT_NO_START_CONTEXT;
            }
            if (run.end() == contextEnd) {
                layoutFlags |= Font.LAYOUT_NO_LIMIT_CONTEXT;
            }
            GlyphVector vector = font.layoutGlyphVector(g.getFontRenderContext(),
                    buf, run.start(), run.end(), layoutFlags);
            int nGlyphs = vector.getNumGlyphs();

            for (int i = 0; i < nGlyphs; i++) {
                int charIndex = vector.getGlyphCharIndex(i);
                mAdvances[charIndex + run.start() - start] = vector.getGlyphMetrics(i).getAdvanceX();
            }

            GraphemeBreak.forTextRun(buf, paint.mLocale, run.start(), run.end(),
                    (clusterStart, clusterEnd) -> {
                        int rStart = clusterStart - start;
                        int rEnd = clusterEnd - start;
                        // accumulate advance to cluster
                        for (int i = rStart + 1; i < rEnd; i++) {
                            mAdvances[rStart] += mAdvances[i];
                            mAdvances[i] = 0;
                        }
                    });

            // this is visually left-to-right
            for (int i = 0; i < nGlyphs; i++) {
                glyphs.add(vector.getGlyphCode(i));
                var point = vector.getGlyphPosition(i);
                positions.add((float) point.getX() + advance);
                positions.add((float) point.getY());
            }

            int oldFontIdxSize = fontIndices.size();
            fontIndices.size(oldFontIdxSize + nGlyphs);
            Arrays.fill(fontIndices.elements(), oldFontIdxSize, oldFontIdxSize + nGlyphs, fontIdx);

            advance += vector.getGlyphPosition(vector.getNumGlyphs()).getX();
        }
        mGlyphs = glyphs.toIntArray();
        mPositions = positions.toFloatArray();
        if (fonts.size() > 1) {
            mFontIndices = fontIndices.toByteArray();
        } else {
            mFontIndices = null;
        }
        mFonts = fonts.toArray(new FontFamily[0]);

        mAscent = extent.ascent;
        mDescent = extent.descent;
        mAdvance = advance;
    }

    /**
     * The array is about all laid-out glyphs for in order visually from left to right.
     *
     * @return glyphs
     */
    public int[] getGlyphs() {
        return mGlyphs;
    }

    /**
     * This array holds the repeat of x offset, y offset of glyph positions.
     * The length is twice as long as the glyph array.
     *
     * @return glyph positions
     */
    public float[] getPositions() {
        return mPositions;
    }

    /**
     * Returns which font should be used for the i-th glyph.
     *
     * @param i the index
     * @return the font family
     */
    public FontFamily getFont(int i) {
        if (mFontIndices != null) {
            return mFonts[mFontIndices[i]];
        }
        return mFonts[0];
    }

    /**
     * The array of all chars advance, the length and order are relative to the text buffer.
     * Only grapheme cluster bounds have advances, others are zeros. For example: [13.57, 0, 14.26, 0, 0].
     * The length is constructor <code>end - start</code>.
     *
     * @return advances
     */
    public float[] getAdvances() {
        return mAdvances;
    }

    /**
     * Expands the font metrics of the maximum extent of this piece.
     *
     * @param extent to expand from
     */
    public void getExtent(@Nonnull FontMetricsInt extent) {
        extent.extendBy(getAscent(), getDescent());
    }

    /**
     * Gets the font metrics of the maximum extent of this piece.
     *
     * @return ascent to baseline, always positive
     */
    public int getAscent() {
        return mAscent;
    }

    /**
     * Gets the font metrics of the maximum extent of this piece.
     *
     * @return descent to baseline, always positive
     */
    public int getDescent() {
        return mDescent;
    }

    /**
     * Returns the total advance of this piece.
     *
     * @return advance
     */
    public float getAdvance() {
        return mAdvance;
    }

    public int getMemoryUsage() {
        int m = 48;
        m += 16 + MathUtil.align8(mGlyphs.length << 2);
        m += 16 + MathUtil.align8(mPositions.length << 2);
        if (mFontIndices != null) {
            m += 16 + MathUtil.align8(mFontIndices.length);
        }
        m += 16 + MathUtil.align8(mFonts.length << 2);
        m += 16 + MathUtil.align8(mAdvances.length << 2);
        return m;
    }

    @Override
    public String toString() {
        return "LayoutPiece{" +
                "mGlyphs=" + Arrays.toString(mGlyphs) +
                ", mPositions=" + Arrays.toString(mPositions) +
                ", mFonts=" + Arrays.toString(mFonts) +
                ", mFontIndices=" + Arrays.toString(mFontIndices) +
                ", mAdvances=" + Arrays.toString(mAdvances) +
                ", mAscent=" + mAscent +
                ", mDescent=" + mDescent +
                ", mAdvance=" + mAdvance +
                '}';
    }
}
