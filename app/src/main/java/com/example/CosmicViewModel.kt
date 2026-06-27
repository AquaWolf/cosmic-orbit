package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
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
    val titleRes: Int,
    val summaryRes: Int,
    val bulletPointsRes: List<Int>,
    val alignmentTimeOfYear: Float, // 0.0 to 1.0 (0 is Jan 1st, 0.5 is early July)
    val alignmentTimeOfDay: Float,  // 0.0 to 1.0
    val alignmentMoonPhase: Float,  // 0.0 to 1.0
    val cameraMode: CameraMode,
    val explanationTextRes: Int
)

data class ChatMessage(
    val sender: String, // "user" or "tutor"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class CosmicViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("cosmic_settings", Context.MODE_PRIVATE)

    private val _customApiKey = MutableStateFlow(prefs.getString("gemini_api_key", "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _isChatAvailable = MutableStateFlow(checkChatAvailableInitial())
    val isChatAvailable: StateFlow<Boolean> = _isChatAvailable.asStateFlow()

    private fun checkChatAvailableInitial(): Boolean {
        val buildKey = BuildConfig.GEMINI_API_KEY
        val hasBuildKey = buildKey.isNotEmpty() && buildKey != "MY_GEMINI_API_KEY"
        val hasCustomKey = (prefs.getString("gemini_api_key", "") ?: "").trim().isNotEmpty()
        return hasBuildKey || hasCustomKey
    }

    fun saveCustomApiKey(key: String) {
        val trimmed = key.trim()
        _customApiKey.value = trimmed
        prefs.edit().putString("gemini_api_key", trimmed).apply()
        
        val buildKey = BuildConfig.GEMINI_API_KEY
        val hasBuildKey = buildKey.isNotEmpty() && buildKey != "MY_GEMINI_API_KEY"
        val hasCustomKey = trimmed.isNotEmpty()
        _isChatAvailable.value = hasBuildKey || hasCustomKey
    }

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
                if (java.util.Locale.getDefault().language == "de") {
                    "Hallo, Weltraumforscher! 🚀 Ich bin Stella, deine kosmische Tutorin. Stelle mir Fragen zu Erdbahn, Tag und Nacht, Jahreszeiten, Mondphasen oder spektakulären Finsternissen! Klicke auch auf die Lektionen, um die Simulation auszurichten."
                } else {
                    "Hello, space explorer! 🚀 I'm Stella, your Cosmic Tutor. Ask me any question about the Earth's orbit, day and night, the seasons, Moon phases, or spectacular eclipses! You can also click the lessons on the side to align the simulation."
                }
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

    // Pre-configured Lessons list mapped to string resource IDs for localized content rendering
    val lessonsList = listOf(
        Lesson(
            type = LessonType.DAY_NIGHT,
            titleRes = R.string.lesson_day_night_title,
            summaryRes = R.string.lesson_day_night_summary,
            bulletPointsRes = listOf(
                R.string.lesson_day_night_bullet1,
                R.string.lesson_day_night_bullet2,
                R.string.lesson_day_night_bullet3
            ),
            alignmentTimeOfYear = 0.15f,
            alignmentTimeOfDay = 0.5f,
            alignmentMoonPhase = 0.25f,
            cameraMode = CameraMode.EARTH,
            explanationTextRes = R.string.lesson_day_night_explanation
        ),
        Lesson(
            type = LessonType.SEASONS,
            titleRes = R.string.lesson_seasons_title,
            summaryRes = R.string.lesson_seasons_summary,
            bulletPointsRes = listOf(
                R.string.lesson_seasons_bullet1,
                R.string.lesson_seasons_bullet2,
                R.string.lesson_seasons_bullet3,
                R.string.lesson_seasons_bullet4
            ),
            alignmentTimeOfYear = 0.47f, // June Solstice (approximate)
            alignmentTimeOfDay = 0.5f,
            alignmentMoonPhase = 0.25f,
            cameraMode = CameraMode.SYSTEM,
            explanationTextRes = R.string.lesson_seasons_explanation
        ),
        Lesson(
            type = LessonType.MOON_PHASES,
            titleRes = R.string.lesson_moon_phases_title,
            summaryRes = R.string.lesson_moon_phases_summary,
            bulletPointsRes = listOf(
                R.string.lesson_moon_phases_bullet1,
                R.string.lesson_moon_phases_bullet2,
                R.string.lesson_moon_phases_bullet3
            ),
            alignmentTimeOfYear = 0.15f,
            alignmentTimeOfDay = 0.5f,
            alignmentMoonPhase = 0.5f, // Full Moon alignment
            cameraMode = CameraMode.SYSTEM,
            explanationTextRes = R.string.lesson_moon_phases_explanation
        ),
        Lesson(
            type = LessonType.SOLAR_ECLIPSE,
            titleRes = R.string.lesson_solar_eclipse_title,
            summaryRes = R.string.lesson_solar_eclipse_summary,
            bulletPointsRes = listOf(
                R.string.lesson_solar_eclipse_bullet1,
                R.string.lesson_solar_eclipse_bullet2,
                R.string.lesson_solar_eclipse_bullet3
            ),
            alignmentTimeOfYear = 0.15f,
            alignmentTimeOfDay = 0.45f,
            alignmentMoonPhase = 0.0f, // New Moon alignment
            cameraMode = CameraMode.ECLIPSE,
            explanationTextRes = R.string.lesson_solar_eclipse_explanation
        ),
        Lesson(
            type = LessonType.LUNAR_ECLIPSE,
            titleRes = R.string.lesson_lunar_eclipse_title,
            summaryRes = R.string.lesson_lunar_eclipse_summary,
            bulletPointsRes = listOf(
                R.string.lesson_lunar_eclipse_bullet1,
                R.string.lesson_lunar_eclipse_bullet2,
                R.string.lesson_lunar_eclipse_bullet3
            ),
            alignmentTimeOfYear = 0.15f,
            alignmentTimeOfDay = 0.5f,
            alignmentMoonPhase = 0.5f, // Full Moon alignment
            cameraMode = CameraMode.ECLIPSE,
            explanationTextRes = R.string.lesson_lunar_eclipse_explanation
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
        var apiKey = _customApiKey.value.trim()
        if (apiKey.isEmpty()) {
            apiKey = BuildConfig.GEMINI_API_KEY
        }
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // No API key configured - use local smart responder
            return@withContext getFallbackExplanation(prompt)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val systemInstruction = "You are Stella, a friendly and expert high school astronomy teacher. Answer questions about the Earth's orbit, rotation, axial tilt, seasons, moon phases, and eclipses in a clear, simple, and exciting way for students. Use formatting, bold headers, and bullet points. Mention how they can adjust the sliders or view modes in the 3D Cosmic Orbit simulator above to see what you are describing (e.g., say 'Try setting the camera mode to Eclipse to see the shadow in action!'). Keep replies under 3-4 short paragraphs. Crucial: Always reply in German if the user's prompt is in German. If the prompt is in English, reply in English."

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

    // A smart keyword-matching local response database for quick, offline educational feedback supporting English and German
    private fun getLocalResponse(text: String): String? {
        val query = text.lowercase()
        val isDe = java.util.Locale.getDefault().language == "de" ||
                query.contains("mond") || query.contains("erde") || query.contains("sonne") ||
                query.contains("jahr") || query.contains("finsternis") || query.contains("achse") ||
                query.contains("drehung") || query.contains("zeit")

        if (isDe) {
            return when {
                query.contains("blutmond") || query.contains("roter mond") || query.contains("blut") -> {
                    "🌘 **Das Geheimnis des Blutmonds (Mondfinsternis)**\n\n" +
                            "Bei einer totalen Mondfinsternis steht die Erde direkt zwischen Sonne und Mond. Da die Erde viel größer ist, blockiert sie das direkte Sonnenlicht komplett!\n\n" +
                            "**Warum wird er rot?**\n" +
                            "Das liegt an der **Rayleigh-Streuung** – genau derselbe Effekt, der auch Sonnenuntergänge rot färbt! Die dichte Erdatmosphäre filtert das kurzwellige blaue und grüne Licht heraus, bricht (biegt) aber das langwellige rote Licht in den Schattenkegel der Erde. Dieses schwache rote Licht wird vom Mondstaub reflektiert und färbt den Mond kupferrot!\n\n" +
                            "🔭 *Probiere es im Simulator aus:* Klicke auf die Lektion **Mondfinsternis**, um die Himmelskörper auszurichten, und stelle die Kamera auf die **Finsternis-Ansicht**, um den roten Erdschatten zu sehen!"
                }
                query.contains("sonnenfinsternis") -> {
                    "☀️ **Die spektakuläre Sonnenfinsternis**\n\n" +
                            "Eine Sonnenfinsternis ereignet sich, wenn sich der **Mond direkt zwischen Sonne und Erde schiebt** und einen Schatten auf unseren Planeten wirft. Dies kann nur während der **Neumondphase** geschehen.\n\n" +
                            "**Arten von Sonnenfinsternissen:**\n" +
                            "• **Totale Finsternis:** Der Mond verdeckt die Sonne vollständig, wodurch die glühende Korona sichtbar wird.\n" +
                            "• **Partielle Finsternis:** Nur ein Teil der Sonne wird verdeckt.\n\n" +
                            "Da der Mond relativ klein ist, fällt sein Kernschatten nur auf einen sehr schmalen Pfad auf der Erde. Man muss sich genau auf diesem Pfad befinden, um eine totale Finsternis zu erleben!\n\n" +
                            "🔭 *Probiere es im Simulator aus:* Klicke auf die Lektion **Sonnenfinsternis**! Der Schattenkegel des Mondes wird auf die Erde projiziert. Zoome heran, um den Schatten zu sehen!"
                }
                query.contains("mondfinsternis") -> {
                    "🌕 **Der riesige Erdschatten: Mondfinsternis**\n\n" +
                            "Eine Mondfinsternis entsteht, wenn die **Erde perfekt zwischen Sonne und Mond steht** und den Mond in ihren Schattenkegel hüllt. Dies kann nur bei **Vollmond** passieren.\n\n" +
                            "Da die Erde sehr groß ist, kann eine Mondfinsternis von überall auf der Nachtseite der Erde aus beobachtet werden! Sie dauert meist mehrere Stunden.\n\n" +
                            "• **Kernschatten (Umbra):** Der dunkle, innere Teil des Erdschattens, in dem das Sonnenlicht vollständig blockiert ist.\n" +
                            "• **Halbschatten (Penumbra):** Der hellere äußere Schatten, in dem das Licht nur teilweise blockiert ist.\n" +
                            "• Wenn der Mond vollständig in den Kernschatten eintritt, leuchtet er wunderschön rötlich-kupferfarben (Blutmond)!\n\n" +
                            "🔭 *Probiere es im Simulator aus:* Richte das System über die Lektion **Mondfinsternis** aus, um zu sehen, wie der kreisförmige Erdschatten den Mond verschluckt."
                }
                query.contains("jahreszeit") || query.contains("sommer") || query.contains("winter") || query.contains("neigung") || query.contains("achse") -> {
                    "☀️❄️ **Was verursacht die Jahreszeiten?**\n\n" +
                            "Viele Menschen glauben, dass Jahreszeiten entstehen, weil die Erde im Sommer näher an der Sonne ist – das ist jedoch ein **Mythos**! Jahreszeiten entstehen ausschließlich durch die **Achsenneigung der Erde von 23,5°** auf ihrer Umlaufbahn um die Sonne.\n\n" +
                            "**Wie es funktioniert:**\n" +
                            "1. **Sommer (Zugegnete Halbkugel):** Die Halbkugel, die zur Sonne geneigt ist, erhält direkteres, konzentriertes Sonnenlicht. Die Tage sind auch viel länger.\n" +
                            "2. **Winter (Weggeneigte Halbkugel):** Die weggeneigte Halbkugel erhält flachere, schwächere Sonnenstrahlen, und die Tage sind kürzer.\n" +
                            "3. **Feste Achse:** Da die Erdachse im Weltraum immer in die gleiche Richtung zeigt, haben Nord- und Südhalbkugel entgegengesetzte Jahreszeiten!\n\n" +
                            "🔭 *Probiere es im Simulator aus:* Bewege den **Monats-Regler**! Beachte, wie im Juni der Nordpol zur Sonne zeigt, im Dezember jedoch weggeneigt ist. Verwende den **Neigungs-Regler**, um zu sehen, was bei einer Achsenneigung von 0° (keine Jahreszeiten) oder 90° (extreme Jahreszeiten) passieren würde!"
                }
                query.contains("tag") || query.contains("nacht") || query.contains("drehung") || query.contains("rotation") || query.contains("spin") -> {
                    "🌎 **Die Drehung von Tag und Nacht**\n\n" +
                            "Tag und Nacht entstehen durch die **Drehung der Erde um ihre eigene Achse** alle 24 Stunden.\n\n" +
                            "• Zu jedem Zeitpunkt ist genau eine Hälfte der Erde von der Sonne beleuchtet (Tag), während die andere Hälfte im Dunkeln liegt (Nacht).\n" +
                            "• Die Trennlinie wird als **Terminator** bezeichnet.\n" +
                            "• Da sich die Erde von Westen nach Osten dreht, scheint die Sonne im Osten aufzugehen und im Westen unterzugehen.\n\n" +
                            "🔭 *Probiere es im Simulator aus:* Schalte **Start 24H** ein und verändere den **Uhrzeit-Regler**. Sieh zu, wie sich Kontinente über den Terminator bewegen!"
                }
                query.contains("phase") || query.contains("mondphase") || query.contains("vollmond") || query.contains("neumond") -> {
                    "🌙 **Der Tanz des Mondes: Mondphasen**\n\n" +
                            "Der Mond benötigt etwa **29.5 Tage**, um die Erde einmal zu umkreisen. Währenddessen ändert sich der Winkel zwischen Sonne, Mond und Erde, wodurch sich der von der Erde aus sichtbare beleuchtete Teil verändert.\n\n" +
                            "**Die Phasen-Reihenfolge:**\n" +
                            "1. **Neumond:** Der Mond steht zwischen Sonne und Erde; die dunkle Seite zeigt zu uns.\n" +
                            "2. **Zunehmende Sichel:** Eine feine Sichel wird sichtbar.\n" +
                            "3. **Erstes Viertel:** Der Mond erscheint halb beleuchtet.\n" +
                            "4. **Zunehmender Dreiviertelmond:** Mehr als die Hälfte ist beleuchtet und wächst.\n" +
                            "5. **Vollmond:** Die Erde steht zwischen Sonne und Mond; die gesamte beleuchtete Seite zeigt zu uns.\n" +
                            "6. **Abnehmend:** Der beleuchtete Teil schrumpft wieder zurück zum Neumond.\n\n" +
                            "🔭 *Probiere es im Simulator aus:* Ziehe am **Mondphasen-Regler**, um den Mond um die Erde kreisen zu lassen und den Phasenübergang zu beobachten."
                }
                else -> null
            }
        } else {
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
    }

    private fun getFallbackExplanation(text: String): String {
        val query = text.lowercase()
        val isDe = java.util.Locale.getDefault().language == "de" ||
                query.contains("mond") || query.contains("erde") || query.contains("sonne") ||
                query.contains("jahr") || query.contains("finsternis") || query.contains("achse") ||
                query.contains("drehung") || query.contains("zeit")

        return getLocalResponse(text) ?: if (isDe) {
            "✨ **Faszinierende kosmische Wunder**\n\n" +
                    "Die Beziehung zwischen Sonne, Erde und Mond ist ein perfektes Zusammenspiel der Himmelsphysik:\n" +
                    "• **Erdrotation (24 Stunden):** Verursacht Tag und Nacht, da sich verschiedene Teile der Erde zur Sonne drehen.\n" +
                    "• **Umlaufbahn um die Sonne (365 Tage):** Zusammen mit der konstanten Achsenneigung von 23,5° entstehen so unsere Jahreszeiten.\n" +
                    "• **Mondumlaufbahn (29,5 Tage):** Erzeugt die verschiedenen Mondphasen und bei perfekter Ausrichtung spektakuläre Finsternisse!\n\n" +
                    "Frage mich gerne nach bestimmten Themen wie **Jahreszeiten**, **Sonnen-/Mondfinsternisse**, **Blutmonde** oder **Tag- und Nachtzyklen**! Ich helfe dir gerne."
        } else {
            "✨ **Fascinating Cosmic Wonders**\n\n" +
                    "The relationship between the Sun, Earth, and Moon is a perfect dance of celestial physics:\n" +
                    "• **Rotation (24 Hours):** Causes day and night as different parts of Earth face the Sun.\n" +
                    "• **Revolution (365 Days):** Combined with Earth's constant 23.5° axial tilt, it creates our seasons.\n" +
                    "• **Lunar Orbit (29.5 Days):** Creates the spectacular Moon phases and, when aligned perfectly, causes Eclipses!\n\n" +
                    "Feel free to ask me more specific questions about **the seasons**, **solar/lunar eclipses**, **blood moons**, or **day/night cycles**! I'm here to help."
        }
    }
}
