package org.jetbrains.jewel.theme.intellij.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import org.jetbrains.jewel.components.Icon
import org.jetbrains.jewel.theme.intellij.IntelliJTheme
import org.jetbrains.jewel.theme.intellij.LocalPalette
import kotlin.math.roundToInt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Dropdown(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    items: List<DropdownItem>,
    selectedItem: DropdownItem,
    onItemSelected: (DropdownItem) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    var hoveredItemId by remember { mutableStateOf(0) }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier
            .widthIn(min = 150.dp)
            .border(
                width = 1.dp,
                color = LocalPalette.current.controlStroke,
                shape = RoundedCornerShape(3.dp)
            )
            .background(LocalPalette.current.dropdown.background)
            .padding(start = 7.dp, top = 4.dp, bottom = 4.dp, end = 4.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (enabled) expanded = !expanded
            }
    ) {
        var textWidthPx by remember {mutableStateOf(-1f)}
        Text(
            text = selectedItem.name,
            fontSize = 13.sp,
            color = LocalPalette.current.dropdown.foreground,
            modifier = Modifier.padding(end = 20.dp).matchParentSize().onGloballyPositioned { textWidthPx = it.boundsInWindow().width }
        )
        Icon(
            painter = IntelliJTheme.painters.dropdown.arrow(),
            contentDescription = null,
            tint = Color(0xff6E6E6E),
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        if (textWidthPx >= 0f && expanded) {
            val popupPositionProvider = DesktopDropdownMenuPositionProvider(
                DpOffset(0.dp, 0.dp),
                LocalDensity.current
            ) { parentBounds, menuBounds ->
            }
            Popup(popupPositionProvider = popupPositionProvider) {
                Column(
                    modifier = Modifier
                        .widthIn(75.dp)
                        .layout{measurable, _ ->
                            val fixedWidthConstrains = Constraints.fixedWidth(textWidthPx.roundToInt())
                            val placable = measurable.measure(fixedWidthConstrains)
                            layout(placable.width, placable.height) {
                                placable.place(0, 0)
                            }
                        }
                        .background(LocalPalette.current.dropdown.background)
                        .border(1.dp, LocalPalette.current.controlStroke),
                ) {
                    items.forEach {
                        Text(
                            it.name,
                            color = if (it.id == hoveredItemId) LocalPalette.current.dropdown.selectionForeground else LocalPalette.current.dropdown.foreground,
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    expanded = false
                                    onItemSelected(it)
                                }.pointerMoveFilter(
                                    onEnter = {
                                        hoveredItemId = it.id
                                        false
                                    },
                                    onExit = {
                                        hoveredItemId = -1
                                        false
                                    })
                                .background(color = if (it.id == hoveredItemId) LocalPalette.current.dropdown.selectionBackground else LocalPalette.current.dropdown.background)
                        )
                    }
                }
            }
        }
    }
}

data class DropdownItem(
    val id: Int,
    val name: String
)

@Immutable
private data class DesktopDropdownMenuPositionProvider(
    val contentOffset: DpOffset,
    val density: Density,
    val onPositionCalculated: (IntRect, IntRect) -> Unit = { _, _ -> }
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        // The min margin above and below the menu, relative to the screen.
        val verticalMargin = with(density) { 48.dp.roundToPx() }
        // The content offset specified using the dropdown offset parameter.
        val contentOffsetX = with(density) { contentOffset.x.roundToPx() }
        val contentOffsetY = with(density) { contentOffset.y.roundToPx() }

        // Compute horizontal position.
        val toRight = anchorBounds.left + contentOffsetX
        val toLeft = anchorBounds.right - contentOffsetX - popupContentSize.width
        val toDisplayRight = windowSize.width - popupContentSize.width
        val toDisplayLeft = 0
        val x = if (layoutDirection == LayoutDirection.Ltr) {
            sequenceOf(toRight, toLeft, toDisplayRight)
        } else {
            sequenceOf(toLeft, toRight, toDisplayLeft)
        }.firstOrNull {
            it >= 0 && it + popupContentSize.width <= windowSize.width
        } ?: toLeft

        // Compute vertical position.
        val toBottom = maxOf(anchorBounds.bottom + contentOffsetY, verticalMargin)
        val toTop = anchorBounds.top - contentOffsetY - popupContentSize.height
        val toCenter = anchorBounds.top - popupContentSize.height / 2
        val toDisplayBottom = windowSize.height - popupContentSize.height - verticalMargin
        var y = sequenceOf(toBottom, toTop, toCenter, toDisplayBottom).firstOrNull {
            it >= verticalMargin &&
                it + popupContentSize.height <= windowSize.height - verticalMargin
        } ?: toTop

        // Desktop specific vertical position checking
        val aboveAnchor = anchorBounds.top + contentOffsetY
        val belowAnchor = windowSize.height - anchorBounds.bottom - contentOffsetY

        if (belowAnchor >= aboveAnchor) {
            y = anchorBounds.bottom + contentOffsetY
        }

        if (y + popupContentSize.height > windowSize.height) {
            y = windowSize.height - popupContentSize.height
        }

        if (y < 0) {
            y = 0
        }

        onPositionCalculated(
            anchorBounds,
            IntRect(x, y, x + popupContentSize.width, y + popupContentSize.height)
        )
        return IntOffset(x, y)
    }
}