# Cosmic Orbit (OrbitEdu) 🌌

An interactive, highly polished, and educational astrophysics simulation app built entirely in Jetpack Compose. **Cosmic Orbit** helps students and astronomy enthusiasts visualize complex celestial dynamics—including the Day/Night cycle, seasonal variations, moon phases, solar eclipses, and lunar eclipses—using real-time interactive physics models and conversational AI.

---

## 🌟 Key Features

### 1. Interactive Celestial Simulator
*   **Real-time Canvas Rendering**: A beautiful, custom-rendered physical space view representing the Sun, Earth, shadow cones (umbra and penumbra), and the Moon's orbit.
*   **Camera Views**: Switch seamlessly between three camera perspectives:
    *   🌌 **System View**: A broad overview of the entire Earth-Sun-Moon coordinate system.
    *   🌍 **Close-Up View**: Focuses on the Earth to observe day/night transitions and axial tilt effects.
    *   🌒 **Eclipse View**: Aligns the view to clearly see shadows and occlusion during eclipses.

### 2. Deep Time & Parameter Control
*   **Earth Spin (24H Loop)**: Adjust the time of day to watch the Earth spin on its axis. Features a real-time **Play 24H** animation mode.
*   **Earth Orbit (Yearly Loop)**: Adjust the month of the year to watch the Earth orbit the Sun. Features a real-time **Play Year** animation mode.
*   **Moon Phase Slider**: Control and visualize moon phases (New Moon, Crescent, Gibbous, Full Moon, etc.) with real-time phase indicator tags.
*   **Axial Tilt Controller**: Alter Earth's tilt from $0^\circ$ to $45^\circ$ to see how tilt affects seasonal solar intensity (Earth actual $23.5^\circ$ marked as reference).

### 3. Interactive Study Lab (Interactive Lessons)
Curated physical lessons featuring automatic parameter alignment to demonstrate four key physical phenomena:
*   ☀️ **Solar Seasons**: Observe how the $23.5^\circ$ tilt causes the winter/summer solstices and equinoxes as Earth orbits the Sun.
*   🌑 **Solar Eclipse**: Watch the Moon pass directly between the Sun and the Earth, casting a shadow on the Earth.
*   🌕 **Lunar Eclipse**: Watch the Moon enter Earth's dark shadow (umbra), producing a dramatic "Blood Moon" due to Rayleigh scattering.
*   🌗 **Day & Night Cycle**: Visualize how solar radiation falls on a rotating Earth to create day/night halves.

*Each lesson displays interactive study guides, bullet facts, and physical explanations.*

### 4. Stella - Cosmic Tutor (Gemini AI Chat) 🛰️
*   Ask Stella any question about eclipses, orbital mechanics, planetary gravity, or astrophysics!
*   **Context-Aware**: Stella's conversational AI helps simplify complex concepts for students of all levels.
*   **Local Fallback**: Works offline or without an active API key using a local smart astrophysicist response engine.

### 5. Custom Gemini API Settings Dialog ⚙️
*   **Configurable API Connection**: A secure settings panel allows students or developers to paste their own **Google AI Studio API Key**.
*   **Persistent & Secure**: Keys are saved securely in local Android Shared Preferences.
*   **Status Indicator**: Shows a live indicator pill on the chat header (**AI ONLINE** vs. **OFFLINE MODE**).
*   **Easy Setup Guidance**: Direct link to [Google AI Studio](https://aistudio.google.com/) is provided to easily generate a free API key.

### 6. Full Localization (English & German) 🌐
*   Supports runtime locale switching. All UI labels, physical event summaries, lesson plans, and settings dialogues are fully translated into both **English** and **German** (`values-de/strings.xml`).

---

## 🛠️ Architecture & Tech Stack

*   **UI Framework**: 100% Jetpack Compose with Material Design 3 (M3).
*   **State Management**: Unidirectional Data Flow (UDF) powered by `ViewModel` and `StateFlow` / `collectAsState`.
*   **Network Service**: OkHttp for ultra-fast, safe, and asynchronous REST interactions with the Google Gemini API.
*   **Testing**: Fully configured for local JVM testing with **Robolectric** and screenshot testing with **Roborazzi** for fast visual regression tracking.
*   **Localization**: Android Resource Bundling (`strings.xml`) ensuring proper translation management.

---

## 🚀 Running and Testing

### Local Signing Setup (Auto-Fallback)
The project includes a custom signing configuration. To support both cloud-based AI Studio containers and local Android Studio machines seamlessly:
*   **AI Studio Environment**: Automatically signs using the local workspace `${rootDir}/debug.keystore`.
*   **Local Developer Machine**: If the workspace keystore is absent (as it is excluded by `.gitignore`), the build configuration automatically falls back to your machine's standard user-level debug keystore at `~/.android/debug.keystore`. Android Studio generates this automatically, allowing the app to build out-of-the-box locally.

### Build & Run
To compile and assemble the application APK, execute:
```bash
gradle assembleDebug
```

### Run Unit & Robolectric Tests
Verify all critical user journeys, states, and business logic instantly on the JVM:
```bash
gradle :app:testDebugUnitTest
```

### Visual Verification (Roborazzi Screenshots)
To run and verify visual screenshot tests:
```bash
gradle :app:verifyRoborazziDebug
```
To record new visual reference screenshots (after styling or layout adjustments):
```bash
gradle :app:recordRoborazziDebug
```

---

## 🌌 Explore the Cosmos!
1. Open the application.
2. Align parameters manually, or tap a lesson in the **Interactive Study Lab**.
3. Open the **Settings Gear** to paste your Gemini API Key.
4. Chat with **Stella** to unlock deep celestial secrets! 🚀
