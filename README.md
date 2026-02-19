# 🐛 PEstAI: Real-Time Edge AI Detection

PEstAI is a high-performance Android application optimized for real-time edge computing. It provides a lag-free object detection pipeline using advanced vision models running entirely on-device via TensorFlow Lite.

## 🌟 Implemented Features

* **Dual-Model Support:** Ships with both **YOLOv8 Nano** and **YOLOv11 Nano** models pre-loaded in the project assets, allowing for flexible testing and deployment.
* **Hybrid Quantization Engine:** The TFLite interpreter is configured to perfectly handle Float32 input/output tensors while the models utilize Int8 quantized internal weights. This keeps the app size minimal (~3MB per model) without crashing from data type mismatches.
* **Lag-Free Camera Preview:** Implements a decoupled CameraX architecture. The inference engine uses a custom "Busy-Flag" backpressure strategy, instantly dropping frames when the CPU is busy. This guarantees the user's camera feed remains at a fluid 30 FPS regardless of the device's processing power.
* **Strict Resolution Pipeline:** Pre-processes the camera feed into a strict 640x640 normalized float array (0.0f - 1.0f) to match the exact training parameters of the YOLO models.

## 🏗️ Technical Architecture

1. **Producer:** `androidx.camera.view.PreviewView` captures frames constantly.
2. **Filter:** The `ImageAnalysis.Analyzer` checks the inference status. If the model is currently processing a frame, the new frame is closed and discarded immediately to prevent CPU queuing and thermal throttling.
3. **Consumer:** The `Detector.kt` processes the 640x640 frame on a dedicated background `Executor`.

## 🛠️ Tech Stack

* **Language:** Kotlin
* **Framework:** Android SDK (API 24+)
* **Machine Learning:** TensorFlow Lite 2.14.0
* **Vision System:** AndroidX CameraX

## 🚀 Getting Started

1. Clone the repository to your local machine.
2. Open the project in **Android Studio**.
3. *(No model download required - `yolov8n.tflite` and `yolov11n.tflite` are already included in the `src/main/assets/` folder).*
4. Sync Gradle and click **Run** to deploy to your Android device.