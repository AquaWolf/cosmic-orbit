package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// Camera view modes for the 3D simulator
enum class CameraMode {
    SYSTEM,   // Shows the overall Sun-Earth-Moon system orbit
    EARTH,    // Close-up of Earth to study axial tilt, seasons, and day/night
    ECLIPSE   // Focuses on alignments and shadows (Solar & Lunar eclipses)
}

// Educational lessons available in the app
enum class LessonType {
    DAY_NIGHT,
    SEASONS,
    MOON_PHASES,
    SOLAR_ECLIPSE,
    LUNAR_ECLIPSE
}

data class Lesson(
    val type: LessonType,
    val title: String,
    val summary: String,
    val bulletPoints: List<String>,
    val alignmentTimeOfYear: Float, // 0.0 to 1.0 (0 is Jan 1st, 0.5 is early July)
    val alignmentTimeOfDay: Float,  // 0.0 to 1.0
    val alignmentMoonPhase: Float,  // 0.0 to 1.0
    val cameraMode: CameraMode,
    val explanationText: String
)

data class ChatMessage(
    val sender: String, // "user" or "tutor"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class CosmicViewModel : ViewModel() {

    // --- Simulation States ---
    private val _timeOfDay = MutableStateFlow(0.35f) // Fraction of day (0.0 to 1.0)
    val timeOfDay: StateFlow<Float> = _timeOfDay.asStateFlow()

    private val _timeOfYear = MutableStateFlow(0.15f) // Fraction of year (0.0 to 1.0)
    val timeOfYear: StateFlow<Float> = _timeOfYear.asStateFlow()

    private val _moonPhase = MutableStateFlow(0.25f) // Position of Moon around Earth (0.0 to 1.0)
    val moonPhase: StateFlow<Float> = _moonPhase.asStateFlow()

    private val _axialTilt = MutableStateFlow(23.5f) // Earth's tilt in degrees (default 23.5)
    val axialTilt: StateFlow<Float> = _axialTilt.asStateFlow()

    private val _isPlayingDay = MutableStateFlow(true)
    val isPlayingDay: StateFlow<Boolean> = _isPlayingDay.asStateFlow()

    private val _isPlayingYear = MutableStateFlow(false)
    val isPlayingYear: StateFlow<Boolean> = _isPlayingYear.asStateFlow()

    private val _cameraMode = MutableStateFlow(CameraMode.SYSTEM)
    val cameraMode: StateFlow<CameraMode> = _cameraMode.asStateFlow()

    // --- Educational States ---
    private val _selectedLesson = MutableStateFlow<Lesson?>(null)
    val selectedLesson: StateFlow<Lesson?> = _selectedLesson.asStateFlow()

    // --- AI Chat States ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                "tutor",
                "Hello, space explorer! 🚀 I'm Stella, your Cosmic Tutor. Ask me any question about the Earth's orbit, day and night, the seasons, Moon phases, or spectacular eclipses! You can also click the lessons on the side to align the simulation."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // OkHttp Client configured with higher timeout for Gemini API calls
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Pre-configured Lessons list
    val lessonsList = listOf(
        Lesson(
            type = LessonType.DAY_NIGHT,
            title = "Day & Night Cycle",
            summary = "How Earth's rotation on its axis creates light and shadow.",
            bulletPoints = listOf(
                "The Earth rotates once every 24 hours.",
                "The side facing the Sun is in Day; the side facing away is in Night.",
                "The dividing line between day and night is called the terminator."
            ),
            alignmentTimeOfYear = 0.15f,
            alignmentTimeOfDay = 0.5f,
            alignmentMoonPhase = 0.25f,
            cameraMode = CameraMode.EARTH,
            explanationText = "As the Earth spins counter-clockwise on its tilted axis, different countries rotate into the Sun's light (sunrise) and rotate out into its shadow (sunset). Try turning on 'Play Axis Spin' and watch the continents move in and out of the night shadow!"
        ),
        Lesson(
            type = LessonType.SEASONS,
            title = "The 4 Seasons",
            summary = "Why the Earth's 23.5° axial tilt causes changing temperatures.",
            bulletPoints = listOf(
                "The Earth's axis points in a fixed direction in space (toward Polaris).",
                "In June, the North Pole tilts TOWARD the Sun (Northern Summer).",
                "In December, the North Pole tilts AWAY from the Sun (Northern Winter).",
                "Equinoxes occur when neither pole is tilted toward the Sun, creating equal day and night."
            ),
            alignmentTimeOfYear = 0.47f, // June Solstice (approximate)
            alignmentTimeOfDay = 0.5f,
            alignmentMoonPhase = 0.25f,
            cameraMode = CameraMode.SYSTEM,
            explanationText = "Seasons are NOT caused by Earth being closer or further from the Sun! It is entirely due to the 23.5° axial tilt. In June, the Northern hemisphere tilts toward the Sun, receiving more direct, concentrated sunlight (Summer). At the exact same time, the Southern hemisphere is tilted away, receiving slanted rays (Winter). Select 'Play Orbit' to see this transition!"
        ),
        Lesson(
            type = LessonType.MOON_PHASES,
            title = "Moon Phases",
            summary = "How the Moon's 29.5-day orbit changes its appearance.",
            bulletPoints = listOf(
                "The Moon does not emit its own light; it reflects the Sun's rays.",
                "As the Moon orbits Earth, we see different amounts of its lit hemisphere.",
                "Positions: Sun-Moon-Earth is a New Moon. Sun-Earth-Moon is a Full Moon."
            ),
            alignmentTimeOfYear = 0.15f,
            alignmentTimeOfDay = 0.5f,
            alignmentMoonPhase = 0.5f, // Full Moon alignment
            cameraMode = CameraMode.SYSTEM,
            explanationText = "The Moon orbits the Earth. Since half of the Moon is always illuminated by the Sun, our perspective of that illuminated half changes. Move the Moon Phase slider or watch the Moon revolve around the Earth to watch it cycle from New Moon (invisible) to First Quarter (half lit) to Full Moon (fully lit)!"
        ),
        Lesson(
            type = LessonType.SOLAR_ECLIPSE,
            title = "Solar Eclipse",
            summary = "When the Moon casts a shadow on Earth, blocking the Sun.",
            bulletPoints = listOf(
                "Only occurs during a New Moon phase.",
                "The Moon aligns exactly between the Earth and the Sun.",
                "The Moon's small, dark shadow sweeps across Earth's surface."
            ),
            alignmentTimeOfYear = 0.15f,
            alignmentTimeOfDay = 0.45f,
            alignmentMoonPhase = 0.0f, // New Moon alignment
            cameraMode = CameraMode.ECLIPSE,
            explanationText = "During a Solar Eclipse, the Moon is directly between the Earth and the Sun, blocking the Sun's light. Because the Moon is relatively small, its shadow (umbra) only covers a tiny strip of the Earth. If you are standing in that shadow, the Sun goes completely dark, revealing the solar corona!"
        ),
        Lesson(
            type = LessonType.LUNAR_ECLIPSE,
            title = "Lunar Eclipse (Blood Moon)",
            summary = "When Earth blocks sunlight from the Moon, turning it coppery-red.",
            bulletPoints = listOf(
                "Only occurs during a Full Moon phase.",
                "The Earth aligns exactly between the Sun and the Moon.",
                "Rayleigh scattering bends red light into Earth's shadow, turning the Moon red."
            ),
            alignmentTimeOfYear = 0.15f,
            alignmentTimeOfDay = 0.5f,
            alignmentMoonPhase = 0.5f, // Full Moon alignment
            cameraMode = CameraMode.ECLIPSE,
            explanationText = "A Lunar Eclipse occurs when the Moon passes directly behind the Earth, entering Earth's dark shadow (umbra). Instead of going black, the Moon glows a spectacular reddish-orange! This 'Blood Moon' happens because Earth's atmosphere filters out blue light but refracts red light into the shadow cone, focusing it onto the Moon."
        )
    )

    init {
        // Start tickers to automatically simulate movement when toggled
        viewModelScope.launch {
            while (true) {
                delay(30) // Apprx. 33 FPS tick rate for smooth animations
                if (_isPlayingDay.value) {
                    _timeOfDay.value = (_timeOfDay.value + 0.005f) % 1.0f
                }
                if (_isPlayingYear.value) {
                    _timeOfYear.value = (_timeOfYear.value + 0.002f) % 1.0f
                }
                // Moon automatically completes a full orbit as the year/days progress,
                // but we can also let it cycle slowly on its own.
                if (_isPlayingDay.value || _isPlayingYear.value) {
                    // 1 year = 12 moon cycles approximately
                    _moonPhase.value = (_moonPhase.value + 0.0015f) % 1.0f
                }
            }
        }
    }

    // --- State setters ---
    fun setTimeOfDay(value: Float) {
        _timeOfDay.value = value.coerceIn(0f, 1f)
    }

    fun setTimeOfYear(value: Float) {
        _timeOfYear.value = value.coerceIn(0f, 1f)
    }

    fun setMoonPhase(value: Float) {
        _moonPhase.value = value.coerceIn(0f, 1f)
    }

    fun setAxialTilt(value: Float) {
        _axialTilt.value = value.coerceIn(0f, 90f)
    }

    fun togglePlayingDay() {
        _isPlayingDay.value = !_isPlayingDay.value
    }

    fun togglePlayingYear() {
        _isPlayingYear.value = !_isPlayingYear.value
    }

    fun setCameraMode(mode: CameraMode) {
        _cameraMode.value = mode
    }

    fun selectLesson(lesson: Lesson?) {
        _selectedLesson.value = lesson
        if (lesson != null) {
            _timeOfYear.value = lesson.alignmentTimeOfYear
            _timeOfDay.value = lesson.alignmentTimeOfDay
            _moonPhase.value = lesson.alignmentMoonPhase
            _cameraMode.value = lesson.cameraMode
            // Stop automatic play when aligning to a lesson so student can inspect
            _isPlayingDay.value = false
            _isPlayingYear.value = false
        }
    }

    fun updateChatInput(text: String) {
        _chatInput.value = text
    }

    // --- Gemini AI Chat Logic ---
    fun sendMessage() {
        val text = _chatInput.value.trim()
        if (text.isEmpty()) return

        // Add user message
        val currentMessages = _chatMessages.value.toMutableList()
        currentMessages.add(ChatMessage("user", text))
        _chatMessages.value = currentMessages
        _chatInput.value = ""
        _isChatLoading.value = true

        viewModelScope.launch {
            val response = try {
                // First check local answers to provide ultra-fast, robust fallback support
                val localAnswer = getLocalResponse(text)
                if (localAnswer != null) {
                    // Yield slightly for a realistic conversational rhythm
                    delay(800)
                    localAnswer
                } else {
                    // Call the actual Gemini API
                    callGeminiApi(text)
                }
            } catch (e: Exception) {
                "Oh no! It seems my stellar communicator got lost in deep space. Let me share some cosmic wisdom instead:\n\n${getFallbackExplanation(text)}"
            }

            val updatedMessages = _chatMessages.value.toMutableList()
            updatedMessages.add(ChatMessage("tutor", response))
            _chatMessages.value = updatedMessages
            _isChatLoading.value = false
        }
    }

    private suspend fun callGeminiApi(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // No API key configured - use local smart responder
            return@withContext getFallbackExplanation(prompt)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val systemInstruction = "You are Stella, a friendly and expert high school astronomy teacher. Answer questions about the Earth's orbit, rotation, axial tilt, seasons, moon phases, and eclipses in a clear, simple, and exciting way for students. Use formatting, bold headers, and bullet points. Mention how they can adjust the sliders or view modes in the 3D Cosmic Orbit simulator above to see what you are describing (e.g., say 'Try setting the camera mode to Eclipse to see the shadow in action!'). Keep replies under 3-4 short paragraphs."

        val requestBodyJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstruction)
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 500)
            })
        }

        val requestBody = requestBodyJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext getFallbackExplanation(prompt)
            }
            val responseBody = response.body?.string() ?: return@withContext getFallbackExplanation(prompt)
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val textResponse = parts.getJSONObject(0).getString("text")
            textResponse
        }
    }

    // A smart keyword-matching local response database for quick, offline educational feedback
    private fun getLocalResponse(text: String): String? {
        val query = text.lowercase()
        return when {
            query.contains("blood moon") || query.contains("red moon") -> {
                "🌘 **The Mystery of the Blood Moon (Lunar Eclipse)**\n\n" +
                        "During a total lunar eclipse, the Earth is directly aligned between the Sun and the Moon. Since Earth is much larger, it blocks all direct sunlight from reaching the Moon!\n\n" +
                        "**Why does it turn red?**\n" +
                        "This is due to **Rayleigh Scattering**—the exact same effect that makes sunsets red! Earth's thick atmosphere filters out short-wavelength blue and green light but refracts (bends) long-wavelength red light into the Earth's shadow cone. This faint red light reflects off the lunar dust, painting the Moon copper-red!\n\n" +
                        "🔭 *Try this in the simulator:* Click the **Lunar Eclipse** lesson to align the bodies, and set the camera to **Eclipse View** to inspect Earth's red shadow!"
            }
            query.contains("solar eclipse") -> {
                "☀️ **The Spectacular Solar Eclipse**\n\n" +
                        "A solar eclipse happens when the **Moon passes directly between the Sun and the Earth**, casting a dark shadow on our planet. This can only happen during a **New Moon** phase.\n\n" +
                        "**Types of Solar Eclipse:**\n" +
                        "• **Total Eclipse:** When the Moon fully blocks the Sun, revealing the Sun's glowing crown (the corona).\n" +
                        "• **Partial Eclipse:** Only a portion of the Sun is covered.\n\n" +
                        "Because the Moon is tiny, its shadow only falls on a very narrow path on Earth. You have to be in the exact path of totality to see a total eclipse!\n\n" +
                        "🔭 *Try this in the simulator:* Click the **Solar Eclipse** lesson! The Moon's orbital shadow cone will project onto the Earth's surface. Zoom in to see the shadow line!"
            }
            query.contains("lunar eclipse") -> {
                "🌕 **The Earth's Giant Shadow: Lunar Eclipse**\n\n" +
                        "A lunar eclipse happens when the **Earth aligns perfectly between the Sun and the Moon**, placing the Moon inside the Earth's shadow cone. This can only occur during a **Full Moon**.\n\n" +
                        "Because the Earth is big, a lunar eclipse can be seen from anywhere on the night-side of the globe! It lasts for several hours.\n\n" +
                        "• **Umbra:** The dark, inner part of Earth's shadow where sunlight is completely blocked.\n" +
                        "• **Penumbra:** The fuzzy outer shadow where light is only partially blocked.\n" +
                        "• When the Moon goes fully into the Umbra, it glows a beautiful reddish-copper (Blood Moon)!\n\n" +
                        "🔭 *Try this in the simulator:* Align the system using the **Lunar Eclipse** lesson to see how Earth's circular shadow swallows the Moon."
            }
            query.contains("season") || query.contains("summer") || query.contains("winter") || query.contains("axial tilt") -> {
                "☀️❄️ **What Causes the Seasons?**\n\n" +
                        "Many people think seasons happen because Earth is closer to the Sun in summer, but that is a **myth**! Seasons are caused entirely by Earth's **23.5° axial tilt** as it orbits the Sun.\n\n" +
                        "**How it works:**\n" +
                        "1. **Summer (Tilted Toward):** Whichever hemisphere is tilted *toward* the Sun gets more direct sunlight, concentrating the heat. Days are also much longer.\n" +
                        "2. **Winter (Tilted Away):** Whichever hemisphere is tilted *away* receives slanted, weaker solar rays, and days are shorter.\n" +
                        "3. **Fixed Axis:** Because Earth's tilt always points in the same direction in deep space, the Northern and Southern hemispheres have opposite seasons!\n\n" +
                        "🔭 *Try this in the simulator:* Toggle the **Month Slider**! Notice how in June, the top (North Pole) leans toward the Sun, while in December it leans away. Use the **Tilt Slider** to see what happens if Earth had a 0° tilt (no seasons at all!) or a 90° tilt (extreme seasons!)."
            }
            query.contains("day") && query.contains("night") || query.contains("spin") || query.contains("rotation") -> {
                "🌎 **The Spin of Day and Night**\n\n" +
                        "Day and night are caused by the **rotation of the Earth on its axis** every 24 hours. \n\n" +
                        "• At any moment, exactly half of the Earth is lit by the Sun (Day) while the other half is shrouded in space darkness (Night).\n" +
                        "• The dividing line is called the **terminator**.\n" +
                        "• Since the Earth spins from west to east, the Sun appears to rise in the east and set in the west.\n\n" +
                        "🔭 *Try this in the simulator:* Turn on **Play Axis Spin** and adjust the **Hour Slider**. Watch how cities rotate across the terminator line!"
            }
            query.contains("moon phase") || query.contains("phases") || query.contains("full moon") || query.contains("new moon") -> {
                "🌙 **The Lunar Dance: Moon Phases**\n\n" +
                        "The Moon takes about **29.5 days** to orbit the Earth. As it travels, the angle between the Sun, Moon, and Earth changes, which changes how much of the Moon's illuminated side we can see from Earth.\n\n" +
                        "**The Phase Sequence:**\n" +
                        "1. **New Moon:** Moon is between Sun & Earth; the dark side faces us.\n" +
                        "2. **Waxing Crescent:** A thin sliver appears.\n" +
                        "3. **First Quarter:** The Moon looks half lit (90-degree position).\n" +
                        "4. **Waxing Gibbous:** More than half lit and growing.\n" +
                        "5. **Full Moon:** Earth is between Sun & Moon; the entire lit side faces us.\n" +
                        "6. **Waning:** The lit portion shrinks back to New Moon.\n" +
                        "• *Waxing* means growing in light; *Waning* means shrinking.\n\n" +
                        "🔭 *Try this in the simulator:* Drag the **Moon Phase Slider** to watch the Moon orbit the Earth and observe the phase shift from New Moon to Full Moon."
            }
            else -> null
        }
    }

    private fun getFallbackExplanation(text: String): String {
        return getLocalResponse(text) ?: (
            "✨ **Fascinating Cosmic Wonders**\n\n" +
            "The relationship between the Sun, Earth, and Moon is a perfect dance of celestial physics:\n" +
            "• **Rotation (24 Hours):** Causes day and night as different parts of Earth face the Sun.\n" +
            "• **Revolution (365 Days):** Combined with Earth's constant 23.5° axial tilt, it creates our seasons.\n" +
            "• **Lunar Orbit (29.5 Days):** Creates the spectacular Moon phases and, when aligned perfectly, causes Eclipses!\n\n" +
            "Feel free to ask me more specific questions about **the seasons**, **solar/lunar eclipses**, **blood moons**, or **day/night cycles**! I'm here to help."
        )
    }
}
