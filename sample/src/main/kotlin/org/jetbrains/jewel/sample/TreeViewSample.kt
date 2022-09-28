package org.jetbrains.jewel.sample

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.singleWindowApplication
import org.jetbrains.jewel.theme.intellij.IntelliJThemeLight
import org.jetbrains.jewel.theme.intellij.components.Text
import org.jetbrains.jewel.theme.intellij.components.TreeView
import org.jetbrains.jewel.theme.intellij.components.asTree
import java.nio.file.Path

val USER_PATH: Path
    get() = Path.of(System.getProperty("user.dir")!!)

fun main() = singleWindowApplication {
    IntelliJThemeLight {
        var tree by remember { mutableStateOf(USER_PATH.asTree()) }
        TreeView(tree, { tree = it }) { fileElement ->
            Text(fileElement.data.name)
        }
    }
}