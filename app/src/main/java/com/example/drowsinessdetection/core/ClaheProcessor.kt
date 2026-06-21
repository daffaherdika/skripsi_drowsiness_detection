package com.example.drowsinessdetection.core

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * CLAHE = Contrast Limited Adaptive Histogram Equalization.
 *
 * Parameter:
 * - clipLimit = 2.0
 * - tileGridSize = 8x8
 */
object ClaheProcessor {

    private val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))

    fun process(input: Bitmap): Bitmap {
        val rgbaMat = Mat()
        val ycrcbMat = Mat()
        val yChannel = Mat()
        val enhancedY = Mat()

        try {
            Utils.bitmapToMat(input, rgbaMat)

            Imgproc.cvtColor(rgbaMat, ycrcbMat, Imgproc.COLOR_RGBA2RGB)
            Imgproc.cvtColor(ycrcbMat, ycrcbMat, Imgproc.COLOR_RGB2YCrCb)

            Core.extractChannel(ycrcbMat, yChannel, 0)
            clahe.apply(yChannel, enhancedY)
            Core.insertChannel(enhancedY, ycrcbMat, 0)

            Imgproc.cvtColor(ycrcbMat, rgbaMat, Imgproc.COLOR_YCrCb2RGB)
            Imgproc.cvtColor(rgbaMat, rgbaMat, Imgproc.COLOR_RGB2RGBA)

            return Bitmap.createBitmap(
                input.width,
                input.height,
                Bitmap.Config.ARGB_8888
            ).also { output ->
                Utils.matToBitmap(rgbaMat, output)
            }
        } finally {
            rgbaMat.release()
            ycrcbMat.release()
            yChannel.release()
            enhancedY.release()
        }
    }

    fun processImageProxy(imageProxy: ImageProxy): Bitmap? {
        return try {
            val bitmap = imageProxy.toBitmap()
            process(bitmap)
        } catch (e: Exception) {
            null
        }
    }
}
