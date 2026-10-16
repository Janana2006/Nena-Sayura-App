package lk.nenasayura.app.ui.student.teachercontent

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.data.model.ContentType
import lk.nenasayura.app.ui.theme.IconPalette
import lk.nenasayura.app.ui.theme.NenaIconColors
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaSecondary

/** Shared pieces of the "My teacher" screens (list, materials, payment). */

/** Moves a block up by [amount] so it floats over the bottom of the gradient header. */
internal fun Modifier.pullUp(amount: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val up = amount.roundToPx()
    layout(placeable.width, maxOf(0, placeable.height - up)) {
        placeable.place(0, -up)
    }
}

/**
 * Blue-to-teal header with rounded bottom corners, the same look as the student home screen.
 * Draws behind the status bar. [content] sits under the back button row.
 */
@Composable
internal fun GradientHero(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomPadding: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(NenaPrimary, NenaSecondary)))
            .drawBehind {
                // soft decorative circles
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = size.width * 0.38f,
                    center = Offset(size.width * 0.95f, size.height * 0.05f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = size.width * 0.22f,
                    center = Offset(size.width * 0.02f, size.height * 0.95f)
                )
            }
            .statusBarsPadding()
            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = bottomPadding)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            actions()
        }
        Column(modifier = Modifier.padding(horizontal = 12.dp), content = content)
    }
}

/** A plain gradient header with a title: used while a screen is loading or has an error. */
@Composable
internal fun ScreenHeader(title: String, onBack: (() -> Unit)?) {
    GradientHero(onBack = onBack, bottomPadding = 20.dp) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/** Translucent white label for use on the gradient header. */
@Composable
internal fun HeroChip(text: String) {
    Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.2f)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White
        )
    }
}

/** Horizontally scrolling row of [HeroChip]s. */
@Composable
internal fun HeroChipRow(texts: List<String>) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        texts.forEach { HeroChip(it) }
    }
}

/** Round avatar with the teacher's first letter. [onDark] = white circle (for the gradient header). */
@Composable
internal fun TeacherAvatar(name: String, size: Int = 48, onDark: Boolean = false) {
    val background = if (onDark) {
        Modifier.background(Color.White, CircleShape)
    } else {
        Modifier.background(Brush.linearGradient(listOf(NenaPrimary, NenaSecondary)), CircleShape)
    }
    Box(
        modifier = Modifier
            .size(size.dp)
            .then(background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.trim().firstOrNull()?.uppercase() ?: "T",
            style = if (size >= 56) MaterialTheme.typography.headlineSmall
            else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (onDark) NenaPrimary else Color.White
        )
    }
}

/** Small coloured label with an optional icon ("Free", "Rs. 1,500", "Paid"). */
@Composable
internal fun IconPill(
    text: String,
    background: Color,
    textColor: Color,
    icon: ImageVector? = null
) {
    Surface(shape = RoundedCornerShape(50), color = background) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

/** Round icon on a pale background. */
@Composable
internal fun IconBadge(icon: ImageVector, palette: IconPalette, size: Int = 44) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(palette.container, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = palette.tint, modifier = Modifier.size((size * 0.5f).dp))
    }
}

/** Icon + colour for each kind of material. */
internal class TypeStyle(val icon: ImageVector, val palette: IconPalette)

internal fun ContentType.style(): TypeStyle = when (this) {
    ContentType.SHORT_NOTE -> TypeStyle(Icons.Filled.EditNote, NenaIconColors.GuideBooks)
    ContentType.TUTE -> TypeStyle(Icons.Filled.Book, NenaIconColors.Papers)
    ContentType.MODEL_PAPER -> TypeStyle(Icons.Filled.Quiz, NenaIconColors.MarkingSchemes)
    ContentType.NOTE -> TypeStyle(Icons.Filled.Description, NenaIconColors.ResourceBooks)
    ContentType.VIDEO_LINK -> TypeStyle(
        Icons.Filled.PlayCircle,
        IconPalette(Color(0xFFC62828), Color(0xFFFDE3E3))
    )
}