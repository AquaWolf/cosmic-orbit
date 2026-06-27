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
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

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
    val isChatAvailable by viewModel.isChatAvailable.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }

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
                    .statusBarsPadding()
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
                            text = stringResource(R.string.education_mode),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentTeal,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.orbit_edu),
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
                                showSettingsDialog = true
                            },
                            modifier = Modifier.testTag("btn_header_settings")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.settings_title),
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
                        isChatAvailable = isChatAvailable,
                        onInputChange = { viewModel.updateChatInput(it) },
                        onSend = { viewModel.sendMessage() },
                        onConfigureClick = { showSettingsDialog = true }
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
                    isChatAvailable = isChatAvailable,
                    onInputChange = { viewModel.updateChatInput(it) },
                    onSend = { viewModel.sendMessage() },
                    onConfigureClick = { showSettingsDialog = true }
                )
            }
        }
    }

    if (showSettingsDialog) {
        CosmicApiSettingsDialog(
            initialKey = customApiKey,
            isChatAvailable = isChatAvailable,
            onDismiss = { showSettingsDialog = false },
            onSave = { key ->
                viewModel.saveCustomApiKey(key)
                showSettingsDialog = false
            }
        )
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
                    eventTitle = stringResource(R.string.winter_solstice_title)
                    eventDesc = stringResource(R.string.winter_solstice_desc)
                }
                2, 3, 4 -> {
                    eventTitle = stringResource(R.string.vernal_equinox_title)
                    eventDesc = stringResource(R.string.vernal_equinox_desc)
                }
                5, 6, 7 -> {
                    eventTitle = stringResource(R.string.summer_solstice_title)
                    eventDesc = stringResource(R.string.summer_solstice_desc)
                }
                else -> {
                    eventTitle = stringResource(R.string.autumnal_equinox_title)
                    eventDesc = stringResource(R.string.autumnal_equinox_desc)
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
                        text = stringResource(R.string.current_event_label),
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
                        label = stringResource(R.string.camera_mode_system),
                        selected = cameraMode == CameraMode.SYSTEM,
                        testTag = "btn_system_view"
                    ) {
                        viewModel.setCameraMode(CameraMode.SYSTEM)
                    }
                    CameraModeButton(
                        label = stringResource(R.string.camera_mode_earth),
                        selected = cameraMode == CameraMode.EARTH,
                        testTag = "btn_earth_view"
                    ) {
                        viewModel.setCameraMode(CameraMode.EARTH)
                    }
                    CameraModeButton(
                        label = stringResource(R.string.camera_mode_eclipse),
                        selected = cameraMode == CameraMode.ECLIPSE,
                        testTag = "btn_eclipse_view"
                    ) {
                        viewModel.setCameraMode(CameraMode.ECLIPSE)
                    }
                }
            }

            // Small watermark displaying instructions
            Text(
                text = stringResource(R.string.drag_sliders_hint),
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
                text = stringResource(R.string.time_simulation_label),
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
                    val isDe = java.util.Locale.getDefault().language == "de"
                    val spinText = if (isDe) {
                        stringResource(R.string.slider_spin_format_de, hour)
                    } else {
                        val amPm = if (hour < 12) "AM" else "PM"
                        val displayHour = if (hour == 0 || hour == 12) 12 else hour % 12
                        stringResource(R.string.slider_spin_format, displayHour, amPm)
                    }
                    Text(
                        text = spinText,
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
                            text = if (isPlayingDay) stringResource(R.string.btn_pause) else stringResource(R.string.btn_play_24h),
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
                        stringResource(R.string.month_jan),
                        stringResource(R.string.month_feb),
                        stringResource(R.string.month_mar),
                        stringResource(R.string.month_apr),
                        stringResource(R.string.month_may),
                        stringResource(R.string.month_jun),
                        stringResource(R.string.month_jul),
                        stringResource(R.string.month_aug),
                        stringResource(R.string.month_sep),
                        stringResource(R.string.month_oct),
                        stringResource(R.string.month_nov),
                        stringResource(R.string.month_dec)
                    )
                    val monthIndex = (timeOfYear * 12).toInt() % 12
                    Text(
                        text = stringResource(R.string.orbit_month_format, months[monthIndex]),
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
                            text = if (isPlayingYear) stringResource(R.string.btn_pause) else stringResource(R.string.btn_play_year),
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
                        moonPhase < 0.05f || moonPhase > 0.95f -> stringResource(R.string.moon_phase_new)
                        moonPhase in 0.22f..0.28f -> stringResource(R.string.moon_phase_1st_qtr)
                        moonPhase in 0.45f..0.55f -> stringResource(R.string.moon_phase_full)
                        moonPhase in 0.72f..0.78f -> stringResource(R.string.moon_phase_3rd_qtr)
                        moonPhase < 0.25f -> stringResource(R.string.moon_phase_waxing)
                        moonPhase < 0.5f -> stringResource(R.string.moon_phase_gibbous)
                        moonPhase < 0.75f -> stringResource(R.string.moon_phase_waning)
                        else -> stringResource(R.string.moon_phase_crescent)
                    }
                    Text(
                        text = stringResource(R.string.moon_format, phaseName),
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
                        text = stringResource(R.string.axis_tilt_format, axialTilt.toInt()),
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
                        text = stringResource(R.string.earth_actual_hint),
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
                text = stringResource(R.string.interactive_study_lab),
                color = AccentTeal,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp
            )
            Text(
                text = stringResource(R.string.study_lab_hint),
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
                                text = stringResource(lesson.titleRes),
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
                    text = stringResource(R.string.active_study_label, stringResource(lesson.titleRes).uppercase()),
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
                text = stringResource(lesson.summaryRes),
                fontSize = 13.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bullet list
            lesson.bulletPointsRes.forEach { pointRes ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(text = "✦ ", color = SunYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = stringResource(pointRes),
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
                    text = stringResource(lesson.explanationTextRes),
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
    isChatAvailable: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onConfigureClick: () -> Unit
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.stella_orbit_guide),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = AccentTeal,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = stringResource(R.string.stella_sub_title),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                // Status Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isChatAvailable) Color(0xFF1B5E20).copy(alpha = 0.2f) else Color(0xFFE65100).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (isChatAvailable) Color(0xFF81C784).copy(alpha = 0.3f) else Color(0xFFFFB74D).copy(alpha = 0.3f)),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (isChatAvailable) Color(0xFF4CAF50) else Color(0xFFFF9800), shape = RoundedCornerShape(50))
                        )
                        Text(
                            text = if (isChatAvailable) "AI ONLINE" else "OFFLINE MODE",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isChatAvailable) Color(0xFF81C784) else Color(0xFFFFB74D)
                        )
                    }
                }

                // Chat Settings Gear Button
                IconButton(
                    onClick = onConfigureClick,
                    modifier = Modifier.size(28.dp).testTag("btn_chat_settings")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configure API Key",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
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
                                    text = stringResource(R.string.stella_loading_hint),
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
                            text = stringResource(R.string.stella_chat_placeholder),
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
                    Icon(imageVector = Icons.Default.Send, contentDescription = stringResource(R.string.send), tint = SpaceBlack, modifier = Modifier.size(18.dp))
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

@Composable
fun CosmicApiSettingsDialog(
    initialKey: String,
    isChatAvailable: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val context = LocalContext.current
    var keyText by remember { mutableStateOf(initialKey) }
    var keyVisible by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceDarkIndigo),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("settings_api_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(SpaceNavy, shape = RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = AccentTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.settings_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Configure custom API connection",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                // Subtitle
                Text(
                    text = stringResource(R.string.settings_subtitle),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    lineHeight = 18.sp
                )

                // Current API Status Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isChatAvailable) Color(0xFF1B5E20).copy(alpha = 0.15f) else Color(0xFFE65100).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (isChatAvailable) Color(0xFF81C784).copy(alpha = 0.25f) else Color(0xFFFFB74D).copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isChatAvailable) Color(0xFF4CAF50) else Color(0xFFFF9800), shape = RoundedCornerShape(50))
                        )
                        Text(
                            text = if (isChatAvailable) stringResource(R.string.api_status_configured) else stringResource(R.string.api_status_missing),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isChatAvailable) Color(0xFF81C784) else Color(0xFFFFB74D)
                        )
                    }
                }

                // Field Label
                Text(
                    text = stringResource(R.string.api_key_label),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentTeal
                )

                // Text field
                TextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.api_key_placeholder),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    },
                    visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(
                            onClick = { keyVisible = !keyVisible },
                            colors = ButtonDefaults.textButtonColors(contentColor = AccentTeal)
                        ) {
                            Text(
                                text = if (keyVisible) "HIDE" else "SHOW",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
                        .fillMaxWidth()
                        .testTag("api_key_input_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                // AI Studio link hints
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.get_key_hint),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "https://aistudio.google.com/",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SunYellow,
                        modifier = Modifier
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback
                                }
                            }
                            .testTag("get_api_key_link_text")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions Buttons (Cancel, Clear, Save)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.6f))
                    ) {
                        Text(text = stringResource(R.string.cancel), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (keyText.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                keyText = ""
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF5350))
                        ) {
                            Text(text = stringResource(R.string.clear), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = {
                            onSave(keyText)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = SpaceBlack),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = stringResource(R.string.save), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
