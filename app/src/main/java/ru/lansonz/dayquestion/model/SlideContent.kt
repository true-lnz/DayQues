package ru.lansonz.dayquestion.model

import android.graphics.drawable.Drawable

data class SlideContent(
    val image: Drawable,
    val header: String,
    val description: String
)