package com.avikshit.PestAI

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.SystemClock
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class Detector(
    private val context: Context,
    private val modelPath: String,
    private val labelPath: String,
    private val detectorListener: DetectorListener,
) {

    // Short-term memory for the 3-Frame Verification Engine
    // Anti-Blink Engine Memory
    private var lastStableBoxes = listOf<BoundingBox>()
    private var emptyFrameCount = 0

    private val penalizedZones = mutableListOf<RectF>()

    private var interpreter: Interpreter
    private var labels = mutableListOf<String>()

    private var tensorWidth = 0
    private var tensorHeight = 0
    private var numChannel = 0
    private var numElements = 0

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()

    init {
        val compatList = CompatibilityList()

        val options = Interpreter.Options().apply{
            if(compatList.isDelegateSupportedOnThisDevice){
                val delegateOptions = compatList.bestOptionsForThisDevice
                this.addDelegate(GpuDelegate(delegateOptions))
            } else {
                this.setNumThreads(4)
            }
        }

        val model = FileUtil.loadMappedFile(context, modelPath)
        interpreter = Interpreter(model, options)

        val inputShape = interpreter.getInputTensor(0)?.shape()
        val outputShape = interpreter.getOutputTensor(0)?.shape()

        if (inputShape != null) {
            tensorWidth = inputShape[1]
            tensorHeight = inputShape[2]

            // If in case input shape is in format of [1, 3, ..., ...]
            if (inputShape[1] == 3) {
                tensorWidth = inputShape[2]
                tensorHeight = inputShape[3]
            }
        }

        if (outputShape != null) {
            numChannel = outputShape[1]
            numElements = outputShape[2]
        }

        try {
            val inputStream: InputStream = context.assets.open(labelPath)
            val reader = BufferedReader(InputStreamReader(inputStream))

            var line: String? = reader.readLine()
            while (line != null && line != "") {
                labels.add(line)
                line = reader.readLine()
            }

            reader.close()
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun restart(isGpu: Boolean) {
        interpreter.close()

        val options = if (isGpu) {
            val compatList = CompatibilityList()
            Interpreter.Options().apply{
                if(compatList.isDelegateSupportedOnThisDevice){
                    val delegateOptions = compatList.bestOptionsForThisDevice
                    this.addDelegate(GpuDelegate(delegateOptions))
                } else {
                    this.setNumThreads(4)
                }
            }
        } else {
            Interpreter.Options().apply{
                this.setNumThreads(4)
            }
        }

        val model = FileUtil.loadMappedFile(context, modelPath)
        interpreter = Interpreter(model, options)
    }

    fun close() {
        interpreter.close()
    }

    fun detect(frame: Bitmap) {
        if (tensorWidth == 0) return
        if (tensorHeight == 0) return
        if (numChannel == 0) return
        if (numElements == 0) return

        var inferenceTime = SystemClock.uptimeMillis()

        // 1. Crop the raw camera feed to a square first
        val squareBitmap = cropToSquare(frame)

        // 2. Scale the perfectly square image down to match the YOLO model
        val resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, tensorWidth, tensorHeight, false)

        val tensorImage = TensorImage(INPUT_IMAGE_TYPE)
        tensorImage.load(resizedBitmap)

        val processedImage = imageProcessor.process(tensorImage)
        val imageBuffer = processedImage.buffer

        val output = TensorBuffer.createFixedSize(intArrayOf(1, numChannel, numElements), OUTPUT_IMAGE_TYPE)
        interpreter.run(imageBuffer, output.buffer)

        val bestBoxes = bestBox(output.floatArray, squareBitmap)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        if (bestBoxes == null) {
            detectorListener.onEmptyDetect()
            return
        }

        // --- LIVE COUNTER LOGIC ---
        // Group the boxes by name and count how many of each pest are on screen
        val pestCounts = bestBoxes.groupingBy { it.clsName }.eachCount()

        // Send the boxes, the time, AND the counts to the frontend
        detectorListener.onDetect(bestBoxes, inferenceTime, pestCounts)
    }
    
    fun penalizeDetection(x: Float, y: Float) {
        val tappedBox = lastStableBoxes.find { box ->
            x >= box.x1 && x <= box.x2 && y >= box.y1 && y <= box.y2
        }

        if (tappedBox != null) {
            penalizedZones.add(RectF(tappedBox.x1, tappedBox.y1, tappedBox.x2, tappedBox.y2))

            // Remove the penalized box from the list of stable boxes
            lastStableBoxes = lastStableBoxes.filter { it != tappedBox }

            // Notify the listener to update the UI instantly
            val pestCounts = lastStableBoxes.groupingBy { it.clsName }.eachCount()
            detectorListener.onDetect(lastStableBoxes, 0, pestCounts)
        }
    }

    private fun bestBox(array: FloatArray, bitmap: Bitmap) : List<BoundingBox>? {

        val boundingBoxes = mutableListOf<BoundingBox>()

        for (c in 0 until numElements) {
            var maxConf = CONFIDENCE_THRESHOLD
            var maxIdx = -1
            var j = 4
            var arrayIdx = c + numElements * j
            while (j < numChannel){
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx]
                    maxIdx = j - 4
                }
                j++
                arrayIdx += numElements
            }

            if (maxConf > CONFIDENCE_THRESHOLD) {
                val cx = array[c]
                val cy = array[c + numElements]
                val w = array[c + numElements * 2]
                val h = array[c + numElements * 3]

                var finalConf = maxConf
                if (!isHabitat(bitmap, cx, cy, w, h)) { finalConf *= 0.5F }

                val isPenalized = penalizedZones.any { it.contains(cx, cy) }
                if (isPenalized) {
                    finalConf = 0.0F
                }

                if (finalConf > CONFIDENCE_THRESHOLD) {
                    val clsName = labels[maxIdx]
                    val x1 = cx - (w/2F)
                    val y1 = cy - (h/2F)
                    val x2 = cx + (w/2F)
                    val y2 = cy + (h/2F)
                    if (x1 < 0F || x1 > 1F) continue
                    if (y1 < 0F || y1 > 1F) continue
                    if (x2 < 0F || x2 > 1F) continue
                    if (y2 < 0F || y2 > 1F) continue

                    boundingBoxes.add(
                        BoundingBox(
                            x1 = x1, y1 = y1, x2 = x2, y2 = y2,
                            cx = cx, cy = cy, w = w, h = h,
                            cnf = finalConf, cls = maxIdx, clsName = clsName
                        )
                    )
                }
            }
        }

        // 1. Get the standard boxes
        val nmsBoxes = if (boundingBoxes.isEmpty()) emptyList<BoundingBox>() else applyNMS(boundingBoxes)

        // 2. THE ANTI-BLINK ENGINE
        if (nmsBoxes.isNotEmpty()) {
            // Pest detected! Update our stable memory and show it immediately.
            lastStableBoxes = nmsBoxes
            emptyFrameCount = 0
            return nmsBoxes
        } else {
            // No pest detected in THIS frame.
            // Give it a 3-frame grace period before deleting the box.
            emptyFrameCount++
            if (emptyFrameCount <= 3 && lastStableBoxes.isNotEmpty()) {
                return lastStableBoxes // Keep drawing the last known boxes so it doesn't blink
            } else {
                lastStableBoxes = emptyList() // It's truly gone
                return null
            }
        }
    } // End of bestBox function

    private fun applyNMS(boxes: List<BoundingBox>) : MutableList<BoundingBox> {
        val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
        val selectedBoxes = mutableListOf<BoundingBox>()

        while(sortedBoxes.isNotEmpty()) {
            val first = sortedBoxes.first()
            selectedBoxes.add(first)
            sortedBoxes.remove(first)

            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                val iou = calculateIoU(first, nextBox)
                if (iou >= IOU_THRESHOLD) {
                    iterator.remove()
                }
            }
        }

        return selectedBoxes
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)
        val intersectionArea = maxOf(0F, x2 - x1) * maxOf(0F, y2 - y1)
        val box1Area = box1.w * box1.h
        val box2Area = box2.w * box2.h
        return intersectionArea / (box1Area + box2Area - intersectionArea)
    }

    interface DetectorListener {
        fun onEmptyDetect()
        fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long, pestCounts: Map<String, Int>)
    }

    companion object {
        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
        private const val CONFIDENCE_THRESHOLD = 0.55F
        private const val IOU_THRESHOLD = 0.5F
    }
}

// Helper function to crop the camera frame to a perfect square
private fun cropToSquare(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val minEdge = Math.min(width, height)

    // Calculate the center crop coordinates
    val startX = (width - minEdge) / 2
    val startY = (height - minEdge) / 2

    return Bitmap.createBitmap(bitmap, startX, startY, minEdge, minEdge)
}
// Checks the 4 pixels immediately outside the bounding box for Leaf (Green) or Soil (Brown) colors
private fun isHabitat(bitmap: Bitmap, cx: Float, cy: Float, w: Float, h: Float): Boolean {
    val pixelCx = (cx * bitmap.width).toInt()
    val pixelCy = (cy * bitmap.height).toInt()
    val pixelW = (w * bitmap.width).toInt()
    val pixelH = (h * bitmap.height).toInt()
    val offset = 15
    val points = listOf(
        Pair(pixelCx, (pixelCy - pixelH / 2 - offset).coerceIn(0, bitmap.height - 1)),
        Pair(pixelCx, (pixelCy + pixelH / 2 + offset).coerceIn(0, bitmap.height - 1)),
        Pair((pixelCx - pixelW / 2 - offset).coerceIn(0, bitmap.width - 1), pixelCy),
        Pair((pixelCx + pixelW / 2 + offset).coerceIn(0, bitmap.width - 1), pixelCy)
    )
    var habitatScore = 0
    for (p in points) {
        val color = bitmap.getPixel(p.first, p.second)
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        val isGreen = g > r && g > b
        val isSoil = r > g && g > b && r < 180
        if (isGreen || isSoil) habitatScore++
    }
    return habitatScore >= 1
}
