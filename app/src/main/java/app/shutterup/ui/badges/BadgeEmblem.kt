package app.shutterup.ui.badges

import android.content.res.Configuration
import android.graphics.BlurMaskFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.shutterup.ui.theme.ShutterUpTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Unknown [badgeId] values render a plain circle. */
@Composable
fun BadgeEmblem(
    badgeId: String,
    unlocked: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val spec = BadgeCatalog[badgeId]
    val family = spec?.familyColor(scheme) ?: scheme.outline
    val ringColor = if (unlocked) family else scheme.outlineVariant
    val symbolColor = family
    val discColor = family.copy(alpha = 0.20f).compositeOver(scheme.surfaceContainerHigh)
    val name = badgeDisplayName(badgeId)
    val state = if (unlocked) "unlocked" else "locked"
    val blurPx = with(LocalDensity.current) { 4.dp.toPx() }
    val surfaceColor = scheme.surface

    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "$name, $state" },
    ) {
        val unit = this.size.minDimension / 96f
        val ringWidth = 3f * unit
        val symbolBox = 52f * unit
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        if (unlocked) {
            val discRadius = (this.size.minDimension - ringWidth * 2f) / 2f
            drawCircle(color = discColor, radius = discRadius, center = center)
        }

        if (spec == null) {
            drawFallbackCircle(center, ringColor, ringWidth)
        } else {
            drawBadgeSymbol(
                spec = spec,
                center = center,
                box = symbolBox,
                color = symbolColor,
                stroke = 2.5f * unit,
                unit = unit,
                unlocked = unlocked,
                blurPx = blurPx,
            )
            drawCircle(
                color = ringColor,
                radius = this.size.minDimension / 2f - ringWidth / 2f,
                center = center,
                style = Stroke(width = ringWidth),
            )
            val dots = spec.tierDots
            if (dots > 0) {
                drawTierDots(
                    count = dots,
                    center = center,
                    ringRadius = this.size.minDimension / 2f - ringWidth / 2f,
                    color = family,
                    knockout = surfaceColor,
                    unit = unit,
                )
            }
        }
    }
}

fun badgeDisplayName(badgeId: String): String {
    val spec = BadgeCatalog[badgeId] ?: return badgeId
    return spec.unlockedName
}

fun badgeLockedHint(badgeId: String): String {
    val spec = BadgeCatalog[badgeId] ?: return badgeId
    return spec.lockedHint
}

fun badgeDescription(badgeId: String): String {
    val spec = BadgeCatalog[badgeId] ?: return ""
    return spec.description
}

fun badgeGridCaption(badgeId: String, unlocked: Boolean): String =
    if (unlocked) badgeDisplayName(badgeId) else badgeLockedHint(badgeId)

data class BadgeSection(
    val title: String,
    val ids: List<String>,
)

val BadgeSections: List<BadgeSection> = listOf(
    BadgeSection(
        title = "Streaks",
        ids = listOf(BadgeIds.STREAK_7, BadgeIds.STREAK_30, BadgeIds.STREAK_100, BadgeIds.STREAK_365),
    ),
    BadgeSection(
        title = "Count",
        ids = listOf(
            BadgeIds.TOTAL_10,
            BadgeIds.TOTAL_50,
            BadgeIds.TOTAL_100,
            BadgeIds.TOTAL_250,
            BadgeIds.TOTAL_500,
        ),
    ),
    BadgeSection(
        title = "Explorer",
        ids = listOf(BadgeIds.EXPLORER_10, BadgeIds.EXPLORER_25),
    ),
    BadgeSection(
        title = "Time of day",
        ids = listOf(BadgeIds.EARLY_BIRD, BadgeIds.NIGHT_OWL),
    ),
    BadgeSection(
        title = "Special",
        ids = listOf(
            BadgeIds.FIRST_LIGHT,
            BadgeIds.PERFECT_MONTH,
            BadgeIds.COMEBACK,
            BadgeIds.ICEBERG,
            BadgeIds.CURATOR,
        ),
    ),
)

fun badgeIdsInSectionOrder(): List<String> = BadgeSections.flatMap { it.ids }

@Composable
internal fun AllBadgesGrid(unlocked: Boolean) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            badgeIdsInSectionOrder().chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { id ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            BadgeEmblem(badgeId = id, unlocked = unlocked, size = 96.dp)
                            Text(
                                text = badgeGridCaption(id, unlocked),
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Preview(name = "unlocked-light", showBackground = true)
@Composable
private fun AllBadgesUnlockedLightPreview() {
    ShutterUpTheme(darkTheme = false) { AllBadgesGrid(unlocked = true) }
}

@Preview(
    name = "unlocked-dark-night",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AllBadgesUnlockedDarkPreview() {
    ShutterUpTheme(darkTheme = true) { AllBadgesGrid(unlocked = true) }
}

@Preview(name = "locked-light", showBackground = true)
@Composable
private fun AllBadgesLockedLightPreview() {
    ShutterUpTheme(darkTheme = false) { AllBadgesGrid(unlocked = false) }
}

@Preview(
    name = "locked-dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AllBadgesLockedDarkPreview() {
    ShutterUpTheme(darkTheme = true) { AllBadgesGrid(unlocked = false) }
}

@Preview(name = "unlocked-font-scale-2", showBackground = true, fontScale = 2f)
@Composable
private fun AllBadgesUnlockedFontScalePreview() {
    ShutterUpTheme(darkTheme = false) { AllBadgesGrid(unlocked = true) }
}

internal enum class BadgeFamily {
    STREAK,
    COUNT,
    EXPLORER,
    EARLY_BIRD,
    NIGHT_OWL,
    SPECIAL,
}

internal enum class BadgeGlyph {
    CHEVRONS,
    APERTURE,
    COMPASS,
    HALF_SUN,
    CRESCENT_STAR,
    RAY_BURST,
    CIRCLE_IN_RING,
    LOOP_ARROW,
    SNOWFLAKE,
    NOTE_CARDS,
}

internal data class BadgeSpec(
    val unlockedName: String,
    val lockedHint: String,
    val description: String,
    val family: BadgeFamily,
    val tierIndex: Int?,
    val glyph: BadgeGlyph,
    val glyphParam: Int,
) {
    /** 250 and 500 share 4 dots; iris blade count already differs. */
    val tierDots: Int = tierIndex?.let { min(it, 4) } ?: 0

    fun familyColor(scheme: ColorScheme): Color = when (family) {
        BadgeFamily.STREAK, BadgeFamily.EARLY_BIRD -> scheme.primary
        BadgeFamily.COUNT, BadgeFamily.SPECIAL -> scheme.secondary
        BadgeFamily.EXPLORER, BadgeFamily.NIGHT_OWL -> scheme.tertiary
    }
}

internal val BadgeCatalog: Map<String, BadgeSpec> = mapOf(
    BadgeIds.FIRST_LIGHT to BadgeSpec(
        unlockedName = "First Light",
        lockedHint = "Complete your first day",
        description = "The first day you complete.",
        family = BadgeFamily.SPECIAL,
        tierIndex = null,
        glyph = BadgeGlyph.RAY_BURST,
        glyphParam = 0,
    ),
    BadgeIds.STREAK_7 to BadgeSpec(
        unlockedName = "Week of Light",
        lockedHint = "Complete 7 days in a row",
        description = "7 days completed in a row.",
        family = BadgeFamily.STREAK,
        tierIndex = 1,
        glyph = BadgeGlyph.CHEVRONS,
        glyphParam = 3,
    ),
    BadgeIds.STREAK_30 to BadgeSpec(
        unlockedName = "Month of Light",
        lockedHint = "Complete 30 days in a row",
        description = "30 days completed in a row.",
        family = BadgeFamily.STREAK,
        tierIndex = 2,
        glyph = BadgeGlyph.CHEVRONS,
        glyphParam = 4,
    ),
    BadgeIds.STREAK_100 to BadgeSpec(
        unlockedName = "Century of Light",
        lockedHint = "Complete 100 days in a row",
        description = "100 days completed in a row.",
        family = BadgeFamily.STREAK,
        tierIndex = 3,
        glyph = BadgeGlyph.CHEVRONS,
        glyphParam = 5,
    ),
    BadgeIds.STREAK_365 to BadgeSpec(
        unlockedName = "Year of Light",
        lockedHint = "Complete 365 days in a row",
        description = "365 days completed in a row.",
        family = BadgeFamily.STREAK,
        tierIndex = 4,
        glyph = BadgeGlyph.CHEVRONS,
        glyphParam = 6,
    ),
    BadgeIds.TOTAL_10 to BadgeSpec(
        unlockedName = "Shutter Count 10",
        lockedHint = "Complete 10 days",
        description = "10 days completed.",
        family = BadgeFamily.COUNT,
        tierIndex = 1,
        glyph = BadgeGlyph.APERTURE,
        glyphParam = 5,
    ),
    BadgeIds.TOTAL_50 to BadgeSpec(
        unlockedName = "Shutter Count 50",
        lockedHint = "Complete 50 days",
        description = "50 days completed.",
        family = BadgeFamily.COUNT,
        tierIndex = 2,
        glyph = BadgeGlyph.APERTURE,
        glyphParam = 6,
    ),
    BadgeIds.TOTAL_100 to BadgeSpec(
        unlockedName = "Shutter Count 100",
        lockedHint = "Complete 100 days",
        description = "100 days completed.",
        family = BadgeFamily.COUNT,
        tierIndex = 3,
        glyph = BadgeGlyph.APERTURE,
        glyphParam = 7,
    ),
    BadgeIds.TOTAL_250 to BadgeSpec(
        unlockedName = "Shutter Count 250",
        lockedHint = "Complete 250 days",
        description = "250 days completed.",
        family = BadgeFamily.COUNT,
        tierIndex = 4,
        glyph = BadgeGlyph.APERTURE,
        glyphParam = 8,
    ),
    BadgeIds.TOTAL_500 to BadgeSpec(
        unlockedName = "Shutter Count 500",
        lockedHint = "Complete 500 days",
        description = "500 days completed.",
        family = BadgeFamily.COUNT,
        tierIndex = 5,
        glyph = BadgeGlyph.APERTURE,
        glyphParam = 9,
    ),
    BadgeIds.EXPLORER_10 to BadgeSpec(
        unlockedName = "Theme Explorer · 10",
        lockedHint = "Complete 10 themes",
        description = "10 distinct themes completed.",
        family = BadgeFamily.EXPLORER,
        tierIndex = 1,
        glyph = BadgeGlyph.COMPASS,
        glyphParam = 8,
    ),
    BadgeIds.EXPLORER_25 to BadgeSpec(
        unlockedName = "Theme Explorer · 25",
        lockedHint = "Complete 25 themes",
        description = "25 distinct themes completed.",
        family = BadgeFamily.EXPLORER,
        tierIndex = 2,
        glyph = BadgeGlyph.COMPASS,
        glyphParam = 16,
    ),
    BadgeIds.EARLY_BIRD to BadgeSpec(
        unlockedName = "Early Bird",
        lockedHint = "Complete 5 days near notification time",
        description = "5 captures within an hour of notification time.",
        family = BadgeFamily.EARLY_BIRD,
        tierIndex = null,
        glyph = BadgeGlyph.HALF_SUN,
        glyphParam = 0,
    ),
    BadgeIds.NIGHT_OWL to BadgeSpec(
        unlockedName = "Night Owl",
        lockedHint = "Complete 5 days after 21:00",
        description = "5 captures after 21:00.",
        family = BadgeFamily.NIGHT_OWL,
        tierIndex = null,
        glyph = BadgeGlyph.CRESCENT_STAR,
        glyphParam = 0,
    ),
    BadgeIds.PERFECT_MONTH to BadgeSpec(
        unlockedName = "Perfect Month",
        lockedHint = "Complete every day of a month",
        description = "Every eligible day of a month. No gaps.",
        family = BadgeFamily.SPECIAL,
        tierIndex = null,
        glyph = BadgeGlyph.CIRCLE_IN_RING,
        glyphParam = 0,
    ),
    BadgeIds.COMEBACK to BadgeSpec(
        unlockedName = "Comeback",
        lockedHint = "Return after quiet days",
        description = "Back after three or more quiet days.",
        family = BadgeFamily.SPECIAL,
        tierIndex = null,
        glyph = BadgeGlyph.LOOP_ARROW,
        glyphParam = 0,
    ),
    BadgeIds.ICEBERG to BadgeSpec(
        unlockedName = "Cool Under Pressure",
        lockedHint = "Let a freeze save a streak",
        description = "A freeze kept a streak alive.",
        family = BadgeFamily.SPECIAL,
        tierIndex = null,
        glyph = BadgeGlyph.SNOWFLAKE,
        glyphParam = 0,
    ),
    BadgeIds.CURATOR to BadgeSpec(
        unlockedName = "Curator",
        lockedHint = "Add notes to 25 entries",
        description = "Notes added to 25 entries.",
        family = BadgeFamily.SPECIAL,
        tierIndex = null,
        glyph = BadgeGlyph.NOTE_CARDS,
        glyphParam = 0,
    ),
)

private fun DrawScope.drawFallbackCircle(
    center: Offset,
    ringColor: Color,
    ringWidth: Float,
) {
    val radius = size.minDimension / 2f - ringWidth / 2f
    drawCircle(
        color = ringColor,
        radius = radius,
        center = center,
        style = Stroke(width = ringWidth),
    )
}

private fun DrawScope.drawBadgeSymbol(
    spec: BadgeSpec,
    center: Offset,
    box: Float,
    color: Color,
    stroke: Float,
    unit: Float,
    unlocked: Boolean,
    blurPx: Float,
) {
    when (spec.glyph) {
        BadgeGlyph.CHEVRONS -> drawStyledPath(
            path = chevronPath(center, box, spec.glyphParam),
            color = color,
            fill = false,
            stroke = stroke,
            unlocked = unlocked,
            blurPx = blurPx,
        )
        BadgeGlyph.APERTURE -> drawAperture(
            center = center,
            box = box,
            blades = spec.glyphParam,
            color = color,
            stroke = stroke,
            unlocked = unlocked,
            blurPx = blurPx,
        )
        BadgeGlyph.COMPASS -> drawStyledPath(
            path = compassPath(center, box, spec.glyphParam),
            color = color,
            fill = true,
            stroke = stroke,
            unlocked = unlocked,
            blurPx = blurPx,
        )
        BadgeGlyph.HALF_SUN -> drawHalfSun(center, box, color, stroke, unlocked, blurPx)
        BadgeGlyph.CRESCENT_STAR -> drawCrescentStar(center, box, color, stroke, unlocked, blurPx)
        BadgeGlyph.RAY_BURST -> drawStyledPath(
            path = rayBurstPath(center, box),
            color = color,
            fill = true,
            stroke = stroke,
            unlocked = unlocked,
            blurPx = blurPx,
        )
        BadgeGlyph.CIRCLE_IN_RING -> drawCircleInRing(center, box, color, stroke, unlocked, blurPx)
        BadgeGlyph.LOOP_ARROW -> drawLoopArrow(center, box, color, stroke, unlocked, blurPx)
        BadgeGlyph.SNOWFLAKE -> drawStyledPath(
            path = snowflakePath(center, box),
            color = color,
            fill = false,
            stroke = stroke,
            unlocked = unlocked,
            blurPx = blurPx,
        )
        BadgeGlyph.NOTE_CARDS -> drawNoteCards(center, box, color, stroke, unit, unlocked, blurPx)
    }
}

private fun DrawScope.drawTierDots(
    count: Int,
    center: Offset,
    ringRadius: Float,
    color: Color,
    knockout: Color,
    unit: Float,
) {
    val spacingDeg = 14f
    val span = spacingDeg * (count - 1)
    val start = 90f - span / 2f
    val radius = 2.2f * unit
    val outline = 1.1f * unit
    for (i in 0 until count) {
        val rad = Math.toRadians((start + i * spacingDeg).toDouble())
        val dotCenter = Offset(
            x = center.x + ringRadius * cos(rad).toFloat(),
            y = center.y + ringRadius * sin(rad).toFloat(),
        )
        drawCircle(color = color, radius = radius, center = dotCenter)
        drawCircle(
            color = knockout,
            radius = radius,
            center = dotCenter,
            style = Stroke(width = outline),
        )
    }
}

private fun DrawScope.drawStyledPath(
    path: Path,
    color: Color,
    fill: Boolean,
    stroke: Float,
    unlocked: Boolean,
    blurPx: Float,
) {
    if (unlocked) {
        if (fill) {
            drawPath(path = path, color = color)
        } else {
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    } else {
        drawBlurredPath(path, color.copy(alpha = 0.40f), fill, stroke, blurPx)
    }
}

private fun DrawScope.drawBlurredPath(
    path: Path,
    color: Color,
    fill: Boolean,
    stroke: Float,
    blurPx: Float,
) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            style = if (fill) {
                android.graphics.Paint.Style.FILL
            } else {
                android.graphics.Paint.Style.STROKE
            }
            strokeWidth = stroke
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
    }
}

private fun chevronPath(center: Offset, box: Float, count: Int): Path {
    val path = Path()
    val compact = (0.78f + 0.04f * (6 - count).coerceAtLeast(0)).coerceIn(0.72f, 0.92f)
    val halfW = box * 0.24f * compact
    val rise = box * 0.11f * compact
    val step = rise * 0.72f
    val span = step * (count - 1)
    val startY = center.y + span / 2f
    for (i in 0 until count) {
        val y = startY - i * step
        path.moveTo(center.x - halfW, y)
        path.lineTo(center.x, y - rise)
        path.lineTo(center.x + halfW, y)
    }
    return path
}

private fun DrawScope.drawAperture(
    center: Offset,
    box: Float,
    blades: Int,
    color: Color,
    stroke: Float,
    unlocked: Boolean,
    blurPx: Float,
) {
    val outer = box * 0.46f
    val inner = box * 0.16f
    val opening = Path().apply { addOval(Rect(center = center, radius = inner)) }
    val bladesPath = Path()
    val step = 2.0 * PI / blades
    val twist = step * 0.28
    for (i in 0 until blades) {
        val a0 = i * step - PI / 2.0
        val a1 = a0 + step * 0.82
        bladesPath.moveTo(polar(center, outer, a0))
        bladesPath.lineTo(polar(center, outer, a1))
        bladesPath.lineTo(polar(center, inner, a1 + twist))
        bladesPath.lineTo(polar(center, inner, a0 + twist))
        bladesPath.close()
    }
    drawStyledPath(bladesPath, color, fill = true, stroke = stroke, unlocked = unlocked, blurPx = blurPx)
    drawStyledPath(opening, color, fill = false, stroke = stroke, unlocked = unlocked, blurPx = blurPx)
}

private fun compassPath(center: Offset, box: Float, points: Int): Path {
    val path = Path()
    val step = 2.0 * PI / points
    val waist = box * 0.07f
    for (i in 0 until points) {
        val angle = i * step - PI / 2.0
        val outer = when {
            points == 8 && i % 2 == 0 -> box * 0.48f
            points == 8 -> box * 0.30f
            i % 4 == 0 -> box * 0.48f
            i % 2 == 0 -> box * 0.34f
            else -> box * 0.22f
        }
        path.moveTo(center)
        path.lineTo(polar(center, waist, angle - PI / 2.0))
        path.lineTo(polar(center, outer, angle))
        path.lineTo(polar(center, waist, angle + PI / 2.0))
        path.close()
    }
    path.addOval(Rect(center = center, radius = box * 0.055f))
    return path
}

private fun rayBurstPath(center: Offset, box: Float): Path {
    val path = Path()
    val hub = box * 0.09f
    path.addOval(Rect(center = center, radius = hub))
    val rayLen = box * 0.46f
    val halfW = box * 0.078f
    path.moveTo(center.x - halfW, center.y - hub * 0.6f)
    path.lineTo(center.x, center.y - rayLen)
    path.lineTo(center.x + halfW, center.y - hub * 0.6f)
    path.close()
    return path
}

private fun snowflakePath(center: Offset, box: Float): Path {
    val path = Path()
    val arm = box * 0.40f
    val branchAt = arm * 0.58f
    val branch = arm * 0.22f
    for (i in 0 until 6) {
        val angle = i * PI / 3.0 - PI / 2.0
        path.moveTo(center)
        path.lineTo(polar(center, arm, angle))
        val joint = polar(center, branchAt, angle)
        path.moveTo(joint)
        path.lineTo(polar(joint, branch, angle - PI / 3.0))
        path.moveTo(joint)
        path.lineTo(polar(joint, branch, angle + PI / 3.0))
    }
    return path
}

private fun DrawScope.drawHalfSun(
    center: Offset,
    box: Float,
    color: Color,
    stroke: Float,
    unlocked: Boolean,
    blurPx: Float,
) {
    val horizonY = center.y + box * 0.10f
    val sunR = box * 0.22f
    val sunCenter = Offset(center.x, horizonY)
    val sun = Path().apply {
        addArc(
            Rect(sunCenter.x - sunR, sunCenter.y - sunR, sunCenter.x + sunR, sunCenter.y + sunR),
            180f,
            180f,
        )
        close()
    }
    val horizon = Path().apply {
        moveTo(center.x - box * 0.42f, horizonY)
        lineTo(center.x + box * 0.42f, horizonY)
    }
    val rays = Path().apply {
        val rayStart = sunR + stroke
        val rayEnd = box * 0.42f
        for (deg in listOf(-60.0, -30.0, 0.0, 30.0, 60.0)) {
            val rad = Math.toRadians(deg - 90.0)
            moveTo(polar(sunCenter, rayStart, rad))
            lineTo(polar(sunCenter, rayEnd, rad))
        }
    }
    drawStyledPath(sun, color, fill = true, stroke = stroke, unlocked = unlocked, blurPx = blurPx)
    drawStyledPath(horizon, color, fill = false, stroke = stroke, unlocked = unlocked, blurPx = blurPx)
    drawStyledPath(rays, color, fill = false, stroke = stroke, unlocked = unlocked, blurPx = blurPx)
}

private fun DrawScope.drawCrescentStar(
    center: Offset,
    box: Float,
    color: Color,
    stroke: Float,
    unlocked: Boolean,
    blurPx: Float,
) {
    val r = box * 0.28f
    val moon = Path().apply { addOval(Rect(center = Offset(center.x - box * 0.04f, center.y), radius = r)) }
    val cut = Path().apply {
        addOval(Rect(center = Offset(center.x + box * 0.10f, center.y - box * 0.04f), radius = r * 0.78f))
    }
    val crescent = Path().apply { op(moon, cut, PathOperation.Difference) }
    val starCenter = Offset(center.x + box * 0.22f, center.y - box * 0.22f)
    val star = fourPointStar(starCenter, box * 0.10f, box * 0.04f)
    drawStyledPath(crescent, color, fill = true, stroke = stroke, unlocked = unlocked, blurPx = blurPx)
    drawStyledPath(star, color, fill = true, stroke = stroke, unlocked = unlocked, blurPx = blurPx)
}

private fun DrawScope.drawCircleInRing(
    center: Offset,
    box: Float,
    color: Color,
    stroke: Float,
    unlocked: Boolean,
    blurPx: Float,
) {
    val ring = Path().apply { addOval(Rect(center = center, radius = box * 0.36f)) }
    val inner = Path().apply { addOval(Rect(center = center, radius = box * 0.16f)) }
    drawStyledPath(ring, color, fill = false, stroke = stroke, unlocked = unlocked, blurPx = blurPx)
    drawStyledPath(inner, color, fill = true, stroke = stroke, unlocked = unlocked, blurPx = blurPx)
}

private fun DrawScope.drawLoopArrow(
    center: Offset,
    box: Float,
    color: Color,
    stroke: Float,
    unlocked: Boolean,
    blurPx: Float,
) {
    val r = box * 0.26f
    val loop = Path().apply { addOval(Rect(center = center, radius = r)) }
    val a = 0.25
    val tip = polar(center, r + box * 0.12f, a + 0.9)
    val inner = polar(center, r + box * 0.04f, a)
    val outer = polar(center, r + box * 0.16f, a + 0.18)
    val arrow = Path().apply {
        moveTo(tip)
        lineTo(inner)
        lineTo(outer)
        close()
    }
    drawStyledPath(loop, color, fill = false, stroke = stroke * 1.2f, unlocked = unlocked, blurPx = blurPx)
    drawStyledPath(arrow, color, fill = true, stroke = stroke, unlocked = unlocked, blurPx = blurPx)
}

private fun DrawScope.drawNoteCards(
    center: Offset,
    box: Float,
    color: Color,
    stroke: Float,
    unit: Float,
    unlocked: Boolean,
    blurPx: Float,
) {
    val w = box * 0.36f
    val h = box * 0.44f
    val offset = box * 0.10f
    val corner = CornerRadius(4f * unit, 4f * unit)
    val back = Path().apply {
        addRoundRect(
            RoundRect(
                left = center.x - w * 0.55f,
                top = center.y - h * 0.55f,
                right = center.x - w * 0.55f + w,
                bottom = center.y - h * 0.55f + h,
                cornerRadius = corner,
            ),
        )
    }
    val front = Path().apply {
        addRoundRect(
            RoundRect(
                left = center.x - w * 0.55f + offset,
                top = center.y - h * 0.55f + offset,
                right = center.x - w * 0.55f + offset + w,
                bottom = center.y - h * 0.55f + offset + h,
                cornerRadius = corner,
            ),
        )
    }
    val frontLeft = center.x - w * 0.55f + offset
    val frontTop = center.y - h * 0.55f + offset
    val lines = Path().apply {
        val left = frontLeft + w * 0.18f
        val right = frontLeft + w * 0.78f
        val y1 = frontTop + h * 0.36f
        val y2 = frontTop + h * 0.56f
        moveTo(left, y1)
        lineTo(right, y1)
        moveTo(left, y2)
        lineTo(right, y2)
    }
    drawStyledPath(back, color, fill = false, stroke = stroke, unlocked = unlocked, blurPx = blurPx)
    drawStyledPath(front, color, fill = false, stroke = stroke, unlocked = unlocked, blurPx = blurPx)
    drawStyledPath(lines, color, fill = false, stroke = stroke, unlocked = unlocked, blurPx = blurPx)
}

private fun fourPointStar(center: Offset, outer: Float, inner: Float): Path {
    val path = Path()
    for (i in 0 until 8) {
        val angle = i * PI / 4.0 - PI / 2.0
        val radius = if (i % 2 == 0) outer else inner
        val point = polar(center, radius, angle)
        if (i == 0) path.moveTo(point) else path.lineTo(point)
    }
    path.close()
    return path
}

private fun polar(origin: Offset, radius: Float, angle: Double): Offset =
    Offset(
        x = origin.x + radius * cos(angle).toFloat(),
        y = origin.y + radius * sin(angle).toFloat(),
    )

private fun Path.moveTo(offset: Offset) = moveTo(offset.x, offset.y)

private fun Path.lineTo(offset: Offset) = lineTo(offset.x, offset.y)
