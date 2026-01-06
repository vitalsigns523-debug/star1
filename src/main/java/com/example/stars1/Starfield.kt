package com.example.stars1

import android.app.Activity
import android.media.MediaPlayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun Starfield() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val orientationManager = remember { OrientationManager(context) }

    var flightTime by remember { mutableFloatStateOf(settingsManager.getFlightTime()) }
    var creationInterval by remember { mutableFloatStateOf(settingsManager.getCreationInterval()) }
    var showControls by remember { mutableStateOf(false) }

    var rollMode by remember { mutableStateOf(settingsManager.getRollMode()) }
    var pitchMode by remember { mutableStateOf(settingsManager.getPitchMode()) }
    var yawMode by remember { mutableStateOf(settingsManager.getYawMode()) }

    val orientation by orientationManager.orientation.collectAsState()
    var viewAzimuth by remember { mutableFloatStateOf(0f) }
    var viewPitch by remember { mutableFloatStateOf(0f) }

    var yawOffset by remember { mutableFloatStateOf(0f) }
    var pitchOffset by remember { mutableFloatStateOf(0f) }
    var rollOffset by remember { mutableFloatStateOf(0f) }

    val stars = remember { mutableStateListOf<Star>() }
    val mediaPlayers = remember { mutableStateMapOf<Int, MediaPlayer>() }

    val soundResources = listOf(
        R.raw.cc, R.raw.b37, R.raw.ddd, R.raw.fff, R.raw.jjj, R.raw.sun, R.raw.a329, R.raw.gggg,
        R.raw.mars, R.raw.vela, R.raw.whsl, R.raw.b1055, R.raw.earth, R.raw.emhop, R.raw.f0329,
        R.raw.merc1, R.raw.vela2, R.raw.venus, R.raw.f7tuc2, R.raw.saturn, R.raw.sirius, R.raw.uranus,
        R.raw.epwhist, R.raw.jupiter, R.raw.mercury, R.raw.neptune, R.raw.blackhole, R.raw.enceladus,
        R.raw.betelgeuze, R.raw.spookysaturn, R.raw.dawn_in_space, R.raw.blackholemerge, R.raw.emfisis_chorus_1,
        R.raw.kepler_star_71081, R.raw.saturn_radio_waves, R.raw.spaces1soundsmovie, R.raw.kepler_star_2268220,
        R.raw.voyager_jupiter_lightning
    )

    var frameTime by remember { mutableStateOf(System.currentTimeMillis()) }

    DisposableEffect(Unit) {
        orientationManager.register()
        onDispose {
            orientationManager.unregister()
            mediaPlayers.values.forEach { it.stop(); it.release() }
            mediaPlayers.clear()

            settingsManager.setFlightTime(flightTime)
            settingsManager.setCreationInterval(creationInterval)
            settingsManager.setRollMode(rollMode)
            settingsManager.setPitchMode(pitchMode)
            settingsManager.setYawMode(yawMode)
        }
    }

    LaunchedEffect(showControls) {
        if (showControls) {
            yawOffset = orientation[0]
            pitchOffset = orientation[2] // landscape mode
            rollOffset = orientation[1]  // landscape mode
        }
    }

    LaunchedEffect(rollMode, pitchMode, yawMode, creationInterval) {
        var nextStarId = 0
        var lastCreationTime = 0L
        var lastFrameTime = System.currentTimeMillis()

        while (true) {
            val currentTime = System.currentTimeMillis()
            val elapsed = currentTime - lastFrameTime
            lastFrameTime = currentTime
            frameTime = currentTime

            if (!showControls) {
                val devicePitch = orientation[2] - pitchOffset
                if (pitchMode == PitchMode.ELEVATOR) {
                    val delta = -devicePitch * (elapsed / 1000f)
                    flightTime = (flightTime + delta).coerceIn(1f, 10f)
                }
                if (yawMode == YawMode.PAN_VIEW) {
                    viewAzimuth = orientation[0] - yawOffset
                }
                if (pitchMode == PitchMode.TILT_VIEW) {
                    viewPitch = devicePitch
                }
            } else {
                viewAzimuth = 0f
                viewPitch = 0f
            }

            if (currentTime - lastCreationTime > (creationInterval * 1000).toLong()) {
                val soundResId = soundResources.random()

                var red = 220; var green = 220; var blue = 220
                when (Random.nextInt(3)) { 0 -> red = 255; 1 -> green = 255; 2 -> blue = 255 }
                when (Random.nextInt(3)) { 0 -> red = 255; 1 -> green = 255; 2 -> blue = 255 }

                val star = Star(id = nextStarId++, x = (Random.nextFloat() * 0.4f) - 0.2f, y = (Random.nextFloat() * 0.4f) - 0.2f, z = 1f, color = Color(red, green, blue).toArgb(), luminosity = Random.nextFloat(), soundResId = soundResId, lifetime = currentTime)
                stars.add(star)

                mediaPlayers[star.id] = MediaPlayer.create(context, star.soundResId).apply {
                    isLooping = true
                    setVolume(0f, 0f)
                    start()
                }
                lastCreationTime = currentTime
            }

            val starsToRemove = mutableListOf<Star>()
            stars.forEach { star ->
                val age = (currentTime - star.lifetime).toFloat() / (flightTime * 1000)
                if (age >= 1) {
                    starsToRemove.add(star)
                } else {
                    val scale = if (age < 0.5f) age * 2 else (1 - age) * 2

                    val pz = 1.0f - 2.0f * age
                    val px = star.x
                    val py = star.y

                    val yaw = if (yawMode == YawMode.RUDDER && !showControls) orientation[0] - yawOffset else 0f
                    val pitch = if (pitchMode == PitchMode.ELEVATOR && !showControls) orientation[2] - pitchOffset else 0f
                    val roll = when(rollMode) {
                        RollMode.AILERON -> if (!showControls) orientation[1] - rollOffset else 0f
                        RollMode.INVARIANT -> if (!showControls) -(orientation[1] - rollOffset) else 0f
                        RollMode.IGNORE -> 0f
                    }

                    val cosYaw = cos(-yaw); val sinYaw = sin(-yaw)
                    val cosPitch = cos(-pitch); val sinPitch = sin(-pitch)
                    val cosRoll = cos(-roll); val sinRoll = sin(-roll)

                    val pxR1 = px * cosYaw + pz * sinYaw
                    val pzR1 = -px * sinYaw + pz * cosYaw

                    val pyR2 = py * cosPitch - pzR1 * sinPitch
                    val pzR2 = py * sinPitch + pzR1 * cosPitch

                    val pxR3 = pxR1 * cosRoll - pyR2 * sinRoll

                    if (yawMode == YawMode.PAN_VIEW || pitchMode == PitchMode.TILT_VIEW || pzR2 > 0) {
                        if (pzR2 != 0f) {
                            val panX = pxR3 / pzR2
                            val pan = (panX.coerceIn(-1f, 1f) + 1) / 2f

                            val leftVolume = scale * (1 - pan); val rightVolume = scale * pan
                            mediaPlayers[star.id]?.setVolume(leftVolume, rightVolume)
                        } else {
                            mediaPlayers[star.id]?.setVolume(scale, scale)
                        }
                    } else {
                        mediaPlayers[star.id]?.setVolume(0f, 0f)
                    }
                }
            }

            if (starsToRemove.isNotEmpty()) {
                stars.removeAll(starsToRemove)
                starsToRemove.forEach { starToRemove -> mediaPlayers.remove(starToRemove.id)?.apply { stop(); release() } }
            }

            delay(16)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { showControls = true } }) {
            val currentTime = frameTime
            val yaw = if (yawMode == YawMode.RUDDER && !showControls) orientation[0] - yawOffset else 0f
            val pitch = if (pitchMode == PitchMode.ELEVATOR && !showControls) orientation[2] - pitchOffset else 0f
            val roll = when(rollMode) {
                RollMode.AILERON -> if (!showControls) orientation[1] - rollOffset else 0f
                RollMode.INVARIANT -> if (!showControls) -(orientation[1] - rollOffset) else 0f
                RollMode.IGNORE -> 0f
            }

            val cosYaw = cos(-yaw); val sinYaw = sin(-yaw)
            val cosPitch = cos(-pitch); val sinPitch = sin(-pitch)
            val cosRoll = cos(-roll); val sinRoll = sin(-roll)

            val viewXOffset = if (yawMode == YawMode.PAN_VIEW) viewAzimuth * size.width / 2 else 0f
            val viewYOffset = if (pitchMode == PitchMode.TILT_VIEW) -viewPitch * size.height / 2 else 0f

            stars.forEach { star ->
                val age = (currentTime - star.lifetime).toFloat() / (flightTime * 1000)
                if (age < 1) {
                    val pz = 1.0f - 2.0f * age
                    val px = star.x
                    val py = star.y

                    val pxR1 = px * cosYaw + pz * sinYaw
                    val pzR1 = -px * sinYaw + pz * cosYaw

                    val pyR2 = py * cosPitch - pzR1 * sinPitch
                    val pzR2 = py * sinPitch + pzR1 * cosPitch

                    val pxR3 = pxR1 * cosRoll - pyR2 * sinRoll
                    val pyR3 = pxR1 * sinRoll + pyR2 * cosRoll

                    if (yawMode == YawMode.PAN_VIEW || pitchMode == PitchMode.TILT_VIEW || pzR2 > 0) {
                        if (pzR2 != 0f) {
                            val projectedX = (pxR3 / pzR2) * size.width / 2f + size.width / 2f - viewXOffset
                            val projectedY = (pyR3 / pzR2) * size.height / 2f + size.height / 2f - viewYOffset

                            val scale = if (age < 0.5f) age * 2 else (1 - age) * 2
                            val radius = (scale * star.luminosity * 10 / abs(pzR2)).coerceAtLeast(0.1f)

                            drawCircle(color = Color(star.color), radius = radius, center = Offset(projectedX, projectedY))
                        }
                    }
                }
            }
        }

        if (showControls) {
            Box(
                modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = {}) },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.8f),
                    shape = MaterialTheme.shapes.medium,
                    color = Color(0xFF0F0000),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Controls", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.align(Alignment.CenterHorizontally), color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(Modifier.fillMaxWidth()) {
                            // Left Column for Spinners
                            Column(
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SpinnerControl("Roll", rollMode, { rollMode = it }, RollMode.entries)
                                SpinnerControl("Pitch", pitchMode, { pitchMode = it }, PitchMode.entries)
                                SpinnerControl("Yaw", yawMode, { yawMode = it }, YawMode.entries)
                            }

                            Spacer(Modifier.width(16.dp))

                            // Right Column for Sliders and Buttons
                            Column(
                                modifier = Modifier.weight(1f).padding(start = 8.dp).verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Flight Time: ${flightTime.toInt()} seconds", color = Color.Gray)
                                Slider(value = flightTime, onValueChange = { flightTime = it }, valueRange = 1f..10f, steps = 9)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Creation Interval: ${String.format(Locale.US, "%.1f", creationInterval)} seconds", color = Color.Gray)
                                Slider(value = creationInterval, onValueChange = { creationInterval = it }, valueRange = 0.2f..2f, steps = 17)
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                                    Button(onClick = { showControls = false }) { Text("Stars") }
                                    Button(onClick = {
                                        mediaPlayers.values.forEach { it.stop(); it.release() }
                                        mediaPlayers.clear()
                                        (context as? Activity)?.finish()
                                    }) { Text("Quit") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun <T> SpinnerControl(label: String, selected: T, onSelected: (T) -> Unit, options: List<T>) where T : Enum<T> {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
            color = Color(0xFF6200EE) // Purple background
        ) {
            Row(
                modifier = Modifier.clickable { expanded = true }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$label: ${selected.name}", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option.name) }, onClick = { onSelected(option); expanded = false })
            }
        }
    }
}
