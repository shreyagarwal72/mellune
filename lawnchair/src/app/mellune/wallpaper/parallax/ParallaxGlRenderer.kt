package app.mellune.wallpaper.parallax

import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

/** Camera state consumed by [ParallaxGlRenderer.draw]. All values are already smoothed. */
class ParallaxCamera {
    /** Camera offset in -1..1 (tilt plus home-page scroll). */
    var x = 0f
    var y = 0f

    /** 0 = rest. Positive pushes the camera into the scene; also drives the blur. */
    var dolly = 0f

    /** Dolly focal point in screen UV (0..1, y down). */
    var focalU = 0.5f
    var focalV = 0.5f

    /** Overall effect strength (user intensity). */
    var strength = 1f
}

/**
 * Minimal EGL + GLES2 renderer for the depth-parallax wallpaper. Every method must be called
 * from the single render thread that owns the EGL context.
 */
class ParallaxGlRenderer {
    private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var context: EGLContext = EGL14.EGL_NO_CONTEXT
    private var config: EGLConfig? = null
    private var surface: EGLSurface = EGL14.EGL_NO_SURFACE

    private var program = 0
    private var texPhoto = 0
    private var texDepth = 0
    private var imageAspect = 1f
    private var surfaceW = 1
    private var surfaceH = 1

    private val quad = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
        position(0)
    }

    private var locPos = 0
    private var uPhoto = 0
    private var uDepth = 0
    private var uUvScale = 0
    private var uCam = 0
    private var uDolly = 0
    private var uFocal = 0
    private var uBlur = 0
    private var uStrength = 0

    val hasSurface: Boolean get() = surface != EGL14.EGL_NO_SURFACE
    val hasImages: Boolean get() = texPhoto != 0 && texDepth != 0

    private fun initEgl(): Boolean {
        if (display != EGL14.EGL_NO_DISPLAY) return true
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (display == EGL14.EGL_NO_DISPLAY) return false
        val version = IntArray(2)
        if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
            display = EGL14.EGL_NO_DISPLAY
            return false
        }
        val attribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val num = IntArray(1)
        if (!EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, num, 0) || num[0] <= 0) return false
        config = configs[0]
        context = EGL14.eglCreateContext(
            display, config, EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0,
        )
        return context != EGL14.EGL_NO_CONTEXT
    }

    /** Attaches a window surface and prepares GL state. Returns false if EGL is unavailable. */
    fun createSurface(target: Surface): Boolean {
        if (!initEgl()) return false
        destroySurface()
        surface = EGL14.eglCreateWindowSurface(display, config, target, intArrayOf(EGL14.EGL_NONE), 0)
        if (surface == EGL14.EGL_NO_SURFACE) return false
        if (!EGL14.eglMakeCurrent(display, surface, surface, context)) return false
        if (program == 0) buildProgram()
        return program != 0
    }

    fun destroySurface() {
        if (display != EGL14.EGL_NO_DISPLAY && surface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(display, surface)
        }
        surface = EGL14.EGL_NO_SURFACE
    }

    fun setSize(w: Int, h: Int) {
        surfaceW = max(1, w)
        surfaceH = max(1, h)
    }

    /** Uploads the photo and depth map. Requires a current surface. */
    fun setImages(photo: Bitmap, depth: Bitmap) {
        if (!hasSurface) return
        if (texPhoto != 0) GLES20.glDeleteTextures(2, intArrayOf(texPhoto, texDepth), 0)
        val ids = IntArray(2)
        GLES20.glGenTextures(2, ids, 0)
        texPhoto = ids[0]
        texDepth = ids[1]
        upload(texPhoto, photo)
        upload(texDepth, depth)
        imageAspect = photo.width.toFloat() / photo.height.toFloat()
    }

    private fun upload(id: Int, bmp: Bitmap) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
    }

    fun draw(cam: ParallaxCamera) {
        if (!hasSurface || !hasImages || program == 0) return
        if (!EGL14.eglMakeCurrent(display, surface, surface, context)) return
        GLES20.glViewport(0, 0, surfaceW, surfaceH)
        GLES20.glUseProgram(program)

        // "Cover" fit, with a small overscan so camera motion never reveals the image edge.
        val screenAspect = surfaceW.toFloat() / surfaceH.toFloat()
        var sx = 1f
        var sy = 1f
        if (imageAspect > screenAspect) sx = screenAspect / imageAspect else sy = imageAspect / screenAspect
        sx *= OVERSCAN
        sy *= OVERSCAN

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texPhoto)
        GLES20.glUniform1i(uPhoto, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texDepth)
        GLES20.glUniform1i(uDepth, 1)

        GLES20.glUniform2f(uUvScale, sx, sy)
        GLES20.glUniform2f(uCam, cam.x, cam.y)
        GLES20.glUniform1f(uDolly, cam.dolly * cam.strength)
        GLES20.glUniform2f(uFocal, cam.focalU, cam.focalV)
        GLES20.glUniform1f(uBlur, cam.dolly.coerceAtLeast(0f) * BLUR_PER_DOLLY)
        GLES20.glUniform1f(uStrength, PARALLAX_STRENGTH * cam.strength)

        GLES20.glEnableVertexAttribArray(locPos)
        GLES20.glVertexAttribPointer(locPos, 2, GLES20.GL_FLOAT, false, 0, quad)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(locPos)
        EGL14.eglSwapBuffers(display, surface)
    }

    fun release() {
        if (display == EGL14.EGL_NO_DISPLAY) return
        if (surface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(display, surface, surface, context)
            if (texPhoto != 0) GLES20.glDeleteTextures(2, intArrayOf(texPhoto, texDepth), 0)
            if (program != 0) GLES20.glDeleteProgram(program)
        }
        texPhoto = 0
        texDepth = 0
        program = 0
        destroySurface()
        EGL14.eglDestroyContext(display, context)
        EGL14.eglTerminate(display)
        display = EGL14.EGL_NO_DISPLAY
        context = EGL14.EGL_NO_CONTEXT
    }

    private fun buildProgram() {
        val vs = compile(GLES20.GL_VERTEX_SHADER, VERTEX)
        val fs = compile(GLES20.GL_FRAGMENT_SHADER, FRAGMENT)
        if (vs == 0 || fs == 0) return
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, vs)
        GLES20.glAttachShader(p, fs)
        GLES20.glLinkProgram(p)
        val status = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            GLES20.glDeleteProgram(p)
            return
        }
        program = p
        locPos = GLES20.glGetAttribLocation(p, "aPos")
        uPhoto = GLES20.glGetUniformLocation(p, "uPhoto")
        uDepth = GLES20.glGetUniformLocation(p, "uDepth")
        uUvScale = GLES20.glGetUniformLocation(p, "uUvScale")
        uCam = GLES20.glGetUniformLocation(p, "uCam")
        uDolly = GLES20.glGetUniformLocation(p, "uDolly")
        uFocal = GLES20.glGetUniformLocation(p, "uFocal")
        uBlur = GLES20.glGetUniformLocation(p, "uBlur")
        uStrength = GLES20.glGetUniformLocation(p, "uStrength")
    }

    private fun compile(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src)
        GLES20.glCompileShader(s)
        val status = IntArray(1)
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            GLES20.glDeleteShader(s)
            return 0
        }
        return s
    }

    private companion object {
        const val OVERSCAN = 0.86f
        const val PARALLAX_STRENGTH = 0.030f
        const val BLUR_PER_DOLLY = 0.006f

        const val VERTEX = """
            attribute vec2 aPos;
            varying vec2 vUv;
            void main() {
                vUv = vec2(aPos.x * 0.5 + 0.5, 0.5 - aPos.y * 0.5);
                gl_Position = vec4(aPos, 0.0, 1.0);
            }
        """

        // Fake-3D: the depth map decides how far each pixel shifts with the camera and how fast
        // it grows during a dolly (near layers move more than far ones), refined over two taps.
        const val FRAGMENT = """
            precision mediump float;
            varying vec2 vUv;
            uniform sampler2D uPhoto;
            uniform sampler2D uDepth;
            uniform vec2 uUvScale;
            uniform vec2 uCam;
            uniform float uDolly;
            uniform vec2 uFocal;
            uniform float uBlur;
            uniform float uStrength;

            vec2 toImg(vec2 s) { return 0.5 + (s - 0.5) * uUvScale; }

            void main() {
                vec2 p = vUv;
                for (int i = 0; i < 2; i++) {
                    float d = texture2D(uDepth, clamp(toImg(p), 0.0, 1.0)).r;
                    float sc = 1.0 + uDolly * (0.06 + 0.22 * d);
                    vec2 q = uFocal + (vUv - uFocal) / sc;
                    q += uCam * (d - 0.5) * uStrength;
                    p = q;
                }
                vec2 uv = clamp(toImg(p), 0.0, 1.0);
                vec4 c = texture2D(uPhoto, uv);
                if (uBlur > 0.0001) {
                    c *= 0.36;
                    c += texture2D(uPhoto, clamp(uv + vec2(uBlur, 0.0), 0.0, 1.0)) * 0.16;
                    c += texture2D(uPhoto, clamp(uv - vec2(uBlur, 0.0), 0.0, 1.0)) * 0.16;
                    c += texture2D(uPhoto, clamp(uv + vec2(0.0, uBlur), 0.0, 1.0)) * 0.16;
                    c += texture2D(uPhoto, clamp(uv - vec2(0.0, uBlur), 0.0, 1.0)) * 0.16;
                }
                gl_FragColor = vec4(c.rgb, 1.0);
            }
        """
    }
}
