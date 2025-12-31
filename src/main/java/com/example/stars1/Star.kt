package com.example.stars1

import androidx.annotation.RawRes

data class Star(
    val id: Int,
    val x: Float,
    val y: Float,
    val z: Float,
    val color: Int,
    val luminosity: Float,
    @RawRes val soundResId: Int,
    val lifetime: Long
)
