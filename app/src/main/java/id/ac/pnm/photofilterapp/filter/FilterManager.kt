package id.ac.pnm.photofilterapp.filter

import android.graphics.ColorMatrix
import id.ac.pnm.photofilterapp.R

data class FilterConfig(
    val id: String,
    val buttonColorRes: Int,
    val colorMatrix: ColorMatrix? 
)

object FilterManager {
    
    val filters = listOf(
        FilterConfig(
            id = "Normal",
            buttonColorRes = android.R.color.white,
            colorMatrix = null
        ),
        FilterConfig(
            id = "Autumn",
            buttonColorRes = R.color.corn,
            colorMatrix = ColorMatrix().apply {
                setScale(1.3f, 1.1f, 0.9f, 1f)
                val saturation = ColorMatrix()
                saturation.setSaturation(1.2f)
                postConcat(saturation)
            }
        ),
        FilterConfig(
            id = "SumBlueThingy",
            buttonColorRes = R.color.arlyde_yellow, 
            colorMatrix = ColorMatrix().apply {
                setScale(0.9f, 0.95f, 1.2f, 1f)
                val desat = ColorMatrix()
                desat.setSaturation(0.85f)
                postConcat(desat)
            }
        )


        // if y'all finna add new filters. make sure to follow the same pattern such as
        // ex:
        // FilterConfig(
        //     id = "B&W",
        //     buttonColorRes = R.color.black, // this part is for the shutter button 
        //     colorMatrix = ColorMatrix().apply { setSaturation(0f) } // you may ask AI or just read the damn doc https://developer.android.com/reference/android/graphics/ColorMatrixColorFilter
        // )
    )
}
