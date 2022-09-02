/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.test;

import icyllis.arcticgi.core.MathUtil;
import icyllis.arcticgi.core.*;
import icyllis.arcticgi.engine.*;
import icyllis.arcticgi.engine.geom.RoundRectProcessor;
import icyllis.arcticgi.opengl.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.EXTTextureCompressionS3TC;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntConsumer;

import static org.lwjgl.system.MemoryUtil.memAddress;

public class TestManagedResource {

    public static void main(String[] args) {
        long time = System.nanoTime();
        PrintWriter pw = new PrintWriter(System.out, true, StandardCharsets.UTF_8);

        GLFW.glfwInit();
        // load first
        Objects.requireNonNull(GL.getFunctionProvider());
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        long window = GLFW.glfwCreateWindow(1600, 900, "Test Window", 0, 0);
        if (window == 0) {
            throw new RuntimeException();
        }
        GLFW.glfwMakeContextCurrent(window);

        DirectContext directContext = DirectContext.makeOpenGL();
        if (directContext == null) {
            throw new RuntimeException();
        }
        String glVersion = GLCore.glGetString(GLCore.GL_VERSION);
        pw.println("OpenGL version: " + glVersion);
        pw.println("OpenGL vendor: " + GLCore.glGetString(GLCore.GL_VENDOR));
        pw.println("OpenGL renderer: " + GLCore.glGetString(GLCore.GL_RENDERER));
        pw.println("Max vertex attribs: " + GLCore.glGetInteger(GLCore.GL_MAX_VERTEX_ATTRIBS));
        pw.println("Max vertex bindings: " + GLCore.glGetInteger(GLCore.GL_MAX_VERTEX_ATTRIB_BINDINGS));
        pw.println("Max vertex stride: " + GLCore.glGetInteger(GLCore.GL_MAX_VERTEX_ATTRIB_STRIDE));

        pw.println("0f int bits: " + (-0.0f == 0.0f));

        if (directContext.caps().isFormatTexturable(
                BackendFormat.makeGL(EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT,
                        EngineTypes.TextureType_2D))) {
            pw.println("Compressed format: OK");
        }
        Swizzle.make("rgb1");
        SamplerState.make(SamplerState.FILTER_MODE_NEAREST, SamplerState.MIPMAP_MODE_NONE);

        TextureProxy proxy = directContext.getProxyProvider().createTextureProxy(
                BackendFormat.makeGL(GLCore.GL_RGBA8, EngineTypes.TextureType_2D),
                1600, 900, EngineTypes.Mipmapped_Yes, CoreTypes.BackingFit_Exact, true, 0, false);
        try (proxy) {
            pw.println(proxy);
        }

        Matrix4 transform = Matrix4.identity();
        transform.m34 = 1 / 4096f;
        transform.preRotateX(MathUtil.PI_O_3);
        Matrix3 matrix3 = new Matrix3();
        transform.toMatrix3(matrix3);
        pw.println(matrix3);

        GLServer server = (GLServer) directContext.getServer();
        GLPipelineStateCache pipelineStateCache = (GLPipelineStateCache) server.getPipelineBuilder();
        GLPipelineState pipelineState = pipelineStateCache.findOrCreatePipelineState(server,
                new ProgramInfo(new SurfaceProxyView(directContext.getProxyProvider().createRenderTargetProxy(
                        BackendFormat.makeGL(GLCore.GL_RGBA8, EngineTypes.TextureType_2D),
                        800, 800, 4, false, CoreTypes.BackingFit_Exact,
                        true, 0, true
                )), new RoundRectProcessor(true),
                        null, null, null, null,
                        EngineTypes.PrimitiveType_Triangles,
                        TransferProcessor.XferBarrierFlag_None,
                        EngineTypes.LoadOp_Discard,
                        ProgramInfo.InputFlag_None));

        pw.println(directContext.getServer().getPipelineBuilder().stats());

        testLexicon(pw);

        if (Platform.get() == Platform.WINDOWS) {
            if (!Kernel32.CloseHandle(959595595959595959L)) {
                pw.println("Failed to close handle");
            }
        }

        testCamera(pw);

        testRightHandedRotation(pw);

        testKeyBuilder(pw);

        testSimilarity(pw);

        directContext.close();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();

        try {
            assert false;
        } catch (AssertionError e) {
            System.out.println("Assertion works " + (System.nanoTime() - time) / 1000000);
        }
    }

    public static void testRightHandedRotation(PrintWriter pw) {
        Matrix4 mat = Matrix4.identity();
        mat.preRotateZ(MathUtil.PI_O_3);
        pw.println("preRotateX " + mat);

        Matrix4 mat2 = Matrix4.identity();
        mat2.preRotate(0, 0, 1, MathUtil.PI_O_3);
        pw.println("preRotateAxisAngle " + mat2);

        final double x = mat2.m11 * 2 + mat2.m21 * 2 + mat2.m31 * 2 + mat2.m41;
        final double y = mat2.m12 * 2 + mat2.m22 * 2 + mat2.m32 * 2 + mat2.m42;
        final double z = mat2.m13 * 2 + mat2.m23 * 2 + mat2.m33 * 2 + mat2.m43;
        pw.println("Point: " + x + ", " + y + ", " + z);
    }

    public static void testLexicon(PrintWriter pw) {
        pw.println("Matrix3 offset: " + Matrix3.OFFSET);
    }

    public static void testKeyBuilder(PrintWriter pw) {
        IntArrayList intArrayList = new IntArrayList();
        KeyBuilder keyBuilder = new KeyBuilder.StringKeyBuilder(intArrayList);
        keyBuilder.addBits(6, 0x2F, "");
        keyBuilder.add32(0xC1111111);
        keyBuilder.flush();
        pw.println(keyBuilder);
    }

    public static void testSimilarity(PrintWriter pw) {
        Matrix4 mat = Matrix4.identity();
        mat.preRotateZ(MathUtil.PI_O_2 * 29);
        Matrix3 m3 = new Matrix3();
        mat.toMatrix3(m3);
        pw.println(m3);
        pw.println(m3.getType());
        pw.println("Similarity: " + m3.isSimilarity());
    }

    public static void testCamera(PrintWriter pw) {
        Matrix4 mat = Matrix4.identity();
        mat.m34 = 1 / 576f;
        //mat.preTranslateZ(-20f);
        mat.preRotateY(MathUtil.PI_O_3);
        float[] p1 = new float[]{-25, -15};
        float[] p2 = new float[]{25, -15};
        float[] p3 = new float[]{25, 15};
        float[] p4 = new float[]{-25, 15};
        pw.println(mat);
        mat.mapPoint(p1);
        mat.mapPoint(p2);
        mat.mapPoint(p3);
        mat.mapPoint(p4);
        pw.println(Arrays.toString(p1));
        pw.println(Arrays.toString(p2));
        pw.println(Arrays.toString(p3));
        pw.println(Arrays.toString(p4));


        Camera.Camera3D camera3D = new Camera.Camera3D();
        Matrix4 transformMat = Matrix4.identity();
        transformMat.preRotateY(MathUtil.PI_O_3);
        Matrix3 outMatrix = new Matrix3();
        camera3D.getMatrix(transformMat, outMatrix, pw);
        pw.println("Orien: " + camera3D.mOrientation);
        pw.println(outMatrix);
        p1 = new float[]{-25, -15};
        p2 = new float[]{25, -15};
        p3 = new float[]{25, 15};
        p4 = new float[]{-25, 15};
        outMatrix.mapPoint(p1);
        outMatrix.mapPoint(p2);
        outMatrix.mapPoint(p3);
        outMatrix.mapPoint(p4);
        pw.println(Arrays.toString(p1));
        pw.println(Arrays.toString(p2));
        pw.println(Arrays.toString(p3));
        pw.println(Arrays.toString(p4));
    }

    public static void decodeLargeGIFUsingSTBImage(PrintWriter pw, String path) {
        ByteBuffer buffer = null;
        long image = 0;
        try (FileChannel channel = FileChannel.open(Path.of(path),
                StandardOpenOption.READ)) {
            //ByteBuffer mapper = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            buffer = MemoryUtil.memAlloc((int) channel.size());
            channel.read(buffer);
            buffer.rewind();
            pw.println("Raw size in bytes " + channel.size());
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer delays = stack.mallocPointer(1);
                IntBuffer x = stack.mallocInt(1);
                IntBuffer y = stack.mallocInt(1);
                IntBuffer z = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);
                new Thread(() -> {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    System.gc();
                }).start();
                image = STBImage.nstbi_load_gif_from_memory(memAddress(buffer), buffer.remaining(),
                        memAddress(delays), memAddress(x), memAddress(y), memAddress(z), memAddress(channels), 0);
                if (image == 0) {
                    throw new IOException(STBImage.stbi_failure_reason());
                }
                IntBuffer delay = delays.getIntBuffer(z.get(0));
                pw.printf("width:%d height:%s layers:%s channels:%s size_in_bytes:%s\n", x.get(0), y.get(0), z.get(0),
                        channels.get(0), (long) x.get(0) * y.get(0) * z.get(0) * channels.get(0));
                for (int i = 0; i < z.get(0); i++) {
                    pw.print(delay.get(i) + " ");
                }
                pw.println();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (image != 0) {
                STBImage.nstbi_image_free(image);
            }
            MemoryUtil.memFree(buffer);
        }
    }
}
