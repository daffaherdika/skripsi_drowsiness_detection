package com.example.drowsinessdetection.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {
    fun exportPerformance(context: Context, records: List<PerformanceRecord>, label: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "performa_${label}_$timestamp.csv"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            savePerformanceMediaStore(context, records, fileName)
        } else {
            savePerformanceLegacy(records, fileName)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun savePerformanceMediaStore(
        context: Context, records: List<PerformanceRecord>, fileName: String
    ): String {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
        ) ?: return "Gagal membuat file"
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.writer().use { writer ->
                writer.write("time,fps,latency_ms,temp_celsius\n")
                records.forEach { r -> writer.write(buildPerformanceRow(r) + "\n") }
            }
        }
        return "Downloads/$fileName"
    }

    private fun savePerformanceLegacy(records: List<PerformanceRecord>, fileName: String): String {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(dir, fileName)
        FileWriter(file).use { writer ->
            writer.write("time,fps,latency_ms,temp_celsius\n")
            records.forEach { r -> writer.write(buildPerformanceRow(r) + "\n") }
        }
        return file.absolutePath
    }

    private fun buildPerformanceRow(r: PerformanceRecord) =
        String.format(
            Locale.US,
            "%.0f,%.0f,%d,%.1f",
            r.time, r.fps, r.latencyMs, r.tempCelsius
        )
}
