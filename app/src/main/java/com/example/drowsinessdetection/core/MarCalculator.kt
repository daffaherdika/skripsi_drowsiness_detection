package com.example.drowsinessdetection.core

import com.google.mlkit.vision.facemesh.FaceMeshPoint
import kotlin.math.sqrt

/**
 * MAR = Mouth Aspect Ratio
 *
 * Menggunakan 8 landmark bibir bagian dalam:
 *        p2   p3   p4
 *   p1                 p5
 *        p8   p7   p5
 *
 *
 *        81   13   311
 *   78                 308
 *        88   14   318
 *
 * Rumus: (|P2-P8| + |P3-P7| + |P4-P6|) / (3 × |P1-P5|)
 */
object MarCalculator {

    fun calculate(allPoints: List<FaceMeshPoint>): Float {
        return try {
            // Sudut kiri dan kanan mulut
            val left  = allPoints[78]
            val right = allPoints[308]

            // Titik vertikal atas (3 titik bibir atas dalam)
            val topLeft   = allPoints[81]
            val topCenter = allPoints[13]
            val topRight  = allPoints[311]

            // Titik vertikal bawah (3 titik bibir bawah dalam)
            val botLeft   = allPoints[88]
            val botCenter = allPoints[14]
            val botRight  = allPoints[318]

            // Jarak vertikal (atas ke bawah, 3 pasang)
            val v1 = distance(topLeft,   botLeft)    // kiri
            val v2 = distance(topCenter, botCenter)  // tengah
            val v3 = distance(topRight,  botRight)   // kanan

            // Jarak horizontal (lebar mulut)
            val h = distance(left, right)

            if (h == 0f) 0f
            else (v1 + v2 + v3) / (3f * h)

        } catch (e: Exception) {
            0f
        }
    }

    private fun distance(a: FaceMeshPoint, b: FaceMeshPoint): Float {
        val dx = a.position.x - b.position.x
        val dy = a.position.y - b.position.y
        val dz = a.position.z - b.position.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}
