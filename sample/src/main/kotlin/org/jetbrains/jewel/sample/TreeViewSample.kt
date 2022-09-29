package org.jetbrains.jewel.sample

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import org.jetbrains.jewel.theme.intellij.IntelliJThemeLight
import org.jetbrains.jewel.theme.intellij.components.Text
import org.jetbrains.jewel.theme.intellij.components.TreeView
import org.jetbrains.jewel.theme.intellij.components.asTree
import java.nio.file.Path

val USER_PATH: Path
    get() = Path.of(System.getProperty("user.dir"))

fun main() = singleWindowApplication(state = (WindowState(width = 1024.dp, height = 1024.dp))) {
    IntelliJThemeLight {
        val tree by remember { mutableStateOf(USER_PATH.asTree()) }
        TreeView(tree = tree) { fileElement ->
            Text(fileElement.data.name)
        }
    }
}