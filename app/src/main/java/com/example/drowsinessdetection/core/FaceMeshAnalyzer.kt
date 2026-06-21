package com.example.drowsinessdetection.core

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.TransformExperimental
import androidx.camera.view.transform.ImageProxyTransformFactory
import androidx.camera.view.transform.OutputTransform
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import kotlin.math.sqrt

@TransformExperimental
class FaceMeshAnalyzer(
    private val useClahe: Boolean = false,
    private val shouldCreateDisplayBitmap: () -> Boolean = { false },
    private val onResult: (
        ear: Float,
        mar: Float,
        faceDetected: Boolean,
        fps: Float,
        latencyMs: Long,
        allPoints: List<FaceMeshPoint>?,
        imageWidth: Int,
        imageHeight: Int,
        displayBitmap: Bitmap?,
        imageTransform: OutputTransform?
    ) -> Unit
) : ImageAnalysis.Analyzer {

    private val detector = FaceMeshDetection.getClient(
        FaceMeshDetectorOptions.Builder()
            .setUseCase(FaceMeshDetectorOptions.FACE_MESH)
            .build()
    )

    private val leftEye = intArrayOf(33, 160, 158, 133, 153, 144)
    private val rightEye = intArrayOf(362, 385, 387, 263, 373, 380)
    private val imageTransformFactory = ImageProxyTransformFactory().apply {
        setUsingRotationDegrees(true)
        setUsingCropRect(true)
    }
    private var lastFrameTime = 0L

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val startTime = System.currentTimeMillis()
        val fps = if (lastFrameTime != 0L) 1000f / (startTime - lastFrameTime) else 0f
        lastFrameTime = startTime

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val imageTransform = imageTransformFactory.getOutputTransform(imageProxy)
        val renderLandmarks = shouldCreateDisplayBitmap()
        val imageWidth: Int
        val imageHeight: Int
        val displayBitmap: Bitmap?
        val inputImage: InputImage

        if (useClahe || renderLandmarks) {
            val cameraBitmap = imageProxy.toDisplayBitmap(rotation)
            if (cameraBitmap == null) {
                imageProxy.close()
                return
            }
            val analysisBitmap = if (useClahe) {
                ClaheProcessor.process(cameraBitmap)
            } else {
                cameraBitmap
            }
            displayBitmap = if (renderLandmarks) cameraBitmap else null
            imageWidth = analysisBitmap.width
            imageHeight = analysisBitmap.height
            inputImage = InputImage.fromBitmap(analysisBitmap, 0)
        } else {
            displayBitmap = null
            imageWidth = if (rotation == 90 || rotation == 270) imageProxy.height else imageProxy.width
            imageHeight = if (rotation == 90 || rotation == 270) imageProxy.width else imageProxy.height
            inputImage = InputImage.fromMediaImage(mediaImage, rotation)
        }

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                val latency = System.currentTimeMillis() - startTime
                if (faces.isNotEmpty()) {
                    val points = faces[0].allPoints
                    val ear = calculateEAR(points)
                    val mar = MarCalculator.calculate(points)
                    onResult(ear, mar, true, fps, latency, points, imageWidth, imageHeight, displayBitmap, imageTransform)
                } else {
                    onResult(0f, 0f, false, fps, latency, null, imageWidth, imageHeight, displayBitmap, imageTransform)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun ImageProxy.toDisplayBitmap(rotation: Int): Bitmap? {
        return try {
            val rawBitmap = toBitmap()
            val matrix = Matrix()
            if (rotation != 0) matrix.postRotate(rotation.toFloat())

            val (pivotX, pivotY) = if (rotation == 90 || rotation == 270) {
                rawBitmap.height / 2f to rawBitmap.width / 2f
            } else {
                rawBitmap.width / 2f to rawBitmap.height / 2f
            }
            matrix.postScale(-1f, 1f, pivotX, pivotY)

            if (matrix.isIdentity) {
                rawBitmap
            } else {
                Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun distance(a: FaceMeshPoint, b: FaceMeshPoint): Float {
        val dx = a.position.x - b.position.x
        val dy = a.position.y - b.position.y
        val dz = a.position.z - b.position.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun earFromPoints(pts: List<FaceMeshPoint>): Float {
        if (pts.size < 6) return 0f
        val v1 = distance(pts[1], pts[5])
        val v2 = distance(pts[2], pts[4])
        val h = distance(pts[0], pts[3])
        return if (h == 0f) 0f else (v1 + v2) / (2f * h)
    }

    private fun calculateEAR(allPoints: List<FaceMeshPoint>): Float {
        val leftPts = leftEye.map { allPoints[it] }
        val rightPts = rightEye.map { allPoints[it] }
        return (earFromPoints(leftPts) + earFromPoints(rightPts)) / 2f
    }
}
