package com.example.stars1

import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlin.random.Random

const val STAR_LIFETIME_MS = 10000L // 10 seconds
const val STAR_CREATION_INTERVAL_MS = 1000L // 1 second

@Composable
fun Starfield() {
    val context = LocalContext.current
    val stars = remember { mutableStateListOf<Star>() }
    val mediaPlayers = remember { mutableStateMapOf<Int, MediaPlayer>() }

    val soundResources = listOf(
        R.raw.cc, R.raw.b37, R.raw.ddd, R.raw.fff, R.raw.jjj, R.raw.sun, R.raw.a329, R.raw.gggg,
        R.raw.mars, R.raw.whsl, R.raw.earth, R.raw.venus, R.raw.saturn, R.raw.uranus, R.raw.jupiter,
        R.raw.mercury, R.raw.neptune, R.raw.blackhole, R.raw.enceladus, R.raw.scarun001, R.raw.spookysaturn, R.raw.dawn_in_space,
        R.raw.blackholemerge, R.raw.emfisis_chorus_1, R.raw.auroral_star_wars, R.raw.kepler_star_71081, R.raw.saturn_radio_waves,
        R.raw.spaces1soundsmovie, R.raw.kepler_star_2268220, R.raw.voyager_jupiter_lightning
    )

    var nextStarId = 0

    LaunchedEffect(Unit) {
        while (true) {
            val currentTime = System.currentTimeMillis()

            // Create a new star
            val soundResId = soundResources.random()
            val star = Star(
                id = nextStarId++,
                x = Random.nextFloat() * 2 - 1,
                y = Random.nextFloat() * 2 - 1,
                z = 1f,
                color = Color(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256)).hashCode(),
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

            // Remove old stars
            val starsToRemove = stars.filter { currentTime - it.lifetime > STAR_LIFETIME_MS }
            stars.removeAll(starsToRemove)
            starsToRemove.forEach { oldStar ->
                mediaPlayers.remove(oldStar.id)?.apply {
                    stop()
                    release()
                }
            }

            delay(STAR_CREATION_INTERVAL_MS)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val currentTime = System.currentTimeMillis()
        stars.forEach { star ->
            val age = (currentTime - star.lifetime).toFloat() / STAR_LIFETIME_MS
            val scale = if (age < 0.5f) age * 2 else (1 - age) * 2

            mediaPlayers[star.id]?.setVolume(scale, scale)

            val x = (star.x / (1 - age)) * size.width / 2 + size.width / 2
            val y = (star.y / (1 - age)) * size.height / 2 + size.height / 2

            if (x >= 0 && x < size.width && y >= 0 && y < size.height) {
                drawCircle(
                    color = Color(star.color),
                    radius = scale * star.luminosity * 10,
                    center = Offset(x, y)
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayers.values.forEach {
                it.stop()
                it.release()
            }
            mediaPlayers.clear()
        }
    }
}
