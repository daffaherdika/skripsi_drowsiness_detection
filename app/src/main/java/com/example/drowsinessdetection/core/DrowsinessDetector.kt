package com.example.drowsinessdetection.core

/**
 * Status deteksi kantuk
 * NORMAL                  = tidak mengantuk
 * KANTUK                  = EAR rendah > 3 detik (microsleep) ATAU MAR tinggi > 3 detik (menguap)
 * WAJAH_TIDAK_TERDETEKSI = wajah tidak ditemukan sehingga EAR dan MAR tidak dapat dievaluasi
 */
enum class EyeStatus { NORMAL, KANTUK, WAJAH_TIDAK_TERDETEKSI }

/**
 * Threshold statis:
 * EAR < 0.24 selama > 3000ms = microsleep
 * MAR > 0.5 selama > 3000ms = menguap
 */
class DrowsinessDetector {

    companion object {
        const val EAR_THRESHOLD = 0.24f
        const val MAR_THRESHOLD = 0.5f
        const val EAR_DURATION_MS = 3000L   // 3 detik
        const val MAR_DURATION_MS = 3000L   // 3 detik
    }

    private var earClosedSince: Long? = null
    private var mouthOpenSince: Long? = null

    fun update(ear: Float, mar: Float, faceDetected: Boolean): EyeStatus {
        if (!faceDetected) {
            reset()
            return EyeStatus.WAJAH_TIDAK_TERDETEKSI
        }

        val now = System.currentTimeMillis()

        // --- Cek EAR (microsleep) ---
        if (ear < EAR_THRESHOLD) {
            if (earClosedSince == null) earClosedSince = now
            if (now - earClosedSince!! >= EAR_DURATION_MS) return EyeStatus.KANTUK
        } else {
            earClosedSince = null
        }

        // --- Cek MAR (menguap) ---
        if (mar > MAR_THRESHOLD) {
            if (mouthOpenSince == null) mouthOpenSince = now
            if (now - mouthOpenSince!! >= MAR_DURATION_MS) return EyeStatus.KANTUK
        } else {
            mouthOpenSince = null
        }

        return EyeStatus.NORMAL
    }

    fun reset() {
        earClosedSince = null
        mouthOpenSince = null
    }
}
