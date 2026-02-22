# 🐛 PestAI: Contextual Edge-AI for Crop Protection

Farmers lose billions of dollars to crop pests every year, but identifying the exact species before the infestation spreads is incredibly difficult. Most farmers don't have entomologists on speed dial, and spraying chemicals at the wrong time leads to environmental damage and financial loss.

**PestAI** is a real-time, edge-computing mobile tool that puts an agricultural expert in the farmer's pocket. It combines on-device machine learning with dynamic weather heuristics to not only detect pests but also advise on the exact right time to treat the crop.

---

## ✨ Spray Viability Decision Engine
* **Contextual Weather Warnings:** Issues a "Do Not Spray" warning if rain, high wind, or extreme temperatures are detected, preventing chemical runoff and saving farmers money.
* **Dynamic Location Services:** Automatically requests the user's location to provide hyper-local, accurate weather data for spray recommendations.
* **Live Weather Dashboard:** The home screen displays the current location, temperature, and wind speed, giving farmers a complete picture of the environmental conditions before treating crops.

---

## 🧠 Advanced AI/ML Architecture
We didn't just wrap a model in an app; we engineered a production-grade, context-aware pipeline optimized for low-end mobile hardware.

* **Active Learning Feedback Loop:** Users can tap on an incorrect bounding box to instantly hide it. The AI adds that coordinate to a "penalized zone" for the rest of the session, preventing the same false-positive from recurring.
* **Context-Aware Habitat Filter:** An intelligent heuristic filter that probes the pixels immediately outside a bounding box. If the background isn't mathematically detected as green leaves or brown soil, the model's confidence is dynamically penalized to reduce false positives on unnatural surfaces.
* **Anti-Flicker Engine:** A custom 3-frame memory grace period integrated into the Non-Maximum Suppression (NMS) logic that prevents bounding boxes from blinking in and out of existence during camera shake, providing a highly stable user experience.
* **Zero-Distortion Cropping:** Prevents 16:9 camera feeds from being squashed into 1:1 AI tensors, preserving real-world geometry for higher inference accuracy.

---

## 🛠️ Tech Stack
* **Language:** Kotlin
* **Machine Learning:** TensorFlow Lite (YOLOv8 Nano)
* **Networking:** Retrofit and Gson for communicating with the OpenWeatherMap API.
* **Asynchronous Operations:** Kotlin Coroutines and `lifecycleScope` for managing background tasks and API calls without blocking the UI thread.
* **Camera:** AndroidX CameraX for a modern, lifecycle-aware edge-computing camera implementation.
* **Location:** Google Play Services Fused Location Provider API.
* **UI Framework:** Modern Android UI with Material Design components, ConstraintLayout, and CoordinatorLayout.
* **Architecture:** Single-Activity architecture using the AndroidX Navigation Component with Fragments.

---

## ⚙️ Installation & Setup

1. **Clone the repository:**
    git clone https://github.com/YOUR_USERNAME/PestAI.git

2. **Open in Android Studio:** Ensure you have the latest stable version installed.

3. **Add your API Key:** * Obtain a free API key from OpenWeatherMap.
    * Place your key securely in your project.

4. **Build and Run:** Connect a physical Android device via USB debugging for the best camera and ML performance.
