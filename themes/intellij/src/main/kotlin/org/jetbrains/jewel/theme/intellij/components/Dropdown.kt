package org.jetbrains.jewel.theme.intellij.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import org.jetbrains.jewel.theme.intellij.LocalPalette

@Composable
fun Dropdown(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    items: List<DropdownItem>,
    selectedItem: DropdownItem,
    onItemSelected: (DropdownItem) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Column(modifier) {
        Box(
            Modifier
                .border(
                    width = 1.dp,
                    color = LocalPalette.current.controlStroke,
                    shape = RoundedCornerShape(3.dp)
                )
                .widthIn(min = 150.dp)
                .padding(start = 7.dp, top = 4.dp, bottom = 4.dp, end = 4.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    if (enabled) expanded = !expanded
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.background(LocalPalette.current.dropdown.background)
            ) {
                Text(
                    text = selectedItem.name,
                    fontSize = 13.sp,
                    color = LocalPalette.current.dropdown.foreground,
                    modifier = Modifier.weight(1f)
                )
//                Icon(
//                    painter = painterResource("icons/chevron-down.svg"),
//                    contentDescription = null,
//                    tint = Color(0xff6E6E6E),
//                    modifier = Modifier
//                        .widthIn(max = 40.dp)
//                )
            }
        }

        if (expanded) {
            Popup {
                Column(
                    modifier = Modifier
                        .widthIn(75.dp)
                        .background(LocalPalette.current.dropdown.background)
                        .border(1.dp, LocalPalette.current.controlStroke)
                        .shadow(4.dp),
                ) {
                    items.forEach {
                        Text(it.name)
                    }
                }
            }
        }
    }
}

data class DropdownItem(
    val id:Int,
    val name:String
)