package com.example.drowsinessdetection.ui.detection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.camera.view.TransformExperimental
import androidx.camera.view.transform.CoordinateTransform
import com.google.mlkit.vision.facemesh.FaceMeshPoint

@TransformExperimental
class LandmarkOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paintLeftEye = Paint().apply {
        color = Color.CYAN; style = Paint.Style.FILL; isAntiAlias = true
    }
    private val paintRightEye = Paint().apply {
        color = Color.YELLOW; style = Paint.Style.FILL; isAntiAlias = true
    }
    private val paintMouth = Paint().apply {
        color = Color.GREEN; style = Paint.Style.FILL; isAntiAlias = true
    }

    // Indeks mata — sama persis dengan FaceMeshAnalyzer.kt
    private val leftEyeIdx  = intArrayOf(33, 160, 158, 133, 153, 144)
    private val rightEyeIdx = intArrayOf(362, 385, 387, 263, 373, 380)

    // Indeks mulut — sama persis dengan MarCalculator.kt
    // left=61, right=291, topLeft=82, topCenter=13, topRight=312
    // botLeft=87, botCenter=14, botRight=317
    private val mouthIdx = intArrayOf(78, 81, 13, 311, 308, 318, 14, 88)

    private var allPoints: List<FaceMeshPoint> = emptyList()
    private var imgWidth  = 1
    private var imgHeight = 1
    private var scaleF    = 1f
    private var offsetX   = 0f
    private var offsetY   = 0f
    private var coordinateTransform: CoordinateTransform? = null
    private var mirrorHorizontally = false

    fun updateLandmarks(
        points: List<FaceMeshPoint>,
        imageWidth: Int,
        imageHeight: Int,
        transform: CoordinateTransform?,
        mirror: Boolean = false
    ) {
        allPoints = points
        imgWidth  = imageWidth
        imgHeight = imageHeight
        coordinateTransform = transform
        mirrorHorizontally = mirror
        computeTransform()
        invalidate()
    }

    fun clear() {
        allPoints = emptyList()
        coordinateTransform = null
        mirrorHorizontally = false
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        computeTransform()
    }

    private fun computeTransform() {
        if (imgWidth == 0 || imgHeight == 0 || width == 0 || height == 0) return

        val viewW = width.toFloat()
        val viewH = height.toFloat()

        val imgW = imgWidth.toFloat()
        val imgH = imgHeight.toFloat()

        // fillCenter = scale terbesar
        val scale = maxOf(viewW / imgW, viewH / imgH)

        // ukuran setelah scaling
        val scaledW = imgW * scale
        val scaledH = imgH * scale

        // crop offset (ini kunci!)
        offsetX = (viewW - scaledW) / 2f
        offsetY = (viewH - scaledH) / 2f

        scaleF = scale
    }

    private fun toScreen(point: FaceMeshPoint): Pair<Float, Float> {
        coordinateTransform?.let { transform ->
            val points = floatArrayOf(point.position.x, point.position.y)
            transform.mapPoints(points)
            if (mirrorHorizontally) {
                points[0] = width - points[0]
            }
            return Pair(points[0], points[1])
        }

        // 🔥 TANPA ROTASI MANUAL
        val x = if (mirrorHorizontally) {
            imgWidth - point.position.x
        } else {
            point.position.x
        }
        val y = point.position.y

        val screenX = x * scaleF + offsetX
        val screenY = y * scaleF + offsetY

        return Pair(screenX, screenY)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (allPoints.isEmpty()) return

        try {
            val dotRadius = 6f

            // Mata kiri (CYAN)
            leftEyeIdx.forEach { idx ->
                val (x, y) = toScreen(allPoints[idx])
                canvas.drawCircle(x, y, dotRadius, paintLeftEye)
            }
            drawEyeOutline(canvas, leftEyeIdx, paintLeftEye)

            // Mata kanan (KUNING)
            rightEyeIdx.forEach { idx ->
                val (x, y) = toScreen(allPoints[idx])
                canvas.drawCircle(x, y, dotRadius, paintRightEye)
            }
            drawEyeOutline(canvas, rightEyeIdx, paintRightEye)

            // Mulut (HIJAU) — titik atas dan bawah
            mouthIdx.forEach { idx ->
                val (x, y) = toScreen(allPoints[idx])
                canvas.drawCircle(x, y, dotRadius, paintMouth)
            }

            drawMouthOutline(canvas)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun drawEyeOutline(canvas: Canvas, indices: IntArray, paint: Paint) {
        val lp = Paint(paint).apply {
            style = Paint.Style.STROKE; strokeWidth = 2f; alpha = 200
        }
        val order = intArrayOf(0, 1, 2, 3, 4, 5, 0)
        for (i in 0 until order.size - 1) {
            val (x1, y1) = toScreen(allPoints[indices[order[i]]])
            val (x2, y2) = toScreen(allPoints[indices[order[i + 1]]])
            canvas.drawLine(x1, y1, x2, y2, lp)
        }
    }

    private fun drawMouthOutline(canvas: Canvas) {
        val lp = Paint(paintMouth).apply {
            style = Paint.Style.STROKE; strokeWidth = 2f; alpha = 200
        }

        val mouthPoints = mouthIdx.map { toScreen(allPoints[it]) }

        // Garis horizontal atas: sudut kiri → sudut kanan
        for (i in mouthPoints.indices) {
            val p1 = mouthPoints[i]
            val p2 = mouthPoints[(i + 1) % mouthPoints.size]
            canvas.drawLine(p1.first, p1.second, p2.first, p2.second, lp)
        }

        // Garis horizontal atas dalam: topL → topC → topR

        // Garis horizontal bawah dalam: botL → botC → botR

        // Garis vertikal: atas ke bawah (3 pasang)

        // Garis ke sudut: sudut kiri → topL, sudut kiri → botL

        // Garis ke sudut: sudut kanan → topR, sudut kanan → botR
    }
}
