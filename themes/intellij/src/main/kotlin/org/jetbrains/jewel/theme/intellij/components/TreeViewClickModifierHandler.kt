package org.jetbrains.jewel.theme.intellij.components

import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import org.jetbrains.skiko.hostOs

open class DefaultTreeViewClickModifierHandler : TreeViewClickModifierHandler {

    companion object : DefaultTreeViewClickModifierHandler()

    override fun getMultipleElementClickKeyboardModifier(keyboardModifiers: PointerKeyboardModifiers): Boolean = when {
        hostOs.isWindows || hostOs.isLinux -> keyboardModifiers.isCtrlPressed
        hostOs.isMacOS -> keyboardModifiers.isMetaPressed
        else -> false
    }
}

interface TreeViewClickModifierHandler {

    fun getMultipleElementClickKeyboardModifier(keyboardModifiers: PointerKeyboardModifiers): Boolean
}