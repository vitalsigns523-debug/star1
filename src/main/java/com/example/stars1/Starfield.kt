package com.example.stars1

import android.app.Activity
import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun Starfield() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val orientationManager = remember { OrientationManager(context) }

    var flightTime by remember { mutableStateOf(settingsManager.getFlightTime()) }
    var creationInterval by remember { mutableStateOf(settingsManager.getCreationInterval()) }
    var showControls by remember { mutableStateOf(false) }

    var rollMode by remember { mutableStateOf(settingsManager.getRollMode()) }
    var pitchMode by remember { mutableStateOf(settingsManager.getPitchMode()) }
    var yawMode by remember { mutableStateOf(settingsManager.getYawMode()) }

    val orientation by orientationManager.orientation.collectAsState()
    var viewAzimuth by remember { mutableStateOf(0f) }
    var viewPitch by remember { mutableStateOf(0f) }

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
            mediaPlayers.values.forEach {
                it.stop()
                it.release()
            }
            mediaPlayers.clear()

            settingsManager.setFlightTime(flightTime)
            settingsManager.setCreationInterval(creationInterval)
            settingsManager.setRollMode(rollMode)
            settingsManager.setPitchMode(pitchMode)
            settingsManager.setYawMode(yawMode)
        }
    }

    LaunchedEffect(flightTime, showControls, rollMode, pitchMode, yawMode) {
        var nextStarId = 0
        var lastCreationTime = 0L
        var lastFrameTime = System.currentTimeMillis()

        while (true) {
            val currentTime = System.currentTimeMillis()
            val elapsed = currentTime - lastFrameTime
            lastFrameTime = currentTime
            frameTime = currentTime

            if (!showControls) {
                if (pitchMode == PitchMode.ELEVATOR) {
                    val pitch = orientation[1]
                    val delta = -pitch * (elapsed / 1000f)
                    flightTime = (flightTime + delta).coerceIn(1f, 10f)
                }
                if (yawMode == YawMode.PAN_VIEW) {
                    viewAzimuth = orientation[0]
                }
                if (pitchMode == PitchMode.TILT_VIEW) {
                    viewPitch = orientation[1]
                }
            } else {
                viewAzimuth = 0f
                viewPitch = 0f
            }

            if (currentTime - lastCreationTime > (creationInterval * 1000).toLong()) {
                val soundResId = soundResources.random()

                var red = 220
                var green = 220
                var blue = 220

                when (Random.nextInt(3)) { 0 -> red = 255; 1 -> green = 255; 2 -> blue = 255 }
                when (Random.nextInt(3)) { 0 -> red = 255; 1 -> green = 255; 2 -> blue = 255 }

                val star = Star(
                    id = nextStarId++,
                    x = (Random.nextFloat() * 0.4f) - 0.2f,
                    y = (Random.nextFloat() * 0.4f) - 0.2f,
                    z = 1f,
                    color = Color(red, green, blue).toArgb(),
                    luminosity = Random.nextFloat(),
                    soundResId = soundResId,
                    lifetime = currentTime
                )
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

                    val azimuth = if (yawMode == YawMode.RUDDER) orientation[0] else 0f
                    val pitch = if (pitchMode == PitchMode.ELEVATOR) orientation[1] else 0f
                    val roll = if (rollMode == RollMode.AILERON) orientation[2] else 0f

                    val cosYaw = cos(-azimuth)
                    val sinYaw = sin(-azimuth)
                    val cosPitch = cos(-pitch)
                    val sinPitch = sin(-pitch)
                    val cosRoll = cos(-roll)
                    val sinRoll = sin(-roll)

                    val px_r1 = px * cosYaw + pz * sinYaw
                    val pz_r1 = -px * sinYaw + pz * cosYaw

                    val py_r2 = py * cosPitch - pz_r1 * sinPitch
                    val pz_r2 = py * sinPitch + pz_r1 * cosPitch
                    val px_r2 = px_r1

                    val px_r3 = px_r2 * cosRoll - py_r2 * sinRoll
                    val py_r3 = px_r2 * sinRoll + py_r2 * cosRoll
                    val pz_r3 = pz_r2

                    val panX = if (pz_r3 > 0) px_r3 / pz_r3 else px_r3
                    val pan = (panX.coerceIn(-1f, 1f) + 1) / 2f

                    val leftVolume = scale * (1 - pan)
                    val rightVolume = scale * pan

                    mediaPlayers[star.id]?.setVolume(leftVolume, rightVolume)
                }
            }

            if (starsToRemove.isNotEmpty()) {
                stars.removeAll(starsToRemove)
                starsToRemove.forEach { starToRemove ->
                    mediaPlayers.remove(starToRemove.id)?.apply {
                        stop()
                        release()
                    }
                }
            }

            delay(16)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
        detectTapGestures { showControls = true }
    }) {
        val currentTime = frameTime
        val azimuth = if (yawMode == YawMode.RUDDER) orientation[0] else 0f
        val pitch = if (pitchMode == PitchMode.ELEVATOR) orientation[1] else 0f
        val roll = if (rollMode == RollMode.AILERON) orientation[2] else 0f

        val cosYaw = cos(-azimuth)
        val sinYaw = sin(-azimuth)
        val cosPitch = cos(-pitch)
        val sinPitch = sin(-pitch)
        val cosRoll = cos(-roll)
        val sinRoll = sin(-roll)

        val viewXOffset = if (yawMode == YawMode.PAN_VIEW) viewAzimuth * size.width / 2 else 0f
        val viewYOffset = if (pitchMode == PitchMode.TILT_VIEW) -viewPitch * size.height / 2 else 0f

        stars.forEach { star ->
            val age = (currentTime - star.lifetime).toFloat() / (flightTime * 1000)
            if (age < 1) {
                val pz = 1.0f - 2.0f * age
                val px = star.x
                val py = star.y

                val px_r1 = px * cosYaw + pz * sinYaw
                val pz_r1 = -px * sinYaw + pz * cosYaw

                val py_r2 = py * cosPitch - pz_r1 * sinPitch
                val pz_r2 = py * sinPitch + pz_r1 * cosPitch
                val px_r2 = px_r1

                val px_r3 = px_r2 * cosRoll - py_r2 * sinRoll
                val py_r3 = px_r2 * sinRoll + py_r2 * cosRoll
                val pz_r3 = pz_r2

                if (pz_r3 > 0) {
                    val projectedX = (px_r3 / pz_r3) * size.width / 2f + size.width / 2f - viewXOffset
                    val projectedY = (py_r3 / pz_r3) * size.height / 2f + size.height / 2f - viewYOffset

                    val scale = if (age < 0.5f) age * 2 else (1 - age) * 2
                    val radius = (scale * star.luminosity * 10 / pz_r3).coerceAtLeast(0.1f)

                    if (projectedX >= 0 && projectedX < size.width && projectedY >= 0 && projectedY < size.height) {
                        drawCircle(
                            color = Color(star.color),
                            radius = radius,
                            center = Offset(projectedX, projectedY)
                        )
                    }
                }
            }
        }
    }

    if (showControls) {
        Dialog(onDismissRequest = { showControls = false }) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Controls")
                    Spacer(modifier = Modifier.height(16.dp))

                    SpinnerControl("Roll", rollMode, { rollMode = it }, RollMode.values())
                    SpinnerControl("Pitch", pitchMode, { pitchMode = it }, PitchMode.values())
                    SpinnerControl("Yaw", yawMode, { yawMode = it }, YawMode.values())

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Flight Time: ${flightTime.toInt()} seconds")
                    Slider(
                        value = flightTime,
                        onValueChange = { flightTime = it },
                        valueRange = 1f..10f,
                        steps = 9,
                        enabled = pitchMode != PitchMode.ELEVATOR
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Creation Interval: ${String.format("%.1f", creationInterval)} seconds")
                    Slider(
                        value = creationInterval,
                        onValueChange = { creationInterval = it },
                        valueRange = 0.2f..2f,
                        steps = 17
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { showControls = false }) {
                            Text("Return to Starfield")
                        }
                        Button(onClick = { (context as? Activity)?.finish() }) {
                            Text("Quit App")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun <T> SpinnerControl(label: String, selected: T, onSelected: (T) -> Unit, options: Array<T>) {
    var expanded by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ")
        Box {
            Text(selected.toString(), modifier = Modifier.clickable { expanded = true })
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option.toString()) }, onClick = {
                        onSelected(option)
                        expanded = false
                    })
                }
            }
        }
    }
}
