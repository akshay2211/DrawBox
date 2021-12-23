package io.ak1.pencil.models

import android.graphics.Path
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color


/**
 * Created by akshay on 23/12/21
 * https://ak1.io
 */

internal data class PathWrapper(val path: Path, val strokeWidth: Float = 5f, val strokeColor :Color)
internal data class Dot(var x: Float = 0.0f, var y: Float = 0.0f)
