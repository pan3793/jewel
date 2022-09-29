package org.jetbrains.jewel.theme.intellij.components

import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import org.jetbrains.skiko.hostOs

val DefaultWindowsTreeViewClickModifierHandler: TreeViewClickModifierHandler
    get() = {
        when {
            hostOs.isWindows || hostOs.isLinux -> isCtrlPressed
            hostOs.isMacOS -> isMetaPressed
            else -> false
        }
    }

typealias TreeViewClickModifierHandler = PointerKeyboardModifiers.() -> Boolean