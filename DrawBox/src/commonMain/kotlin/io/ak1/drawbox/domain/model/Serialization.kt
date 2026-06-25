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

/**
 * Encode a [Element.PathSample] as a comma-separated string. Chosen over a
 * JSON-object form to keep PEN strokes (which can have thousands of samples)
 * small on the wire.
 *
 * Length-adaptive: only emits fields that carry signal.
 *   - 3 parts: "x,y,w"            — position + width only
 *   - 4 parts: "x,y,w,t"          — adds tilt
 *   - 5 parts: "x,y,w,t,a"        — adds tilt + azimuth
 *
 * When only azimuth is present (tilt null), tilt is emitted as the literal
 * `_` so positional parsing stays unambiguous.
 */
fun Element.PathSample.toJsonString(): String {
    val base = "${position.x},${position.y},${width}"
    if (tilt == null && azimuth == null) return base
    val t = tilt?.toString() ?: "_"
    return if (azimuth == null) "$base,$t" else "$base,$t,${azimuth}"
}

fun String.toPathSample(fallbackWidth: Float): Element.PathSample {
    val parts = this.split(",")
    fun parseTilt(s: String): Float? = if (s == "_") null else s.toFloatOrNull()
    return when (parts.size) {
        5 -> Element.PathSample(
            position = Offset(parts[0].toFloat(), parts[1].toFloat()),
            width = parts[2].toFloat(),
            tilt = parseTilt(parts[3]),
            azimuth = parts[4].toFloatOrNull(),
        )
        4 -> Element.PathSample(
            position = Offset(parts[0].toFloat(), parts[1].toFloat()),
            width = parts[2].toFloat(),
            tilt = parseTilt(parts[3]),
        )
        3 -> Element.PathSample(
            position = Offset(parts[0].toFloat(), parts[1].toFloat()),
            width = parts[2].toFloat(),
        )
        2 -> Element.PathSample(
            position = Offset(parts[0].toFloat(), parts[1].toFloat()),
            width = fallbackWidth,
        )
        else -> Element.PathSample(Offset.Zero, fallbackWidth)
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

fun StrokeStyle.toWireString(): String = name

fun String?.toStrokeStyle(): StrokeStyle = when (this) {
    "DASHED" -> StrokeStyle.DASHED
    "DOTTED" -> StrokeStyle.DOTTED
    else -> StrokeStyle.SOLID
}

data class ElementDto(
    val id: String,
    val type: String,
    val zIndex: Int,
    /**
     * Shape position list, encoded as "x,y" strings. Empty for Path elements
     * — paths use [samples] instead so per-sample widths are colocated.
     */
    val points: List<String>,
    val strokeColor: String,
    val strokeWidth: Float,
    val alpha: Float? = null,
    val shapeType: String? = null,
    val fillColor: String? = null,
    val rotation: Float? = null,
    val cornerRadius: Float? = null,
    val strokeStyle: String? = null,
    val bend: String? = null,
    val startBinding: String? = null,
    val endBinding: String? = null,
    val createdAt: Long? = null,
    val modifiedAt: Long? = null,
    /**
     * Path sample list, encoded as "x,y,w" strings. One entry per stroke
     * sample. Only set for `type = "Path"`; null on Shape DTOs.
     */
    val samples: List<String>? = null,
)

fun Element.toDto(): ElementDto = when (this) {
    is Element.Path -> ElementDto(
        id = id,
        type = "Path",
        zIndex = zIndex,
        points = emptyList(),
        samples = samples.map { it.toJsonString() },
        strokeColor = strokeColor.toHexString(),
        strokeWidth = strokeWidth,
        alpha = alpha,
        rotation = rotation.takeIf { it != 0f },
        strokeStyle = strokeStyle.toWireString().takeIf { strokeStyle != StrokeStyle.SOLID },
        createdAt = createdAt.takeIf { it != 0L },
        modifiedAt = modifiedAt.takeIf { it != 0L },
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
        cornerRadius = cornerRadius.takeIf { it != 0f },
        strokeStyle = strokeStyle.toWireString().takeIf { strokeStyle != StrokeStyle.SOLID },
        bend = bend.toJsonString().takeIf { bend != Offset.Zero },
        startBinding = startBinding,
        endBinding = endBinding,
        createdAt = createdAt.takeIf { it != 0L },
        modifiedAt = modifiedAt.takeIf { it != 0L },
    )
}

fun ElementDto.toElement(): Element = when (type) {
    "Path" -> Element.Path(
        id = id,
        // Prefer the new `samples` wire format; fall back to the legacy
        // `points` field so old exports keep loading. Legacy points get a
        // uniform width = strokeWidth.
        samples = (samples?.map { it.toPathSample(fallbackWidth = strokeWidth) }
            ?: points.map { Element.PathSample(it.toOffset(), strokeWidth) }),
        strokeColor = strokeColor.toColor(),
        strokeWidth = strokeWidth,
        alpha = alpha ?: 1f,
        zIndex = zIndex,
        rotation = rotation ?: 0f,
        strokeStyle = strokeStyle.toStrokeStyle(),
        createdAt = createdAt ?: 0L,
        modifiedAt = modifiedAt ?: createdAt ?: 0L,
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
        cornerRadius = cornerRadius ?: 0f,
        strokeStyle = strokeStyle.toStrokeStyle(),
        bend = bend?.toOffset() ?: Offset.Zero,
        startBinding = startBinding,
        endBinding = endBinding,
        createdAt = createdAt ?: 0L,
        modifiedAt = modifiedAt ?: createdAt ?: 0L,
    )
    else -> Element.Path(
        id = id,
        samples = (samples?.map { it.toPathSample(fallbackWidth = strokeWidth) }
            ?: points.map { Element.PathSample(it.toOffset(), strokeWidth) }),
        strokeColor = strokeColor.toColor(),
        strokeWidth = strokeWidth,
        alpha = alpha ?: 1f,
        zIndex = zIndex,
        rotation = rotation ?: 0f,
        strokeStyle = strokeStyle.toStrokeStyle(),
        createdAt = createdAt ?: 0L,
        modifiedAt = modifiedAt ?: createdAt ?: 0L,
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
    val cornerRadius: Float? = null,
    val strokeStyle: String? = null,
    val bend: String? = null,
    val startBinding: String? = null,
    val endBinding: String? = null,
    val createdAt: Long? = null,
    val modifiedAt: Long? = null,
    val samples: List<String>? = null,
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
                    cornerRadius = element.cornerRadius,
                    strokeStyle = element.strokeStyle,
                    bend = element.bend,
                    startBinding = element.startBinding,
                    endBinding = element.endBinding,
                    createdAt = element.createdAt,
                    modifiedAt = element.modifiedAt,
                    samples = element.samples,
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
                    cornerRadius = element.cornerRadius,
                    strokeStyle = element.strokeStyle,
                    bend = element.bend,
                    startBinding = element.startBinding,
                    endBinding = element.endBinding,
                    createdAt = element.createdAt,
                    modifiedAt = element.modifiedAt,
                    samples = element.samples,
                )
            },
        )
        return dto.toPayLoad()
    }
}
