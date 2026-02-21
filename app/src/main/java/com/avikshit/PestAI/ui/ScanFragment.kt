package com.avikshit.PestAI.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.avikshit.PestAI.BoundingBox
import com.avikshit.PestAI.Constants.LABELS_PATH
import com.avikshit.PestAI.Constants.MODEL_PATH
import com.avikshit.PestAI.Detector
import com.avikshit.PestAI.OverlayView
import com.avikshit.PestAI.R
import com.avikshit.PestAI.data.AppDatabase
import com.avikshit.PestAI.data.ScanEntity
import com.avikshit.PestAI.data.ScanRepository
import com.google.android.material.materialswitch.MaterialSwitch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanFragment : Fragment(R.layout.fragment_scan), Detector.DetectorListener {
    private val isFrontCamera = false

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var detector: Detector? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var scanRepository: ScanRepository

    private var viewFinder: androidx.camera.view.PreviewView? = null
    private var overlayView: OverlayView? = null
    private var inferenceTextView: TextView? = null
    private var gpuSwitch: MaterialSwitch? = null

    private var lastSavedAtMillis: Long = 0L

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it[Manifest.permission.CAMERA] == true) {
                startCamera()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewFinder = view.findViewById(R.id.viewFinder)
        overlayView = view.findViewById(R.id.overlay)
        inferenceTextView = view.findViewById(R.id.tvInferenceTime)
        gpuSwitch = view.findViewById(R.id.switchGpu)

        scanRepository = ScanRepository(AppDatabase.getInstance(requireContext()).scanDao())
        cameraExecutor = Executors.newSingleThreadExecutor()

        cameraExecutor.execute {
            detector = Detector(requireContext().applicationContext, MODEL_PATH, LABELS_PATH, this)
        }

        bindListeners()
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    override fun onPause() {
        super.onPause()
        cameraProvider?.unbindAll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        detector?.close()
        cameraExecutor.shutdown()
        viewFinder = null
        overlayView = null
        inferenceTextView = null
        gpuSwitch = null
    }

    private fun bindListeners() {
        gpuSwitch?.setOnCheckedChangeListener { _, isChecked ->
            cameraExecutor.submit {
                detector?.restart(isGpu = isChecked)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    private fun bindCameraUseCases() {
        val previewView = viewFinder ?: return
        val provider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val rotation = previewView.display.rotation
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            val bitmapBuffer = Bitmap.createBitmap(
                imageProxy.width,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )
            bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer)

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                if (isFrontCamera) {
                    postScale(
                        -1f,
                        1f,
                        imageProxy.width.toFloat(),
                        imageProxy.height.toFloat()
                    )
                }
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer,
                0,
                0,
                bitmapBuffer.width,
                bitmapBuffer.height,
                matrix,
                true
            )
            imageProxy.close()

            detector?.detect(rotatedBitmap)
        }

        provider.unbindAll()

        try {
            camera = provider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
            preview?.setSurfaceProvider(previewView.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onEmptyDetect() {
        activity?.runOnUiThread {
            overlayView?.clear()
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        activity?.runOnUiThread {
            inferenceTextView?.text = "${inferenceTime}ms"
            overlayView?.apply {
                setResults(boundingBoxes)
                invalidate()
            }
        }

        saveHighConfidenceDetection(boundingBoxes)
    }

    private fun saveHighConfidenceDetection(boundingBoxes: List<BoundingBox>) {
        val topDetection = boundingBoxes.maxByOrNull { it.cnf } ?: return
        if (topDetection.cnf < SAVE_CONFIDENCE_THRESHOLD) return

        val now = System.currentTimeMillis()
        if (now - lastSavedAtMillis < SAVE_COOLDOWN_MS) return
        lastSavedAtMillis = now

        cameraExecutor.execute {
            scanRepository.insertScan(
                ScanEntity(
                    pestName = topDetection.clsName,
                    confidence = topDetection.cnf,
                    timestamp = now,
                    remedySuggested = remedyFor(topDetection.clsName)
                )
            )
        }
    }

    private fun remedyFor(pestName: String): String {
        return when (pestName.lowercase()) {
            "aphid" -> "Use neem oil spray and remove infected leaves."
            "armyworm" -> "Use biological control and monitor crop edges."
            "whitefly" -> "Introduce sticky traps and avoid over-fertilization."
            else -> "Inspect affected area and apply integrated pest management."
        }
    }

    companion object {
        private const val TAG = "ScanFragmentCamera"
        private const val SAVE_COOLDOWN_MS = 5_000L
        private const val SAVE_CONFIDENCE_THRESHOLD = 0.80f
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
