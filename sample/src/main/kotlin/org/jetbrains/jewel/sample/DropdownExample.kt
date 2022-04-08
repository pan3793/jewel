@file:OptIn(ExperimentalTime::class, ExperimentalComposeUiApi::class, ExperimentalSplitPaneApi::class)

package org.jetbrains.jewel.sample

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.jewel.theme.intellij.IntelliJTheme
import org.jetbrains.jewel.theme.intellij.components.Checkbox
import org.jetbrains.jewel.theme.intellij.components.Dropdown
import org.jetbrains.jewel.theme.intellij.components.DropdownItem
import org.jetbrains.jewel.theme.intellij.components.Surface
import org.jetbrains.jewel.theme.intellij.components.Text
import kotlin.time.ExperimentalTime

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalFoundationApi::class)
fun main() = singleWindowApplication {
    var isDarkTheme by remember { mutableStateOf(true) }
    val dropdownList = listOf(
        DropdownItem(1, "Test"),
        DropdownItem(2, "Test2"),
        DropdownItem(3, "This is a really long combo box item")
    )
    var selectedDropdownItem by remember { mutableStateOf(dropdownList.get(0)) }

    IntelliJTheme(isDarkTheme) {
        Surface {
            Column {
                Row(Modifier.focusable()) {
                    Text("Dark theme:")
                    Checkbox(checked = isDarkTheme, onCheckedChange = { isDarkTheme = it })
                }
                Dropdown(selectedItem = selectedDropdownItem,
                    items = dropdownList,
                    onItemSelected = { selectedDropdownItem = it },
                    enabled = true,
                    modifier = Modifier.widthIn(max = 700.dp)
                )
            }
        }
    }
}