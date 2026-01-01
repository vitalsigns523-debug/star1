package com.example.stars1

import android.app.Activity
import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
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
import kotlin.random.Random

@Composable
fun Starfield() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val orientationManager = remember { OrientationManager(context) }

    var flightTime by remember { mutableStateOf(settingsManager.getFlightTime()) }
    var creationInterval by remember { mutableStateOf(settingsManager.getCreationInterval()) }
    var showControls by remember { mutableStateOf(false) }

    val orientation by orientationManager.orientation.collectAsState()

    val stars = remember { mutableStateListOf<Star>() }
    val mediaPlayers = remember { mutableStateMapOf<Int, MediaPlayer>() }

    val soundResources = remember {
        R.raw::class.java.fields.map { it.getInt(null) }
    }

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
        }
    }

    LaunchedEffect(flightTime) {
        var nextStarId = 0
        var lastCreationTime = 0L

        while (true) {
            val currentTime = System.currentTimeMillis()
            frameTime = currentTime

            val pitch = orientationManager.orientation.value[1].coerceIn(-PI.toFloat() / 4, PI.toFloat() / 4)
            val mappedPitch = (pitch + (PI.toFloat() / 4)) / (PI.toFloat() / 2)
            val newCreationInterval = 2.0f - mappedPitch * 1.8f
            creationInterval = newCreationInterval

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

                    val projectedX = (star.x / (1 - age)).coerceIn(-1f, 1f)
                    val pan = (projectedX + 1) / 2f

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
        val azimuth = orientation[0]
        val pitch = orientation[1]

        val viewXOffset = azimuth * size.width / 2
        val viewYOffset = -pitch * size.height / 2

        stars.forEach { star ->
            val age = (currentTime - star.lifetime).toFloat() / (flightTime * 1000)
            if (age < 1) {
                val scale = if (age < 0.5f) age * 2 else (1 - age) * 2
                val denominator = 1 - age

                if (denominator > 0) {
                    val x = (star.x / denominator) * size.width / 2 + size.width / 2 - viewXOffset
                    val y = (star.y / denominator) * size.height / 2 + size.height / 2 - viewYOffset

                    if (x >= 0 && x < size.width && y >= 0 && y < size.height) {
                        drawCircle(
                            color = Color(star.color),
                            radius = (scale * star.luminosity * 10).coerceAtLeast(0.1f),
                            center = Offset(x, y)
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

                    Text("Flight Time: ${flightTime.toInt()} seconds")
                    Slider(
                        value = flightTime,
                        onValueChange = { flightTime = it },
                        valueRange = 1f..10f,
                        steps = 9
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Creation Interval: ${String.format("%.1f", creationInterval)} seconds")
                    Slider(
                        value = creationInterval,
                        onValueChange = { creationInterval = it },
                        valueRange = 0.2f..2f,
                        steps = 17,
                        enabled = false // Disabled when using motion controls
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
