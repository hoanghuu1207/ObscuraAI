package com.techvertex.obscura.core.video.gl

import android.opengl.GLES20
import android.util.Log
import com.techvertex.obscura.core.video.model.VideoBlurType

class BlurShaderProgram {

    companion object {
        private const val TAG = "BlurShaderProgram"

        const val VERTEX_SHADER = """
            uniform mat4 u_MVPMatrix;
            uniform mat4 u_STMatrix;
            attribute vec4 a_Position;
            attribute vec4 a_TextureCoord;
            varying vec2 v_TexCoord;
            varying vec2 v_ScreenCoord;

            void main() {
                gl_Position = u_MVPMatrix * a_Position;
                v_TexCoord = (u_STMatrix * a_TextureCoord).xy;
                v_ScreenCoord = a_TextureCoord.xy;
            }
        """

        const val FRAGMENT_SHADER_PASSTHROUGH = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;

            uniform samplerExternalOES sTexture;
            uniform float u_BlurIntensity;
            uniform vec2 u_Resolution;
            uniform vec4 u_FrameRect;
            varying vec2 v_TexCoord;
            varying vec2 v_ScreenCoord;

            void main() {
                gl_FragColor = texture2D(sTexture, v_TexCoord);
            }
        """

        const val FRAGMENT_SHADER_GAUSSIAN = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;

            uniform samplerExternalOES sTexture;
            uniform float u_BlurIntensity;
            uniform vec2 u_Resolution;
            uniform vec4 u_FrameRect;
            varying vec2 v_TexCoord;
            varying vec2 v_ScreenCoord;

            bool insideRect(vec2 uv, vec4 rect) {
                return uv.x >= rect.x && uv.x <= rect.z &&
                       uv.y >= rect.y && uv.y <= rect.w;
            }

            void main() {
                if (!insideRect(v_ScreenCoord, u_FrameRect)) {
                    gl_FragColor = texture2D(sTexture, v_TexCoord);
                    return;
                }

                float radius = u_BlurIntensity * 100.0;
                vec2 texelSize = 1.0 / u_Resolution;

                float weights[7];
                weights[0] = 0.1964825501511404;
                weights[1] = 0.2969069646728344;
                weights[2] = 0.2195956971951564;
                weights[3] = 0.0448868288101688;
                weights[4] = 0.0045913556992408;
                weights[5] = 0.0002189576832480;
                weights[6] = 0.0000046640341760;

                vec4 color = texture2D(sTexture, v_TexCoord) * weights[0];

                for (int i = 1; i < 7; i++) {
                    float offset = float(i) * radius / 6.0;
                    vec2 offsetH = vec2(texelSize.x * offset, 0.0);
                    color += texture2D(sTexture, v_TexCoord + offsetH) * weights[i] * 0.5;
                    color += texture2D(sTexture, v_TexCoord - offsetH) * weights[i] * 0.5;
                }

                for (int i = 1; i < 7; i++) {
                    float offset = float(i) * radius / 6.0;
                    vec2 offsetV = vec2(0.0, texelSize.y * offset);
                    color += texture2D(sTexture, v_TexCoord + offsetV) * weights[i] * 0.5;
                    color += texture2D(sTexture, v_TexCoord - offsetV) * weights[i] * 0.5;
                }

                gl_FragColor = color;
            }
        """

        const val FRAGMENT_SHADER_LINE = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;

            uniform samplerExternalOES sTexture;
            uniform float u_BlurIntensity;
            uniform vec2 u_Resolution;
            uniform vec4 u_FrameRect;
            varying vec2 v_TexCoord;
            varying vec2 v_ScreenCoord;

            bool insideRect(vec2 uv, vec4 rect) {
                return uv.x >= rect.x && uv.x <= rect.z &&
                       uv.y >= rect.y && uv.y <= rect.w;
            }

            void main() {
                if (!insideRect(v_ScreenCoord, u_FrameRect)) {
                    gl_FragColor = texture2D(sTexture, v_TexCoord);
                    return;
                }

                float radius = u_BlurIntensity * 80.0;
                vec2 texelSize = 1.0 / u_Resolution;
                vec2 direction = normalize(vec2(1.0, 0.0)) * texelSize * radius;

                vec4 color = vec4(0.0);
                float totalWeight = 0.0;

                for (int i = -4; i <= 4; i++) {
                    float fi = float(i);
                    float weight = 1.0 - abs(fi) / 5.0;
                    vec2 offset = direction * fi / 4.0;
                    color += texture2D(sTexture, v_TexCoord + offset) * weight;
                    totalWeight += weight;
                }

                gl_FragColor = color / totalWeight;
            }
        """

        const val FRAGMENT_SHADER_ZOOM = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;

            uniform samplerExternalOES sTexture;
            uniform float u_BlurIntensity;
            uniform vec2 u_Resolution;
            uniform vec4 u_FrameRect;
            varying vec2 v_TexCoord;
            varying vec2 v_ScreenCoord;

            bool insideRect(vec2 uv, vec4 rect) {
                return uv.x >= rect.x && uv.x <= rect.z &&
                       uv.y >= rect.y && uv.y <= rect.w;
            }

            void main() {
                if (!insideRect(v_ScreenCoord, u_FrameRect)) {
                    gl_FragColor = texture2D(sTexture, v_TexCoord);
                    return;
                }

                vec2 center = vec2(
                    (u_FrameRect.x + u_FrameRect.z) * 0.5,
                    (u_FrameRect.y + u_FrameRect.w) * 0.5
                );

                float strength = u_BlurIntensity * 1.05;
                vec2 dir = v_ScreenCoord - center;
                float dist = length(dir);

                vec4 color = vec4(0.0);
                const int SAMPLES = 9;
                float totalWeight = 0.0;

                for (int i = 0; i < SAMPLES; i++) {
                    float t = float(i) / float(SAMPLES - 1);
                    float scale = 1.0 - strength * t * dist;
                    vec2 screenSamplePos = center + dir * scale;
                    vec2 texOffset = screenSamplePos - v_ScreenCoord;
                    vec2 sampleUV = v_TexCoord + texOffset;
                    float weight = 1.0 - t * 0.5;
                    color += texture2D(sTexture, sampleUV) * weight;
                    totalWeight += weight;
                }

                gl_FragColor = color / totalWeight;
            }
        """

        const val FRAGMENT_SHADER_PAINT = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;

            uniform samplerExternalOES sTexture;
            uniform float u_BlurIntensity;
            uniform vec2 u_Resolution;
            uniform vec4 u_FrameRect;
            varying vec2 v_TexCoord;
            varying vec2 v_ScreenCoord;

            bool insideRect(vec2 uv, vec4 rect) {
                return uv.x >= rect.x && uv.x <= rect.z &&
                       uv.y >= rect.y && uv.y <= rect.w;
            }

            void main() {
                if (!insideRect(v_ScreenCoord, u_FrameRect)) {
                    gl_FragColor = texture2D(sTexture, v_TexCoord);
                    return;
                }

                float radius = u_BlurIntensity * 2.5 + 2.0; 
                vec2 texelSize = 6.0 / u_Resolution;

                vec3 mean[4];
                vec3 variance[4];
                float count;
                vec3 s;

                count = 0.0;
                mean[0] = vec3(0.0);
                variance[0] = vec3(0.0);
                for (int x = -2; x <= 0; x++) {
                    for (int y = -2; y <= 0; y++) {
                        float fx = float(x);
                        float fy = float(y);
                        if (abs(fx) <= radius && abs(fy) <= radius) {
                            s = texture2D(sTexture, v_TexCoord + vec2(fx, fy) * texelSize).rgb;
                            mean[0] += s;
                            variance[0] += s * s;
                            count += 1.0;
                        }
                    }
                }
                mean[0] /= count;
                variance[0] = abs(variance[0] / count - mean[0] * mean[0]);

                count = 0.0;
                mean[1] = vec3(0.0);
                variance[1] = vec3(0.0);
                for (int x = 0; x <= 2; x++) {
                    for (int y = -2; y <= 0; y++) {
                        float fx = float(x);
                        float fy = float(y);
                        if (abs(fx) <= radius && abs(fy) <= radius) {
                            s = texture2D(sTexture, v_TexCoord + vec2(fx, fy) * texelSize).rgb;
                            mean[1] += s;
                            variance[1] += s * s;
                            count += 1.0;
                        }
                    }
                }
                mean[1] /= count;
                variance[1] = abs(variance[1] / count - mean[1] * mean[1]);

                count = 0.0;
                mean[2] = vec3(0.0);
                variance[2] = vec3(0.0);
                for (int x = -2; x <= 0; x++) {
                    for (int y = 0; y <= 2; y++) {
                        float fx = float(x);
                        float fy = float(y);
                        if (abs(fx) <= radius && abs(fy) <= radius) {
                            s = texture2D(sTexture, v_TexCoord + vec2(fx, fy) * texelSize).rgb;
                            mean[2] += s;
                            variance[2] += s * s;
                            count += 1.0;
                        }
                    }
                }
                mean[2] /= count;
                variance[2] = abs(variance[2] / count - mean[2] * mean[2]);

                count = 0.0;
                mean[3] = vec3(0.0);
                variance[3] = vec3(0.0);
                for (int x = 0; x <= 2; x++) {
                    for (int y = 0; y <= 2; y++) {
                        float fx = float(x);
                        float fy = float(y);
                        if (abs(fx) <= radius && abs(fy) <= radius) {
                            s = texture2D(sTexture, v_TexCoord + vec2(fx, fy) * texelSize).rgb;
                            mean[3] += s;
                            variance[3] += s * s;
                            count += 1.0;
                        }
                    }
                }
                mean[3] /= count;
                variance[3] = abs(variance[3] / count - mean[3] * mean[3]);

                float minVar = dot(variance[0], vec3(0.299, 0.587, 0.114));
                vec3 result = mean[0];
                for (int q = 1; q < 4; q++) {
                    float v = dot(variance[q], vec3(0.299, 0.587, 0.114));
                    if (v < minVar) {
                        minVar = v;
                        result = mean[q];
                    }
                }

                gl_FragColor = vec4(result, 1.0);
            }
        """

        const val FRAGMENT_SHADER_POLAR = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;

            uniform samplerExternalOES sTexture;
            uniform float u_BlurIntensity;
            uniform vec2 u_Resolution;
            uniform vec4 u_FrameRect;
            varying vec2 v_TexCoord;
            varying vec2 v_ScreenCoord;

            #define PI 3.14159265359

            bool insideRect(vec2 uv, vec4 rect) {
                return uv.x >= rect.x && uv.x <= rect.z &&
                       uv.y >= rect.y && uv.y <= rect.w;
            }

            void main() {
                if (!insideRect(v_ScreenCoord, u_FrameRect)) {
                    gl_FragColor = texture2D(sTexture, v_TexCoord);
                    return;
                }

                vec2 center = vec2(
                    (u_FrameRect.x + u_FrameRect.z) * 0.5,
                    (u_FrameRect.y + u_FrameRect.w) * 0.5
                );

                vec2 diff = v_ScreenCoord - center;
                float aspect = u_Resolution.x / u_Resolution.y;
                diff.x *= aspect;

                float dist = length(diff);
                float angle = atan(diff.y, diff.x);

                float segments = max(4.0, 64.0 - u_BlurIntensity * 30.0);
                float rings = max(2.0, 32.0 - u_BlurIntensity * 15.0);

                float maxRadius = length(vec2(
                    (u_FrameRect.z - u_FrameRect.x) * 0.5 * aspect,
                    (u_FrameRect.w - u_FrameRect.y) * 0.5
                ));

                float angleStep = 2.0 * PI / segments;
                float quantizedAngle = floor(angle / angleStep + 0.5) * angleStep;

                float radiusStep = maxRadius / rings;
                float quantizedDist = floor(dist / radiusStep + 0.5) * radiusStep;

                vec2 quantizedDiff = vec2(
                    cos(quantizedAngle) * quantizedDist / aspect,
                    sin(quantizedAngle) * quantizedDist
                );

                vec2 screenSamplePos = center + quantizedDiff;
                vec2 texOffset = screenSamplePos - v_ScreenCoord;
                vec2 sampleUV = v_TexCoord + texOffset;
                sampleUV = clamp(sampleUV, vec2(0.0), vec2(1.0));

                gl_FragColor = texture2D(sTexture, sampleUV);
            }
        """

        const val FRAGMENT_SHADER_MOTION = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;

            uniform samplerExternalOES sTexture;
            uniform float u_BlurIntensity;
            uniform vec2 u_Resolution;
            uniform vec4 u_FrameRect;
            varying vec2 v_TexCoord;
            varying vec2 v_ScreenCoord;

            bool insideRect(vec2 uv, vec4 rect) {
                return uv.x >= rect.x && uv.x <= rect.z &&
                       uv.y >= rect.y && uv.y <= rect.w;
            }

            void main() {
                if (!insideRect(v_ScreenCoord, u_FrameRect)) {
                    gl_FragColor = texture2D(sTexture, v_TexCoord);
                    return;
                }

                float radius = u_BlurIntensity * 100.0;
                vec2 texelSize = 1.0 / u_Resolution;

                float angle = 0.5236;
                vec2 direction = vec2(cos(angle), sin(angle)) * texelSize * radius;

                vec4 color = vec4(0.0);
                float totalWeight = 0.0;
                const int SAMPLES = 13;

                for (int i = -6; i <= 6; i++) {
                    float fi = float(i);
                    float t = fi / 6.0;
                    vec2 offset = direction * t;
                    float weight = 1.0 - abs(t);
                    color += texture2D(sTexture, v_TexCoord + offset) * weight;
                    totalWeight += weight;
                }

                gl_FragColor = color / totalWeight;
            }
        """

        fun compileShader(type: Int, source: String): Int {
            val shader = GLES20.glCreateShader(type)
            if (shader == 0) {
                Log.e(TAG, "Failed to create shader (type=$type)")
                return 0
            }

            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)

            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                val infoLog = GLES20.glGetShaderInfoLog(shader)
                Log.e(TAG, "Shader compilation failed (type=$type): $infoLog")
                GLES20.glDeleteShader(shader)
                return 0
            }

            return shader
        }

        fun createProgram(vertexSource: String, fragmentSource: String): Int {
            val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
            if (vertexShader == 0) return 0

            val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
            if (fragmentShader == 0) {
                GLES20.glDeleteShader(vertexShader)
                return 0
            }

            val program = GLES20.glCreateProgram()
            if (program == 0) {
                Log.e(TAG, "Failed to create program")
                GLES20.glDeleteShader(vertexShader)
                GLES20.glDeleteShader(fragmentShader)
                return 0
            }

            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)

            val linked = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0)
            if (linked[0] == 0) {
                val infoLog = GLES20.glGetProgramInfoLog(program)
                Log.e(TAG, "Program linking failed: $infoLog")
                GLES20.glDeleteProgram(program)
                GLES20.glDeleteShader(vertexShader)
                GLES20.glDeleteShader(fragmentShader)
                return 0
            }

            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)

            return program
        }

        fun getShaderForType(type: VideoBlurType): String {
            return when (type) {
                VideoBlurType.NONE -> FRAGMENT_SHADER_PASSTHROUGH
                VideoBlurType.GAUSSIAN_BLUR -> FRAGMENT_SHADER_GAUSSIAN
                VideoBlurType.LINE_BLUR -> FRAGMENT_SHADER_LINE
                VideoBlurType.ZOOM_BLUR -> FRAGMENT_SHADER_ZOOM
                VideoBlurType.PAINT_BLUR -> FRAGMENT_SHADER_PAINT
                VideoBlurType.POLAR_BLUR -> FRAGMENT_SHADER_POLAR
                VideoBlurType.MOTION_BLUR -> FRAGMENT_SHADER_MOTION
            }
        }

        fun createProgramForType(type: VideoBlurType): Int {
            val fragmentSource = getShaderForType(type)
            return createProgram(VERTEX_SHADER, fragmentSource)
        }
    }
}
