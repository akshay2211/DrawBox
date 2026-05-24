package io.ak1.drawbox.domain.model

import androidx.compose.ui.graphics.Color

data class PayLoad(
    val bgColor: Color,
    val elements: List<Element>,
)
