@file:OptIn(ExperimentalEncodingApi::class)

package io.ak1.drawbox.domain.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
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
     * Shape / image position list, encoded as "x,y" strings. Empty for Path
     * elements — paths use [samples] instead so per-sample widths are
     * colocated. For Image, this is `[topLeft, bottomRight]`.
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
     * sample. Only set for `type = "Path"`; null on Shape / Image DTOs.
     */
    val samples: List<String>? = null,
    /**
     * Tri-state Shape stroke toggle. `null` (omitted) and `true` both mean
     * "draw stroke" so legacy exports keep rendering; `false` skips the
     * stroke pass entirely (fill-only shapes).
     */
    val strokeEnabled: Boolean? = null,
    /**
     * Raw encoded image payload, base64-encoded. Only set for Image
     * elements. Inline (rather than external reference) so the JSON file
     * round-trips as a single artifact.
     */
    val imageData: String? = null,
    /** Source image's intrinsic pixel width. Only set for Image elements. */
    val intrinsicWidth: Float? = null,
    /** Source image's intrinsic pixel height. Only set for Image elements. */
    val intrinsicHeight: Float? = null,
    /** Render-time alpha for Image / Text elements (0.0..1.0). */
    val opacity: Float? = null,
    /** Raw text content. Only set for Text elements. */
    val text: String? = null,
    /** Logical font family key resolved by the host's font registry. */
    val fontFamilyKey: String? = null,
    /** Em size in world pixels. */
    val fontSize: Float? = null,
    /** Horizontal alignment inside the wrap box: `LEFT`, `CENTER`, or `RIGHT`. */
    val alignment: String? = null,
    /**
     * World-space top-left corner of a text element's wrap box, encoded as
     * `"x,y"`. Only set for Text elements; Shape / Path / Image use the
     * existing `points` field.
     */
    val textTopLeft: String? = null,
    /** World-space wrap width for a text element. */
    val wrapWidth: Float? = null,
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
    is Element.Image -> ElementDto(
        id = id,
        type = "Image",
        zIndex = zIndex,
        points = points.map { it.toJsonString() },
        // Image has no stroke; emit zeros so the field's contract (always
        // present) is preserved. Loader ignores them.
        strokeColor = "#00000000",
        strokeWidth = 0f,
        rotation = rotation.takeIf { it != 0f },
        createdAt = createdAt.takeIf { it != 0L },
        modifiedAt = modifiedAt.takeIf { it != 0L },
        imageData = Base64.encode(bytes),
        intrinsicWidth = intrinsicSize.width,
        intrinsicHeight = intrinsicSize.height,
        opacity = opacity.takeIf { it != 1f },
    )
    is Element.Text -> ElementDto(
        id = id,
        type = "Text",
        zIndex = zIndex,
        // Text geometry moved off the `points` list onto dedicated wire
        // fields (textTopLeft + wrapWidth). measuredHeight is intentionally
        // not serialized — it's renderer-cached and refreshed on load by
        // the first SyncTextMeasuredHeight pass.
        points = emptyList(),
        // Text has no stroke; reuse strokeColor as the wire slot for the
        // text color so legacy readers still parse a meaningful value.
        // The dedicated `text` / `fontSize` fields below carry the rest.
        strokeColor = color.toHexString(),
        strokeWidth = 0f,
        rotation = rotation.takeIf { it != 0f },
        createdAt = createdAt.takeIf { it != 0L },
        modifiedAt = modifiedAt.takeIf { it != 0L },
        text = text,
        fontFamilyKey = fontFamilyKey,
        fontSize = fontSize,
        alignment = alignment.name,
        opacity = opacity.takeIf { it != 1f },
        textTopLeft = topLeft.toJsonString(),
        wrapWidth = wrapWidth,
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
        strokeEnabled = false.takeIf { !strokeEnabled },
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
    "Image" -> Element.Image(
        id = id,
        bytes = imageData?.let { Base64.decode(it) } ?: ByteArray(0),
        intrinsicSize = Size(
            intrinsicWidth ?: 0f,
            intrinsicHeight ?: 0f,
        ),
        points = points.map { it.toOffset() },
        opacity = opacity ?: 1f,
        zIndex = zIndex,
        rotation = rotation ?: 0f,
        createdAt = createdAt ?: 0L,
        modifiedAt = modifiedAt ?: createdAt ?: 0L,
    )
    "Text" -> {
        val resolvedFontSize = fontSize ?: 24f
        // Prefer the dedicated `textTopLeft` / `wrapWidth` fields; fall back
        // to the first/last of `points` for any (in-development) JSON that
        // predates the two-piece text geometry.
        val resolvedTopLeft = textTopLeft?.toOffset()
            ?: points.firstOrNull()?.toOffset()
            ?: Offset.Zero
        val resolvedWrapWidth = wrapWidth ?: run {
            val pts = points.map { it.toOffset() }
            if (pts.size >= 2) pts.last().x - pts.first().x else 240f
        }
        Element.Text(
            id = id,
            text = text ?: "",
            fontFamilyKey = fontFamilyKey ?: io.ak1.drawbox.domain.model.DEFAULT_FONT_FAMILY_KEY,
            fontSize = resolvedFontSize,
            color = strokeColor.toColor(),
            alignment = when (alignment) {
                "CENTER" -> io.ak1.drawbox.domain.model.TextAlignment.CENTER
                "RIGHT" -> io.ak1.drawbox.domain.model.TextAlignment.RIGHT
                else -> io.ak1.drawbox.domain.model.TextAlignment.LEFT
            },
            topLeft = resolvedTopLeft,
            wrapWidth = resolvedWrapWidth.coerceAtLeast(1f),
            // Single-line guess — the renderer's first layout pass dispatches
            // SyncTextMeasuredHeight to bring this to the actual rendered
            // height on the next frame.
            measuredHeight = (resolvedFontSize * 1.2f).coerceAtLeast(resolvedFontSize),
            opacity = opacity ?: 1f,
            zIndex = zIndex,
            rotation = rotation ?: 0f,
            createdAt = createdAt ?: 0L,
            modifiedAt = modifiedAt ?: createdAt ?: 0L,
        )
    }
    "Shape" -> Element.Shape(
        id = id,
        shapeType = shapeType?.toShapeType() ?: ShapeType.RECTANGLE,
        points = points.map { it.toOffset() },
        strokeColor = strokeColor.toColor(),
        fillColor = fillColor?.toColor(),
        strokeEnabled = strokeEnabled ?: true,
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
    val strokeEnabled: Boolean? = null,
    val imageData: String? = null,
    val intrinsicWidth: Float? = null,
    val intrinsicHeight: Float? = null,
    val opacity: Float? = null,
    val text: String? = null,
    val fontFamilyKey: String? = null,
    val fontSize: Float? = null,
    val alignment: String? = null,
    val textTopLeft: String? = null,
    val wrapWidth: Float? = null,
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
                    strokeEnabled = element.strokeEnabled,
                    imageData = element.imageData,
                    intrinsicWidth = element.intrinsicWidth,
                    intrinsicHeight = element.intrinsicHeight,
                    opacity = element.opacity,
                    text = element.text,
                    fontFamilyKey = element.fontFamilyKey,
                    fontSize = element.fontSize,
                    alignment = element.alignment,
                    textTopLeft = element.textTopLeft,
                    wrapWidth = element.wrapWidth,
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
                    strokeEnabled = element.strokeEnabled,
                    imageData = element.imageData,
                    intrinsicWidth = element.intrinsicWidth,
                    intrinsicHeight = element.intrinsicHeight,
                    opacity = element.opacity,
                    text = element.text,
                    fontFamilyKey = element.fontFamilyKey,
                    fontSize = element.fontSize,
                    alignment = element.alignment,
                    textTopLeft = element.textTopLeft,
                    wrapWidth = element.wrapWidth,
                )
            },
        )
        return dto.toPayLoad()
    }
}
