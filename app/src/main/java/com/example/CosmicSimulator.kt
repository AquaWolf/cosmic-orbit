package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.*

data class GeoPoint(val lat: Float, val lon: Float)

object ContinentData {
    // Highly-optimized simplified geographic polygon datasets representing continents
    val continents = listOf(
        // North America
        listOf(
            GeoPoint(70f, -160f), GeoPoint(70f, -60f), GeoPoint(55f, -50f),
            GeoPoint(45f, -65f), GeoPoint(25f, -80f), GeoPoint(15f, -90f),
            GeoPoint(18f, -100f), GeoPoint(32f, -115f), GeoPoint(48f, -125f),
            GeoPoint(60f, -140f), GeoPoint(65f, -165f)
        ),
        // South America
        listOf(
            GeoPoint(12f, -72f), GeoPoint(8f, -50f), GeoPoint(-5f, -37f),
            GeoPoint(-20f, -40f), GeoPoint(-35f, -55f), GeoPoint(-55f, -68f),
            GeoPoint(-50f, -75f), GeoPoint(-35f, -72f), GeoPoint(-15f, -75f),
            GeoPoint(-3f, -80f)
        ),
        // Africa
        listOf(
            GeoPoint(37f, 10f), GeoPoint(32f, 32f), GeoPoint(30f, 34f),
            GeoPoint(12f, 51f), GeoPoint(-18f, 40f), GeoPoint(-34f, 20f),
            GeoPoint(-33f, 18f), GeoPoint(-15f, 12f), GeoPoint(5f, 10f),
            GeoPoint(15f, -17f), GeoPoint(32f, -13f)
        ),
        // Eurasia (Europe + Asia)
        listOf(
            GeoPoint(71f, 25f), GeoPoint(75f, 60f), GeoPoint(73f, 100f),
            GeoPoint(70f, 160f), GeoPoint(55f, 160f), GeoPoint(38f, 142f),
            GeoPoint(22f, 120f), GeoPoint(8f, 105f), GeoPoint(20f, 95f),
            GeoPoint(10f, 80f), GeoPoint(25f, 65f), GeoPoint(12f, 44f),
            GeoPoint(30f, 32f), GeoPoint(41f, 29f), GeoPoint(36f, -9f),
            GeoPoint(50f, -10f), GeoPoint(60f, 5f)
        ),
        // Australia
        listOf(
            GeoPoint(-22f, 114f), GeoPoint(-12f, 136f), GeoPoint(-10f, 142f),
            GeoPoint(-25f, 153f), GeoPoint(-38f, 145f), GeoPoint(-35f, 115f)
        ),
        // Greenland
        listOf(
            GeoPoint(78f, -68f), GeoPoint(83f, -40f), GeoPoint(70f, -22f),
            GeoPoint(60f, -45f), GeoPoint(70f, -60f)
        )
    )
}

@Composable
fun CosmicSimulator(
    timeOfDay: Float,     // 0.0 to 1.0 (rotation around axis)
    timeOfYear: Float,    // 0.0 to 1.0 (orbit around Sun)
    moonPhase: Float,     // 0.0 to 1.0 (orbit around Earth)
    axialTilt: Float,     // degrees (usually 23.5)
    cameraMode: CameraMode,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    // Deterministic starry background coordinates
    val stars = remember {
        List(100) {
            val rx = kotlin.random.Random.nextFloat()
            val ry = kotlin.random.Random.nextFloat()
            val rIntensity = 0.5f + kotlin.random.Random.nextFloat() * 0.5f
            Pair(Offset(rx, ry), rIntensity)
        }
    }

    // Pulsing glow animation for the Sun
    val infiniteTransition = rememberInfiniteTransition(label = "SunGlow")
    val sunPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f

        // 1. Draw Starfield Background
        stars.forEach { pair ->
            val pos = pair.first
            val intensity = pair.second
            drawCircle(
                color = Color.White.copy(alpha = intensity * 0.7f),
                radius = 1f + intensity * 1.5f,
                center = Offset(pos.x * width, pos.y * height)
            )
        }

        when (cameraMode) {
            CameraMode.SYSTEM -> {
                // --- SYSTEM VIEW: Shows overall Sun-Earth-Moon system ---
                // Scale factors based on viewport size
                val systemScale = min(width, height)
                val sunRadius = systemScale * 0.08f
                val orbitRadiusX = systemScale * 0.38f
                val orbitRadiusY = systemScale * 0.18f // Tilted ellipse perspective
                val earthRadius = systemScale * 0.035f
                val moonOrbitRadiusX = earthRadius * 2.8f
                val moonOrbitRadiusY = earthRadius * 1.2f
                val moonRadius = earthRadius * 0.32f

                // Draw orbit plane grid/lines (neon blue, semi-transparent)
                drawOrbitPath(centerX, centerY, orbitRadiusX, orbitRadiusY)

                // Draw Sun
                drawGlowingSun(centerX, centerY, sunRadius * sunPulse)

                // Calculate Earth's position along tilted ellipse
                val earthAngle = timeOfYear * 2 * PI
                val earthX = centerX + orbitRadiusX * cos(earthAngle).toFloat()
                val earthY = centerY + orbitRadiusY * sin(earthAngle).toFloat()

                // Draw Moon Orbit (Grey dotted line)
                drawMoonOrbitPath(earthX, earthY, moonOrbitRadiusX, moonOrbitRadiusY)

                // Draw Earth (Base, Continents, Day/Night side)
                draw3DGlobe(
                    centerX = earthX,
                    centerY = earthY,
                    radius = earthRadius,
                    rotationAngle = timeOfDay * 360f,
                    tiltAngle = axialTilt,
                    lightSourceX = centerX,
                    lightSourceY = centerY,
                    drawAxis = true,
                    textMeasurer = textMeasurer
                )

                // Calculate Moon's Position
                val moonAngle = moonPhase * 2 * PI
                val moonX = earthX + moonOrbitRadiusX * cos(moonAngle).toFloat()
                val moonY = earthY + moonOrbitRadiusY * sin(moonAngle).toFloat()

                // Draw Moon with its own day/night shading (facing Sun)
                drawMoon(
                    centerX = moonX,
                    centerY = moonY,
                    radius = moonRadius,
                    lightSourceX = centerX,
                    lightSourceY = centerY,
                    isBloodMoon = false
                )

                // Draw Labels
                drawLabelText("SUN", centerX, centerY - sunRadius - 15f, textMeasurer, SunYellow)
                drawLabelText("EARTH", earthX, earthY - earthRadius - 20f, textMeasurer, EarthBlue)
            }

            CameraMode.EARTH -> {
                // --- EARTH VIEW: Close-up of Earth showing Axial Tilt & Seasons ---
                val scale = min(width, height)
                val earthRadius = scale * 0.28f
                val earthX = centerX
                val earthY = centerY

                // Draw Sun rays coming from the extreme left
                drawSunlightRays(0f, centerY, earthX - earthRadius, centerY, height)

                // Draw Close-up 3D Earth
                draw3DGlobe(
                    centerX = earthX,
                    centerY = earthY,
                    radius = earthRadius,
                    rotationAngle = timeOfDay * 360f,
                    tiltAngle = axialTilt,
                    lightSourceX = -9999f, // Far left
                    lightSourceY = centerY,
                    drawAxis = true,
                    drawLatLonLines = true,
                    textMeasurer = textMeasurer
                )

                // Explain active season on globe
                drawSeasonsAnnotation(centerX, centerY, earthRadius, timeOfYear, textMeasurer)
            }

            CameraMode.ECLIPSE -> {
                // --- ECLIPSE VIEW: Conceptual Alignment to visualize shadows clearly ---
                val scale = min(width, height)
                val sunRadius = scale * 0.12f
                val earthRadius = scale * 0.08f
                val moonRadius = scale * 0.025f

                // Is it solar or lunar alignment?
                val isSolarEclipse = moonPhase < 0.25f || moonPhase > 0.75f // Moon is in front

                if (isSolarEclipse) {
                    // SOLAR ECLIPSE ALIGNMENT: Sun -> Moon -> Earth
                    val sunX = width * 0.15f
                    val earthX = width * 0.82f
                    val moonX = width * 0.52f

                    val sunY = centerY
                    val earthY = centerY
                    val moonY = centerY

                    // Draw Sun
                    drawGlowingSun(sunX, sunY, sunRadius)

                    // Draw light ray boundary lines and shadow cone (Umbra & Penumbra)
                    drawEclipseShadowCones(
                        sunX = sunX, sunY = sunY, sunRadius = sunRadius,
                        blockerX = moonX, blockerY = moonY, blockerRadius = moonRadius,
                        targetX = earthX, targetY = earthY, targetRadius = earthRadius,
                        isSolar = true
                    )

                    // Draw Moon (Dark Silhouette facing Earth)
                    drawMoon(moonX, moonY, moonRadius, sunX, sunY, isBloodMoon = false)

                    // Draw Earth
                    draw3DGlobe(
                        centerX = earthX,
                        centerY = earthY,
                        radius = earthRadius,
                        rotationAngle = timeOfDay * 360f,
                        tiltAngle = axialTilt,
                        lightSourceX = sunX,
                        lightSourceY = sunY,
                        drawAxis = false,
                        textMeasurer = textMeasurer
                    )

                    // Labels and overlays
                    drawLabelText("SUN", sunX, sunY - sunRadius - 15f, textMeasurer, SunYellow)
                    drawLabelText("MOON", moonX, moonY - moonRadius - 25f, textMeasurer, MoonGrey)
                    drawLabelText("EARTH", earthX, earthY - earthRadius - 15f, textMeasurer, EarthBlue)

                    // Shadow labels
                    drawLabelText("Umbra (Total Shadow)", (moonX + earthX)/2f, centerY - 15f, textMeasurer, Color.White, fontSize = 11.sp)
                    drawLabelText("Penumbra (Partial Shadow)", (moonX + earthX)/2f, centerY - 65f, textMeasurer, MoonGrey, fontSize = 10.sp)

                } else {
                    // LUNAR ECLIPSE ALIGNMENT: Sun -> Earth -> Moon
                    val sunX = width * 0.15f
                    val earthX = width * 0.52f
                    val moonX = width * 0.85f

                    val sunY = centerY
                    val earthY = centerY
                    val moonY = centerY

                    // Draw Sun
                    drawGlowingSun(sunX, sunY, sunRadius)

                    // Draw Earth
                    draw3DGlobe(
                        centerX = earthX,
                        centerY = earthY,
                        radius = earthRadius,
                        rotationAngle = timeOfDay * 360f,
                        tiltAngle = axialTilt,
                        lightSourceX = sunX,
                        lightSourceY = sunY,
                        drawAxis = false,
                        textMeasurer = textMeasurer
                    )

                    // Draw shadow cones from Earth to Moon
                    drawEclipseShadowCones(
                        sunX = sunX, sunY = sunY, sunRadius = sunRadius,
                        blockerX = earthX, blockerY = earthY, blockerRadius = earthRadius,
                        targetX = moonX, targetY = moonY, targetRadius = moonRadius,
                        isSolar = false
                    )

                    // Draw Moon - turned copper-red (Blood Moon) because it's inside the umbra shadow!
                    drawMoon(moonX, moonY, moonRadius, sunX, sunY, isBloodMoon = true)

                    // Labels
                    drawLabelText("SUN", sunX, sunY - sunRadius - 15f, textMeasurer, SunYellow)
                    drawLabelText("EARTH", earthX, earthY - earthRadius - 15f, textMeasurer, EarthBlue)
                    drawLabelText("BLOOD MOON", moonX, moonY - moonRadius - 25f, textMeasurer, RedBloodMoon)

                    // Atmospheric refraction explanation
                    drawLabelText("Earth's Atmosphere bends red light", earthX + earthRadius + 15f, centerY + earthRadius + 20f, textMeasurer, RedBloodMoon, fontSize = 10.sp)
                }
            }
        }
    }
}

// --- Helper Drawing Extension Functions ---

private fun DrawScope.drawOrbitPath(cx: Float, cy: Float, rx: Float, ry: Float) {
    drawOval(
        color = AccentTeal.copy(alpha = 0.25f),
        style = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
        ),
        size = Size(rx * 2, ry * 2),
        topLeft = Offset(cx - rx, cy - ry)
    )
}

private fun DrawScope.drawMoonOrbitPath(cx: Float, cy: Float, rx: Float, ry: Float) {
    drawOval(
        color = Color.LightGray.copy(alpha = 0.35f),
        style = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        ),
        size = Size(rx * 2, ry * 2),
        topLeft = Offset(cx - rx, cy - ry)
    )
}

private fun DrawScope.drawGlowingSun(cx: Float, cy: Float, radius: Float) {
    // Multi-layered neon sun glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, SunYellow, SunOrange, Color.Transparent),
            center = Offset(cx, cy),
            radius = radius * 2f
        ),
        radius = radius * 2f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = SunYellow,
        radius = radius,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawSunlightRays(
    startX: Float, startY: Float,
    endX: Float, endY: Float,
    height: Float
) {
    // Conceptual sunlight rays
    for (i in -4..4) {
        val offsetFactor = i * (height / 10f)
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(SunYellow.copy(alpha = 0.15f), Color.Transparent),
                start = Offset(startX, startY + offsetFactor),
                end = Offset(endX, endY + offsetFactor * 0.3f)
            ),
            start = Offset(startX, startY + offsetFactor),
            end = Offset(endX, endY + offsetFactor * 0.3f),
            strokeWidth = 2.dp.toPx()
        )
    }
}

private fun DrawScope.draw3DGlobe(
    centerX: Float,
    centerY: Float,
    radius: Float,
    rotationAngle: Float,
    tiltAngle: Float,
    lightSourceX: Float,
    lightSourceY: Float,
    drawAxis: Boolean,
    drawLatLonLines: Boolean = false,
    textMeasurer: TextMeasurer
) {
    // 1. Draw Ocean Base (Deep Space Celestial Blue)
    drawCircle(
        color = EarthBlue,
        radius = radius,
        center = Offset(centerX, centerY)
    )

    // Clip all continents to the Earth's perfect sphere circle boundary
    drawContext.canvas.save()
    val earthClipPath = Path().apply {
        addOval(Rect(centerX - radius, centerY - radius, centerX + radius, centerY + radius))
    }
    drawContext.canvas.clipPath(earthClipPath)

    // 2. Project and Draw Continents in 3D Orthographic projection
    val tiltRad = Math.toRadians(tiltAngle.toDouble()).toFloat()

    ContinentData.continents.forEach { continent ->
        val projectedPath = Path()
        var polygonStarted = false

        continent.forEach { point ->
            // Apply Earth axial spin rotation
            val rotatedLon = point.lon + rotationAngle
            val latRad = Math.toRadians(point.lat.toDouble()).toFloat()
            val lonRad = Math.toRadians(rotatedLon.toDouble()).toFloat()

            // 3D Cartesian coordinates of the point on a unit sphere (R=1)
            val x = cos(latRad) * sin(lonRad)
            val y = sin(latRad)
            val z = cos(latRad) * cos(lonRad)

            // Rotate around Z-axis by the axial tilt angle
            val xTilted = x * cos(tiltRad) - y * sin(tiltRad)
            val yTilted = x * sin(tiltRad) + y * cos(tiltRad)
            val zTilted = z // Z remains unchanged by 2D plane tilt

            // If the point is on the visible front hemisphere (z' > 0)
            if (zTilted > -0.15f) { // Slightly wrap around for cleaner borders
                val screenX = centerX + xTilted * radius
                val screenY = centerY - yTilted * radius

                if (!polygonStarted) {
                    projectedPath.moveTo(screenX, screenY)
                    polygonStarted = true
                } else {
                    projectedPath.lineTo(screenX, screenY)
                }
            }
        }

        if (polygonStarted) {
            projectedPath.close()
            drawPath(
                path = projectedPath,
                color = EarthGreen
            )
        }
    }

    // 3. Optional Latitude & Longitude Grid Lines for Academic representation
    if (drawLatLonLines) {
        // Draw Equator (Tilted)
        val equatorPath = Path()
        for (lon in -180..180 step 10) {
            val lonRotated = lon + rotationAngle
            val lonRad = Math.toRadians(lonRotated.toDouble()).toFloat()
            val x = sin(lonRad)
            val y = 0f
            val z = cos(lonRad)

            val xTilted = x * cos(tiltRad) - y * sin(tiltRad)
            val yTilted = x * sin(tiltRad) + y * cos(tiltRad)

            if (z >= 0) {
                val sx = centerX + xTilted * radius
                val sy = centerY - yTilted * radius
                if (lon == -180) equatorPath.moveTo(sx, sy) else equatorPath.lineTo(sx, sy)
            }
        }
        drawPath(
            path = equatorPath,
            color = Color.White.copy(alpha = 0.25f),
            style = Stroke(width = 1.dp.toPx())
        )
    }

    // 4. Draw Day/Night terminator shading overlay
    val lightAngle = if (lightSourceX == -9999f) {
        0f // Light directly from the left (180 degrees)
    } else {
        atan2(lightSourceY - centerY, lightSourceX - centerX)
    }

    // The dark hemisphere is always facing away from the Sun
    val terminatorStartAngle = Math.toDegrees((lightAngle + Math.PI / 2).toDouble()).toFloat()
    drawArc(
        color = Color.Black.copy(alpha = 0.65f),
        startAngle = terminatorStartAngle,
        sweepAngle = 180f,
        useCenter = true,
        size = Size(radius * 2f, radius * 2f),
        topLeft = Offset(centerX - radius, centerY - radius)
    )

    // Soft glowing ring edge (atmosphere glow)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, AccentTeal.copy(alpha = 0.3f), Color.Transparent),
            center = Offset(centerX, centerY),
            radius = radius * 1.08f
        ),
        radius = radius * 1.08f,
        center = Offset(centerX, centerY)
    )

    drawContext.canvas.restore()

    // 5. Draw Axial Tilt Line & Labels
    if (drawAxis) {
        val axisLength = radius * 1.35f
        // Axis points up-right at tiltAngle
        val dx = axisLength * sin(tiltRad)
        val dy = axisLength * cos(tiltRad)

        // North pole line
        drawLine(
            color = SunOrange,
            start = Offset(centerX, centerY),
            end = Offset(centerX + dx, centerY - dy),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 5f), 0f)
        )
        // South pole line
        drawLine(
            color = SunOrange.copy(alpha = 0.6f),
            start = Offset(centerX, centerY),
            end = Offset(centerX - dx, centerY + dy),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 5f), 0f)
        )

        // Draw 'N' and 'S' labels
        drawLabelText("N", centerX + dx * 1.12f, centerY - dy * 1.12f, textMeasurer, SunOrange, fontSize = 13.sp)
        drawLabelText("S", centerX - dx * 1.12f, centerY + dy * 1.12f, textMeasurer, SunOrange.copy(alpha = 0.6f), fontSize = 13.sp)

        // Equator label line indicator
        if (drawLatLonLines) {
            val eqX = centerX + radius * cos(tiltRad) * 0.9f
            val eqY = centerY + radius * sin(tiltRad) * 0.9f
            drawLabelText("Equator", eqX + 20f, eqY, textMeasurer, Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
        }
    }
}

private fun DrawScope.drawMoon(
    centerX: Float,
    centerY: Float,
    radius: Float,
    lightSourceX: Float,
    lightSourceY: Float,
    isBloodMoon: Boolean
) {
    if (isBloodMoon) {
        // Render beautiful reddish blood moon during lunar eclipses
        drawCircle(
            color = RedBloodMoon,
            radius = radius,
            center = Offset(centerX, centerY)
        )
        // Draw soft surface craters (dark red circles)
        drawCircle(color = Color(0xFF7F1D1D), radius = radius * 0.25f, center = Offset(centerX - radius * 0.3f, centerY - radius * 0.1f))
        drawCircle(color = Color(0xFF7F1D1D), radius = radius * 0.2f, center = Offset(centerX + radius * 0.2f, centerY + radius * 0.3f))
        drawCircle(color = Color(0xFF7F1D1D), radius = radius * 0.15f, center = Offset(centerX + radius * 0.1f, centerY - radius * 0.4f))

        // Draw soft copper atmosphere refract glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(RedBloodMoon.copy(alpha = 0.4f), Color.Transparent),
                center = Offset(centerX, centerY),
                radius = radius * 1.6f
            ),
            radius = radius * 1.6f,
            center = Offset(centerX, centerY)
        )
    } else {
        // Standard Moon: Grey, craters, and shaded facing Sun
        drawCircle(
            color = Color(0xFFD1D5DB), // Light grey Moon crust
            radius = radius,
            center = Offset(centerX, centerY)
        )

        // Small craters
        drawCircle(color = Color(0xFF9CA3AF), radius = radius * 0.22f, center = Offset(centerX - radius * 0.3f, centerY - radius * 0.1f))
        drawCircle(color = Color(0xFF9CA3AF), radius = radius * 0.18f, center = Offset(centerX + radius * 0.2f, centerY + radius * 0.2f))
        drawCircle(color = Color(0xFF9CA3AF), radius = radius * 0.12f, center = Offset(centerX, centerY - radius * 0.4f))

        // Day/Night Moon shading
        val lightAngle = atan2(lightSourceY - centerY, lightSourceX - centerX)
        val terminatorAngle = Math.toDegrees((lightAngle + Math.PI / 2).toDouble()).toFloat()

        drawArc(
            color = Color.Black.copy(alpha = 0.75f),
            startAngle = terminatorAngle,
            sweepAngle = 180f,
            useCenter = true,
            size = Size(radius * 2f, radius * 2f),
            topLeft = Offset(centerX - radius, centerY - radius)
        )
    }
}

private fun DrawScope.drawEclipseShadowCones(
    sunX: Float, sunY: Float, sunRadius: Float,
    blockerX: Float, blockerY: Float, blockerRadius: Float,
    targetX: Float, targetY: Float, targetRadius: Float,
    isSolar: Boolean
) {
    // Calculate light rays for shadow projection (Umbra & Penumbra)
    val angle = atan2(blockerY - sunY, blockerX - sunX)

    // Tangent geometry lines representing Umbra (Deep, convergent shadow)
    val uOffsetBx = blockerRadius * sin(angle)
    val uOffsetBy = blockerRadius * cos(angle)
    val uOffsetSx = sunRadius * sin(angle)
    val uOffsetSy = sunRadius * cos(angle)

    // Ray 1: Top Sun to Top Blocker (converts to edge of Umbra shadow)
    val ray1Start = Offset(sunX - uOffsetSx, sunY - uOffsetSy)
    val ray1Block = Offset(blockerX - uOffsetBx, blockerY - uOffsetBy)

    // Ray 2: Bottom Sun to Bottom Blocker
    val ray2Start = Offset(sunX + uOffsetSx, sunY + uOffsetSy)
    val ray2Block = Offset(blockerX + uOffsetBx, blockerY + uOffsetBy)

    // Extend shadow rays past blocker toward target
    val extFactor = (targetX - blockerX) * 1.5f
    val ray1End = Offset(ray1Block.x + extFactor, ray1Block.y + (ray1Block.y - ray1Start.y) / (ray1Block.x - ray1Start.x) * extFactor)
    val ray2End = Offset(ray2Block.x + extFactor, ray2Block.y + (ray2Block.y - ray2Start.y) / (ray2Block.x - ray2Start.x) * extFactor)

    // Draw the Umbra (Deep shadow cone)
    val umbraPath = Path().apply {
        moveTo(blockerX, blockerY)
        lineTo(ray1Block.x, ray1Block.y)
        lineTo(ray1End.x, ray1End.y)
        lineTo(ray2End.x, ray2End.y)
        lineTo(ray2Block.x, ray2Block.y)
        close()
    }
    drawPath(
        path = umbraPath,
        color = Color.Black.copy(alpha = 0.72f)
    )

    // Draw Penumbra (Partial divergent shadow cone)
    // Ray 3: Top Sun to Bottom Blocker
    val ray3Start = Offset(sunX - uOffsetSx, sunY - uOffsetSy)
    val ray3Block = Offset(blockerX + uOffsetBx, blockerY + uOffsetBy)
    val ray3End = Offset(ray3Block.x + extFactor, ray3Block.y + (ray3Block.y - ray3Start.y) / (ray3Block.x - ray3Start.x) * extFactor)

    // Ray 4: Bottom Sun to Top Blocker
    val ray4Start = Offset(sunX + uOffsetSx, sunY + uOffsetSy)
    val ray4Block = Offset(blockerX - uOffsetBx, blockerY - uOffsetBy)
    val ray4End = Offset(ray4Block.x + extFactor, ray4Block.y + (ray4Block.y - ray4Start.y) / (ray4Block.x - ray4Start.x) * extFactor)

    val penumbraPath = Path().apply {
        moveTo(blockerX, blockerY)
        lineTo(ray4Block.x, ray4Block.y)
        lineTo(ray4End.x, ray4End.y)
        lineTo(ray1End.x, ray1End.y) // Between Umbra Ray 1 and Penumbra Ray 4
        close()
    }
    val penumbraPath2 = Path().apply {
        moveTo(blockerX, blockerY)
        lineTo(ray3Block.x, ray3Block.y)
        lineTo(ray3End.x, ray3End.y)
        lineTo(ray2End.x, ray2End.y) // Between Umbra Ray 2 and Penumbra Ray 3
        close()
    }
    drawPath(path = penumbraPath, color = Color.Gray.copy(alpha = 0.2f))
    drawPath(path = penumbraPath2, color = Color.Gray.copy(alpha = 0.2f))

    // Draw reference ray outline lines
    drawLine(color = SunYellow.copy(alpha = 0.3f), start = ray1Start, end = ray1End, strokeWidth = 1f)
    drawLine(color = SunYellow.copy(alpha = 0.3f), start = ray2Start, end = ray2End, strokeWidth = 1f)
    drawLine(color = SunYellow.copy(alpha = 0.2f), start = ray3Start, end = ray3End, strokeWidth = 1f)
    drawLine(color = SunYellow.copy(alpha = 0.2f), start = ray4Start, end = ray4End, strokeWidth = 1f)

    if (isSolar) {
        // Draw the black shadow spot landing on the Earth (eclipse totality spot)
        // Find intersection of Umbra path with Earth's center line
        val shadowSpotY = blockerY
        drawCircle(
            color = Color.Black,
            radius = targetRadius * 0.18f,
            center = Offset(targetX - targetRadius * 0.95f, shadowSpotY)
        )
        // Soft outer partial shadow circle
        drawCircle(
            color = Color.Black.copy(alpha = 0.4f),
            radius = targetRadius * 0.45f,
            center = Offset(targetX - targetRadius * 0.95f, shadowSpotY)
        )
    }
}

private fun DrawScope.drawSeasonsAnnotation(
    cx: Float, cy: Float, radius: Float,
    timeOfYear: Float, textMeasurer: TextMeasurer
) {
    // Sub-text explaining which seasons are currently active based on orbital position
    val monthIndex = (timeOfYear * 12).toInt() % 12
    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val activeMonth = months[monthIndex]

    // Solstices or Equinoxes details
    val seasonalDesc = when (monthIndex) {
        5, 6, 7 -> "Northern Summer ☀️ / Southern Winter ❄️\n(North Pole tilted TOWARD Sun)"
        11, 0, 1 -> "Northern Winter ❄️ / Southern Summer ☀️\n(North Pole tilted AWAY from Sun)"
        2, 3, 4 -> "Vernal Equinox 🌱 (March)\n(Equal Day & Night everywhere)"
        else -> "Autumnal Equinox 🍂 (September)\n(Equal Day & Night everywhere)"
    }

    val infoText = "Current Position: $activeMonth\n$seasonalDesc"
    val textLayoutResult = textMeasurer.measure(
        text = AnnotatedString(infoText),
        style = TextStyle(
            color = Color.White,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            background = Color.Black.copy(alpha = 0.6f)
        )
    )

    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(cx - textLayoutResult.size.width / 2f, cy + radius + 30f)
    )
}

private fun DrawScope.drawLabelText(
    text: String,
    x: Float,
    y: Float,
    textMeasurer: TextMeasurer,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp
) {
    val textLayoutResult = textMeasurer.measure(
        text = AnnotatedString(text),
        style = TextStyle(
            color = color,
            fontSize = fontSize,
            background = Color.Black.copy(alpha = 0.5f)
        )
    )
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(x - textLayoutResult.size.width / 2f, y - textLayoutResult.size.height / 2f)
    )
}
