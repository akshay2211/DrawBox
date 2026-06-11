package io.ak1.drawbox.domain.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.json.Json

fun Color.toHexString(): String {
    val r = (red * 255).toInt().toString(16).padStart(2, '0')
    val g = (green * 255).toInt().toString(16).padStart(2, '0')
    val b = (blue * 255).toInt().toString(16).padStart(2, '0')
    val a = (alpha * 255).toInt().toString(16).padStart(2, '0')
    return "#$r$g$b$a"
}

fun String.toColor(): Color {
    val hex = this.removePrefix("#")
    return if (hex.length == 8) {
        val r = hex.substring(0, 2).toInt(16) / 255f
        val g = hex.substring(2, 4).toInt(16) / 255f
        val b = hex.substring(4, 6).toInt(16) / 255f
        val a = hex.substring(6, 8).toInt(16) / 255f
        Color(r, g, b, a)
    } else {
        Color.Black
    }
}

fun Offset.toJsonString(): String = "${x},${y}"

fun String.toOffset(): Offset {
    val parts = this.split(",")
    return if (parts.size == 2) {
        Offset(parts[0].toFloat(), parts[1].toFloat())
    } else {
        Offset.Zero
    }
}

fun ShapeType.toTypeString(): String = when (this) {
    ShapeType.RECTANGLE -> "RECTANGLE"
    ShapeType.CIRCLE -> "CIRCLE"
    ShapeType.TRIANGLE -> "TRIANGLE"
    ShapeType.ARROW -> "ARROW"
    ShapeType.LINE -> "LINE"
}

fun String.toShapeType(): ShapeType = when (this) {
    "RECTANGLE" -> ShapeType.RECTANGLE
    "CIRCLE" -> ShapeType.CIRCLE
    "TRIANGLE" -> ShapeType.TRIANGLE
    "ARROW" -> ShapeType.ARROW
    "LINE" -> ShapeType.LINE
    else -> ShapeType.RECTANGLE
}

data class ElementDto(
    val id: String,
    val type: String,
    val zIndex: Int,
    val points: List<String>,
    val strokeColor: String,
    val strokeWidth: Float,
    val alpha: Float? = null,
    val shapeType: String? = null,
    val fillColor: String? = null,
    val rotation: Float? = null,
)

fun Element.toDto(): ElementDto = when (this) {
    is Element.Path -> ElementDto(
        id = id,
        type = "Path",
        zIndex = zIndex,
        points = points.map { it.toJsonString() },
        strokeColor = strokeColor.toHexString(),
        strokeWidth = strokeWidth,
        alpha = alpha,
        rotation = rotation.takeIf { it != 0f },
    )
    is Element.Shape -> ElementDto(
        id = id,
        type = "Shape",
        zIndex = zIndex,
        shapeType = shapeType.toTypeString(),
        points = points.map { it.toJsonString() },
        strokeColor = strokeColor.toHexString(),
        strokeWidth = strokeWidth,
        fillColor = fillColor?.toHexString(),
        rotation = rotation.takeIf { it != 0f },
    )
}

fun ElementDto.toElement(): Element = when (type) {
    "Path" -> Element.Path(
        id = id,
        points = points.map { it.toOffset() },
        strokeColor = strokeColor.toColor(),
        strokeWidth = strokeWidth,
        alpha = alpha ?: 1f,
        zIndex = zIndex,
        rotation = rotation ?: 0f,
    )
    "Shape" -> Element.Shape(
        id = id,
        shapeType = shapeType?.toShapeType() ?: ShapeType.RECTANGLE,
        points = points.map { it.toOffset() },
        strokeColor = strokeColor.toColor(),
        fillColor = fillColor?.toColor(),
        strokeWidth = strokeWidth,
        zIndex = zIndex,
        rotation = rotation ?: 0f,
    )
    else -> Element.Path(
        id = id,
        points = points.map { it.toOffset() },
        strokeColor = strokeColor.toColor(),
        strokeWidth = strokeWidth,
        alpha = alpha ?: 1f,
        zIndex = zIndex,
        rotation = rotation ?: 0f,
    )
}

data class DrawingExportDto(
    val bgColor: String,
    val elements: List<ElementDto>,
)

fun PayLoad.toDto(): DrawingExportDto = DrawingExportDto(
    bgColor = bgColor.toHexString(),
    elements = elements.map { it.toDto() },
)

fun DrawingExportDto.toPayLoad(): PayLoad = PayLoad(
    bgColor = bgColor.toColor(),
    elements = elements.map { it.toElement() },
)

@kotlinx.serialization.Serializable
data class SerializableElement(
    val id: String,
    val type: String,
    val zIndex: Int,
    val points: List<String>,
    val strokeColor: String,
    val strokeWidth: Float,
    val alpha: Float? = null,
    val shapeType: String? = null,
    val fillColor: String? = null,
    val rotation: Float? = null,
)

@kotlinx.serialization.Serializable
data class SerializableDrawing(
    val bgColor: String,
    val elements: List<SerializableElement>,
)

object DrawingSerializer {
    private val json = Json { prettyPrint = true }

    fun serialize(payLoad: PayLoad): String {
        val dto = payLoad.toDto()
        val serializableDrawing = SerializableDrawing(
            bgColor = dto.bgColor,
            elements = dto.elements.map { element ->
                SerializableElement(
                    id = element.id,
                    type = element.type,
                    zIndex = element.zIndex,
                    points = element.points,
                    strokeColor = element.strokeColor,
                    strokeWidth = element.strokeWidth,
                    alpha = element.alpha,
                    shapeType = element.shapeType,
                    fillColor = element.fillColor,
                    rotation = element.rotation,
                )
            },
        )
        return json.encodeToString(SerializableDrawing.serializer(), serializableDrawing)
    }

    fun deserialize(jsonString: String): PayLoad {
        val serializableDrawing = json.decodeFromString(SerializableDrawing.serializer(), jsonString)
        val dto = DrawingExportDto(
            bgColor = serializableDrawing.bgColor,
            elements = serializableDrawing.elements.map { element ->
                ElementDto(
                    id = element.id,
                    type = element.type,
                    zIndex = element.zIndex,
                    points = element.points,
                    strokeColor = element.strokeColor,
                    strokeWidth = element.strokeWidth,
                    alpha = element.alpha,
                    shapeType = element.shapeType,
                    fillColor = element.fillColor,
                    rotation = element.rotation,
                )
            },
        )
        return dto.toPayLoad()
    }
}
