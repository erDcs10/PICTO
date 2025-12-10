import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.filter.GPUImageVignetteFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import android.graphics.PointF

class VintageFilter : GPUImageFilter(NO_FILTER_VERTEX_SHADER, VINTAGE_FRAGMENT_SHADER) {

    companion object {
        private const val VINTAGE_FRAGMENT_SHADER = """
            varying highp vec2 textureCoordinate;
            uniform sampler2D inputImageTexture;
            
            void main() {
                // 1. Get the original pixel color
                lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);
                
                // 2. Modify the colors (R, G, B, Alpha)
                // Boost Red slightly, Dim Blue slightly
                mediump float newR = textureColor.r * 1.15;
                mediump float newG = textureColor.g * 1.05;
                mediump float newB = textureColor.b * 0.8;
                
                // 3. Output the result
                gl_FragColor = vec4(newR, newG, newB, textureColor.a);
            }
        """
    }
}

class ColdFilter : GPUImageFilter(NO_FILTER_VERTEX_SHADER, COLD_FRAGMENT_SHADER) {
    companion object {
        private const val COLD_FRAGMENT_SHADER = """
            varying highp vec2 textureCoordinate;
            uniform sampler2D inputImageTexture;
            
            void main() {
                lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);
                
                // Reduce Red, Boost Blue
                mediump float newR = textureColor.r * 0.8;
                mediump float newG = textureColor.g * 1.0;
                mediump float newB = textureColor.b * 1.25;
                
                gl_FragColor = vec4(newR, newG, newB, textureColor.a);
            }
        """
    }
}

// 2. WARM FILTER (Boosts Red/Yellow, reduces Blue)
class WarmFilter : GPUImageFilter(NO_FILTER_VERTEX_SHADER, WARM_FRAGMENT_SHADER) {
    companion object {
        private const val WARM_FRAGMENT_SHADER = """
            varying highp vec2 textureCoordinate;
            uniform sampler2D inputImageTexture;
            
            void main() {
                lowp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);
                
                // Boost Red & Green (Yellow), Reduce Blue
                mediump float newR = textureColor.r * 1.15;
                mediump float newG = textureColor.g * 1.10;
                mediump float newB = textureColor.b * 0.9;
                
                gl_FragColor = vec4(newR, newG, newB, textureColor.a);
            }
        """
    }
}

// 3. RETRO SEPIA (Calculates Sepia tones)
class RetroSepiaFilter : GPUImageFilter(NO_FILTER_VERTEX_SHADER, SEPIA_FRAGMENT_SHADER) {
    companion object {
        private const val SEPIA_FRAGMENT_SHADER = """
            varying highp vec2 textureCoordinate;
            uniform sampler2D inputImageTexture;
            
            void main() {
                lowp vec4 color = texture2D(inputImageTexture, textureCoordinate);
                
                // Standard Sepia Formula
                highp float r = (color.r * 0.393) + (color.g * 0.769) + (color.b * 0.189);
                highp float g = (color.r * 0.349) + (color.g * 0.686) + (color.b * 0.168);
                highp float b = (color.r * 0.272) + (color.g * 0.534) + (color.b * 0.131);
                
                gl_FragColor = vec4(r, g, b, color.a);
            }
        """
    }
}

// 4. DRAMATIC VIGNETTE (Combines Dark Corners with Cold)
fun createDramaticFilter(): GPUImageFilterGroup {
    val group = GPUImageFilterGroup()
    group.addFilter(ColdFilter())

    val vignette = GPUImageVignetteFilter()
    vignette.setVignetteCenter(PointF(0.5f, 0.5f))
    vignette.setVignetteStart(0.3f) // Inner radius
    vignette.setVignetteEnd(0.75f)  // Outer radius

    group.addFilter(vignette)
    return group
}