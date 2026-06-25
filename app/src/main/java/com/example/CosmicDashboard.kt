package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import com.example.ui.theme.*
import kotlinx.coroutines.launch

// Dynamic grid background utility matching Clean Minimalism specs
fun Modifier.dotGridBackground(
    color: Color = Color.White,
    opacity: Float = 0.07f,
    dotSize: Float = 1.8f,
    spacing: Float = 36f
): Modifier = this.drawBehind {
    val sizeX = size.width
    val sizeY = size.height
    val numX = (sizeX / spacing).toInt() + 1
    val numY = (sizeY / spacing).toInt() + 1
    for (i in 0..numX) {
        for (j in 0..numY) {
            drawCircle(
                color = color.copy(alpha = opacity),
                radius = dotSize,
                center = Offset(i * spacing, j * spacing)
            )
        }
    }
}

@Composable
fun CosmicDashboard(
    viewModel: CosmicViewModel,
    modifier: Modifier = Modifier
) {
    val timeOfDay by viewModel.timeOfDay.collectAsState()
    val timeOfYear by viewModel.timeOfYear.collectAsState()
    val moonPhase by viewModel.moonPhase.collectAsState()
    val axialTilt by viewModel.axialTilt.collectAsState()
    val isPlayingDay by viewModel.isPlayingDay.collectAsState()
    val isPlayingYear by viewModel.isPlayingYear.collectAsState()
    val cameraMode by viewModel.cameraMode.collectAsState()
    val selectedLesson by viewModel.selectedLesson.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val chatInput by viewModel.chatInput.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()

    val configuration = LocalConfiguration.current
    val isExpanded = configuration.screenWidthDp >= 750

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBlack),
        containerColor = SpaceBlack,
        topBar = {
            // Minimalist Space Header inspired by Clean Minimalism theme
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpaceDarkIndigo)
                    .dotGridBackground(opacity = 0.04f)
                    .padding(horizontal = 24.dp, vertical = 18.dp)
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "EDUCATION MODE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentTeal,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "OrbitEdu",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SpaceNavy,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.updateChatInput("What is OrbitEdu?")
                                viewModel.sendMessage()
                            },
                            modifier = Modifier.testTag("btn_header_settings")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Quick Query",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (isExpanded) {
            // --- EXPANDED TABLET/DESKTOP DUAL COLUMN LAYOUT ---
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(SpaceBlack)
                    .dotGridBackground(opacity = 0.06f)
            ) {
                // Left Column: Interactive 3D Canvas + Sliders (60% width)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1.2f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Simulation Viewport Card
                    SimulatorCard(
                        timeOfDay = timeOfDay,
                        timeOfYear = timeOfYear,
                        moonPhase = moonPhase,
                        axialTilt = axialTilt,
                        cameraMode = cameraMode,
                        viewModel = viewModel
                    )

                    // 2. Main Control Deck Sliders
                    ControlDeckCard(
                        timeOfDay = timeOfDay,
                        timeOfYear = timeOfYear,
                        moonPhase = moonPhase,
                        axialTilt = axialTilt,
                        isPlayingDay = isPlayingDay,
                        isPlayingYear = isPlayingYear,
                        viewModel = viewModel
                    )
                }

                // Right Column: Lessons, active description and Stella AI Chat
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1.0f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Quick Align Lessons List
                    LessonsCard(
                        lessons = viewModel.lessonsList,
                        selectedLesson = selectedLesson,
                        onLessonSelect = { viewModel.selectLesson(it) }
                    )

                    // 2. Active Lesson Explanation Panel
                    AnimatedVisibility(
                        visible = selectedLesson != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        selectedLesson?.let { lesson ->
                            ActiveLessonExplanationPanel(lesson = lesson) {
                                viewModel.selectLesson(null)
                            }
                        }
                    }

                    // 3. Smart Cosmic Tutor Stella AI Chat
                    CosmicTutorChatCard(
                        messages = chatMessages,
                        chatInput = chatInput,
                        isLoading = isChatLoading,
                        onInputChange = { viewModel.updateChatInput(it) },
                        onSend = { viewModel.sendMessage() }
                    )
                }
            }
        } else {
            // --- COMPACT MOBILE PORTRAIT VERTICAL SCROLL LAYOUT ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(SpaceBlack)
                    .dotGridBackground(opacity = 0.06f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Simulator Viewport
                SimulatorCard(
                    timeOfDay = timeOfDay,
                    timeOfYear = timeOfYear,
                    moonPhase = moonPhase,
                    axialTilt = axialTilt,
                    cameraMode = cameraMode,
                    viewModel = viewModel
                )

                // 2. Controls & Sliders
                ControlDeckCard(
                    timeOfDay = timeOfDay,
                    timeOfYear = timeOfYear,
                    moonPhase = moonPhase,
                    axialTilt = axialTilt,
                    isPlayingDay = isPlayingDay,
                    isPlayingYear = isPlayingYear,
                    viewModel = viewModel
                )

                // 3. Align Lessons
                LessonsCard(
                    lessons = viewModel.lessonsList,
                    selectedLesson = selectedLesson,
                    onLessonSelect = { viewModel.selectLesson(it) }
                )

                // 4. Active Lesson Explanation
                AnimatedVisibility(
                    visible = selectedLesson != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    selectedLesson?.let { lesson ->
                        ActiveLessonExplanationPanel(lesson = lesson) {
                            viewModel.selectLesson(null)
                        }
                    }
                }

                // 5. Ask Stella AI Chat
                CosmicTutorChatCard(
                    messages = chatMessages,
                    chatInput = chatInput,
                    isLoading = isChatLoading,
                    onInputChange = { viewModel.updateChatInput(it) },
                    onSend = { viewModel.sendMessage() }
                )
            }
        }
    }
}

// --- SUB-COMPONENTS ---

@Composable
fun SimulatorCard(
    timeOfDay: Float,
    timeOfYear: Float,
    moonPhase: Float,
    axialTilt: Float,
    cameraMode: CameraMode,
    viewModel: CosmicViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            .testTag("simulator_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceDarkIndigo),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Interactive custom physics simulator canvas
            CosmicSimulator(
                timeOfDay = timeOfDay,
                timeOfYear = timeOfYear,
                moonPhase = moonPhase,
                axialTilt = axialTilt,
                cameraMode = cameraMode,
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic Current Celestial Event floating box (from Clean Minimalism design specs)
            val monthIndex = (timeOfYear * 12).toInt() % 12
            val eventTitle: String
            val eventDesc: String
            when (monthIndex) {
                11, 0, 1 -> {
                    eventTitle = "Winter Solstice"
                    eventDesc = "Max polar axial tilt away from Sun"
                }
                2, 3, 4 -> {
                    eventTitle = "Vernal Equinox"
                    eventDesc = "Day & Night equal globally"
                }
                5, 6, 7 -> {
                    eventTitle = "Summer Solstice"
                    eventDesc = "Max polar axial tilt towards Sun"
                }
                else -> {
                    eventTitle = "Autumnal Equinox"
                    eventDesc = "Day & Night equal globally"
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp),
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "CURRENT EVENT",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = eventTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = eventDesc,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // Dynamic camera mode perspective selector buttons
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                color = SpaceBlack.copy(alpha = 0.7f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Row(modifier = Modifier.padding(2.dp)) {
                    CameraModeButton(
                        label = "🌌 System",
                        selected = cameraMode == CameraMode.SYSTEM,
                        testTag = "btn_system_view"
                    ) {
                        viewModel.setCameraMode(CameraMode.SYSTEM)
                    }
                    CameraModeButton(
                        label = "🌍 Close-Up",
                        selected = cameraMode == CameraMode.EARTH,
                        testTag = "btn_earth_view"
                    ) {
                        viewModel.setCameraMode(CameraMode.EARTH)
                    }
                    CameraModeButton(
                        label = "🌒 Eclipse",
                        selected = cameraMode == CameraMode.ECLIPSE,
                        testTag = "btn_eclipse_view"
                    ) {
                        viewModel.setCameraMode(CameraMode.ECLIPSE)
                    }
                }
            }

            // Small watermark displaying instructions
            Text(
                text = "Drag sliders below to control orbit and spin rotation",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.35f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(10.dp)
            )
        }
    }
}

@Composable
fun CameraModeButton(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) AccentTeal else Color.Transparent,
            contentColor = if (selected) SpaceBlack else Color.White
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .height(32.dp)
            .testTag(testTag)
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ControlDeckCard(
    timeOfDay: Float,
    timeOfYear: Float,
    moonPhase: Float,
    axialTilt: Float,
    isPlayingDay: Boolean,
    isPlayingYear: Boolean,
    viewModel: CosmicViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("control_deck_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceDarkIndigo),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "TIME SIMULATION",
                color = AccentTeal,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp
            )

            // Slider 1: Earth Axis Spin (Hour of the day)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val hour = (timeOfDay * 24).toInt()
                    val amPm = if (hour < 12) "AM" else "PM"
                    val displayHour = if (hour == 0 || hour == 12) 12 else hour % 12
                    Text(
                        text = "🌎 Spin: $displayHour:00 $amPm",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Button(
                        onClick = { viewModel.togglePlayingDay() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlayingDay) AccentTeal.copy(alpha = 0.15f) else AccentTeal,
                            contentColor = if (isPlayingDay) AccentTeal else SpaceBlack
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("btn_toggle_day_play")
                    ) {
                        Text(
                            text = if (isPlayingDay) "⏸ Pause" else "▶ Play 24H",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Slider(
                    value = timeOfDay,
                    onValueChange = { viewModel.setTimeOfDay(it) },
                    colors = SliderDefaults.colors(
                        thumbColor = AccentTeal,
                        activeTrackColor = AccentTeal,
                        inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("slider_time_of_day")
                )
            }

            // Slider 2: Orbit around the Sun (Seasons & Month)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val months = listOf(
                        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                    )
                    val monthIndex = (timeOfYear * 12).toInt() % 12
                    Text(
                        text = "📅 Orbit Month: ${months[monthIndex]}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Button(
                        onClick = { viewModel.togglePlayingYear() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlayingYear) SunYellow.copy(alpha = 0.15f) else SunYellow,
                            contentColor = if (isPlayingYear) SunYellow else SpaceBlack
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("btn_toggle_year_play")
                    ) {
                        Text(
                            text = if (isPlayingYear) "⏸ Pause" else "▶ Play Year",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Slider(
                    value = timeOfYear,
                    onValueChange = { viewModel.setTimeOfYear(it) },
                    colors = SliderDefaults.colors(
                        thumbColor = SunYellow,
                        activeTrackColor = SunYellow,
                        inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("slider_time_of_year")
                )
            }

            // Bottom dual controls: Moon orbit + Earth axial tilt degrees
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Moon Phase slider
                Column(modifier = Modifier.weight(1f)) {
                    val phaseName = when {
                        moonPhase < 0.05f || moonPhase > 0.95f -> "New 🌑"
                        moonPhase in 0.22f..0.28f -> "1st Qtr 🌓"
                        moonPhase in 0.45f..0.55f -> "Full 🌕"
                        moonPhase in 0.72f..0.78f -> "3rd Qtr 🌗"
                        moonPhase < 0.25f -> "Waxing 🌒"
                        moonPhase < 0.5f -> "Gibbous 🌔"
                        moonPhase < 0.75f -> "Waning 🌖"
                        else -> "Crescent 🌘"
                    }
                    Text(
                        text = "🌙 Moon: $phaseName",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = moonPhase,
                        onValueChange = { viewModel.setMoonPhase(it) },
                        colors = SliderDefaults.colors(
                            thumbColor = MoonGrey,
                            activeTrackColor = MoonGrey,
                            inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.testTag("slider_moon_phase")
                    )
                }

                // Axial Tilt Slider
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📐 Axis Tilt: ${axialTilt.toInt()}°",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = axialTilt,
                        onValueChange = { viewModel.setAxialTilt(it) },
                        valueRange = 0f..90f,
                        colors = SliderDefaults.colors(
                            thumbColor = SunOrange,
                            activeTrackColor = SunOrange,
                            inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.testTag("slider_axial_tilt")
                    )
                    Text(
                        text = "Earth actual: 23.5°",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun LessonsCard(
    lessons: List<Lesson>,
    selectedLesson: Lesson?,
    onLessonSelect: (Lesson) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lessons_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceDarkIndigo),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "INTERACTIVE STUDY LAB",
                color = AccentTeal,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "Tap a topic below to auto-align the planets and unlock custom simulations:",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                lessons.forEach { lesson ->
                    val isSelected = selectedLesson?.type == lesson.type
                    Button(
                        onClick = { onLessonSelect(lesson) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) AccentTeal else Color.White.copy(alpha = 0.05f),
                            contentColor = if (isSelected) SpaceBlack else Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) AccentTeal else Color.White.copy(alpha = 0.1f)
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("btn_lesson_${lesson.type.name.lowercase()}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val emoji = when (lesson.type) {
                                LessonType.DAY_NIGHT -> "🌎"
                                LessonType.SEASONS -> "☀️"
                                LessonType.MOON_PHASES -> "🌙"
                                LessonType.SOLAR_ECLIPSE -> "🌑"
                                LessonType.LUNAR_ECLIPSE -> "🔴"
                            }
                            Text(text = emoji, fontSize = 14.sp)
                            Text(
                                text = lesson.title,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveLessonExplanationPanel(
    lesson: Lesson,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_lesson_panel"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
        border = BorderStroke(1.dp, AccentTeal.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE STUDY: ${lesson.title.uppercase()}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentTeal,
                    letterSpacing = 1.sp
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("btn_close_active_lesson")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = lesson.summary,
                fontSize = 13.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bullet list
            lesson.bulletPoints.forEach { point ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(text = "✦ ", color = SunYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = point,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = SpaceBlack.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Text(
                    text = lesson.explanationText,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

@Composable
fun CosmicTutorChatCard(
    messages: List<ChatMessage>,
    chatInput: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Automatically scroll to the very bottom of the chat list on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .testTag("chat_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceDarkIndigo),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Chat Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpaceNavy)
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🛰️ ", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "STELLA ORBIT GUIDE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = AccentTeal,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Real-time AI astrophysics support",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            // Message Board
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { message ->
                        ChatBubble(message = message)
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SpaceBlack.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                            border = BorderStroke(1.dp, AccentTeal.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = AccentTeal,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Stella is tracing light rays...",
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Input Control Deck
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpaceNavy)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = chatInput,
                    onValueChange = onInputChange,
                    placeholder = {
                        Text(
                            text = "Ask about eclipses, seasons, gravity...",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SpaceBlack,
                        unfocusedContainerColor = SpaceBlack,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = AccentTeal,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("chat_input_text"),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        onSend()
                        focusManager.clearFocus()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = SpaceBlack),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("chat_send_button")
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send Message", tint = SpaceBlack, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) AccentTeal.copy(alpha = 0.12f) else SpaceCardBg
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (isUser) AccentTeal.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    fontSize = 12.sp,
                    color = Color.White,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
