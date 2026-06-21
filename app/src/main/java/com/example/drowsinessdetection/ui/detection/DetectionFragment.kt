package com.example.drowsinessdetection.ui.detection

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.TransformExperimental
import androidx.camera.view.transform.CoordinateTransform
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.drowsinessdetection.R
import com.example.drowsinessdetection.core.DrowsinessDetector
import com.example.drowsinessdetection.core.EyeStatus
import com.example.drowsinessdetection.core.FaceMeshAnalyzer
import com.example.drowsinessdetection.data.CsvExporter
import com.example.drowsinessdetection.data.PerformanceRecord
import com.example.drowsinessdetection.databinding.FragmentDetectionBinding
import com.example.drowsinessdetection.utils.AlarmUtils
import com.example.drowsinessdetection.utils.SystemMonitor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@TransformExperimental
class DetectionFragment : Fragment() {

    private var _binding: FragmentDetectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private val detector = DrowsinessDetector()
    private var useClahe = false
    @Volatile
    private var showLandmark = false

    // Data performa
    private val records = mutableListOf<PerformanceRecord>()
    private var currentFps      = 0f
    private var currentLatency  = 0L
    private var currentEar      = 0f
    private var currentMar      = 0f
    private var currentStatus   = EyeStatus.NORMAL
    private var renderedStatus: EyeStatus? = null
    private val processedFrameCount = AtomicInteger(0)

    // Timer durasi deteksi
    private var durationTimer: CountDownTimer? = null
    private var elapsedSeconds = 0L

    // Timer rekam performa per detik
    private var recordTimer: CountDownTimer? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else Toast.makeText(requireContext(), "Izin kamera diperlukan!", Toast.LENGTH_LONG).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.keepScreenOn = true
        useClahe = arguments?.getBoolean("useClahe") ?: false

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.tvThreshold.text = "EAR < ${DrowsinessDetector.EAR_THRESHOLD} | MAR > ${DrowsinessDetector.MAR_THRESHOLD}"
        binding.tvClaheStatus.text = "CLAHE: ${if (useClahe) "ON" else "OFF"}"

        // Tombol Stop → tampilkan dialog
        binding.btnStop.setOnClickListener {
            showStopDialog()
        }

        // Tombol toggle landmark
        binding.btnToggleLandmark.setOnClickListener {
            showLandmark = !showLandmark
            binding.btnToggleLandmark.text = if (showLandmark) "👁 Landmark: ON" else "👁 Landmark: OFF"
            binding.cameraBitmapView.visibility = if (showLandmark) View.VISIBLE else View.GONE
            if (!showLandmark) {
                binding.cameraBitmapView.setImageDrawable(null)
                binding.landmarkOverlay.clear()
            }
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        startDurationTimer()
        startRecordTimer()
    }

    private fun startDurationTimer() {
        // Hitung durasi deteksi dari 00:00 ke atas (countup)
        durationTimer = object : CountDownTimer(Long.MAX_VALUE, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                elapsedSeconds++
                val minutes = elapsedSeconds / 60
                val seconds = elapsedSeconds % 60
                binding.tvTimer.text = "%02d:%02d".format(minutes, seconds)
            }
            override fun onFinish() {}
        }.start()
    }

    private fun startRecordTimer() {
        // Rekam data performa setiap 1 detik
        processedFrameCount.set(0)
        currentFps = 0f

        recordTimer = object : CountDownTimer(Long.MAX_VALUE, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                currentFps = processedFrameCount.getAndSet(0).toFloat()
                val temp = SystemMonitor.getBatteryTemp(requireContext())

                activity?.runOnUiThread {
                    binding.tvFps.text  = "%.0f".format(currentFps)
                    binding.tvTemp.text = "%.1f°C".format(temp)
                    binding.tvLatency.text = "${currentLatency}ms"
                }

                records.add(PerformanceRecord(
                    time        = elapsedSeconds.toFloat(),
                    fps         = currentFps,
                    latencyMs   = currentLatency,
                    tempCelsius = temp
                ))
            }
            override fun onFinish() {}
        }.start()
    }

    private fun startCamera() {
        if (binding.previewView.width == 0 || binding.previewView.height == 0) {
            binding.previewView.post { startCamera() }
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also {
                    it.setAnalyzer(
                        cameraExecutor,
                        FaceMeshAnalyzer(
                            useClahe = useClahe,
                            shouldCreateDisplayBitmap = { showLandmark },
                            onResult = { ear, mar, faceDetected, _, latency, allPoints, imageWidth, imageHeight, displayBitmap, _ ->
                                processedFrameCount.incrementAndGet()
                                currentEar     = ear
                                currentMar     = mar
                                currentLatency = latency
                                currentStatus  = detector.update(ear, mar, faceDetected)
                                activity?.runOnUiThread {
                                    if (showLandmark && displayBitmap != null) {
                                        binding.cameraBitmapView.setImageBitmap(displayBitmap)
                                    }
                                    updateUI(ear, mar, currentFps, currentStatus)
                                    if (showLandmark && allPoints != null) {
                                        binding.landmarkOverlay.updateLandmarks(
                                            allPoints,
                                            imageWidth,
                                            imageHeight,
                                            null,
                                            mirror = false
                                        )
                                    } else {
                                        binding.landmarkOverlay.clear()
                                    }
                                }
                            }
                        )
                    )
                }
            try {
                cameraProvider.unbindAll()
                val viewPort = binding.previewView.viewPort
                if (viewPort != null) {
                    val useCaseGroup = UseCaseGroup.Builder()
                        .addUseCase(preview)
                        .addUseCase(imageAnalysis)
                        .setViewPort(viewPort)
                        .build()
                    cameraProvider.bindToLifecycle(
                        viewLifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        useCaseGroup
                    )
                } else {
                    cameraProvider.bindToLifecycle(
                        viewLifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview, imageAnalysis
                    )
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal buka kamera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun updateUI(ear: Float, mar: Float, fps: Float, status: EyeStatus) {
        binding.tvEarValue.text = "%.3f".format(ear)
        binding.tvMarValue.text = "%.3f".format(mar)
        binding.tvFps.text      = "%.0f".format(fps)

        if (status == EyeStatus.KANTUK) {
            AlarmUtils.tickDrowsyAlarm(requireContext())
        }

        if (status == renderedStatus) return
        renderedStatus = status

        when (status) {
            EyeStatus.NORMAL -> {
                binding.tvDetectionStatus.text = "● NORMAL"
                binding.tvDetectionStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.status_open))
                binding.tvEarValue.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.status_open))
                binding.tvMarValue.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.status_open))
                AlarmUtils.stopAlarm()
            }
            EyeStatus.KANTUK -> {
                binding.tvDetectionStatus.text = "MENGANTUK!"
                binding.tvDetectionStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.status_drowsy))
                binding.tvEarValue.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.status_drowsy))
                binding.tvMarValue.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.status_drowsy))
            }
            EyeStatus.WAJAH_TIDAK_TERDETEKSI -> {
                binding.tvDetectionStatus.text = "WAJAH TIDAK TERDETEKSI"
                binding.tvDetectionStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.status_closed))
                binding.tvEarValue.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.text_secondary))
                binding.tvMarValue.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.text_secondary))
                AlarmUtils.stopAlarm()
            }
        }
    }

    private fun showStopDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Hentikan Deteksi?")
            .setMessage("Sudah merekam ${records.size} detik data performa.\nTetap simpan data?")
            .setPositiveButton("Simpan & Keluar") { _, _ ->
                saveAndExit()
            }
            .setNegativeButton("Lanjutkan") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Buang & Keluar") { _, _ ->
                exitWithoutSave()
            }
            .show()
    }

    private fun saveAndExit() {
        if (records.isNotEmpty()) {
            val label = if (useClahe) "clahe_on" else "clahe_off"
            val path = CsvExporter.exportPerformance(requireContext(), records, label)
            Toast.makeText(requireContext(), "✅ Tersimpan: $path", Toast.LENGTH_LONG).show()
        }
        exitWithoutSave()
    }

    private fun exitWithoutSave() {
        AlarmUtils.stopAlarm()
        durationTimer?.cancel()
        recordTimer?.cancel()
        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AlarmUtils.stopAlarm()
        durationTimer?.cancel()
        recordTimer?.cancel()
        cameraExecutor.shutdown()
        _binding = null
    }
}
