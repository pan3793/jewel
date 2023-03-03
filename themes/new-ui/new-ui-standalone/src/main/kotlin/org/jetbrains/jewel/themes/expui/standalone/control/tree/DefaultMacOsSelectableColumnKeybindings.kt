package org.jetbrains.jewel.themes.expui.standalone.control.tree

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isMetaPressed
import org.jetbrains.jewel.themes.expui.standalone.control.lazy.DefaultSelectableColumnKeybindings

open class DefaultMacOsSelectableColumnKeybindings : DefaultSelectableColumnKeybindings() {
    companion object : DefaultMacOsSelectableColumnKeybindings()

    override val KeyEvent.isKeyboardMultiSelectionKeyPressed: Boolean
        get() = isMetaPressed

    override val PointerKeyboardModifiers.isKeyboardMultiSelectionKeyPressed: Boolean
        get() = isMetaPressed
}

open class DefaultMacOsTreeColumnKeybindings : DefaultTreeViewKeybindings() {
    companion object : DefaultMacOsTreeColumnKeybindings()

    override val KeyEvent.isKeyboardMultiSelectionKeyPressed: Boolean
        get() = isMetaPressed

    override val PointerKeyboardModifiers.isKeyboardMultiSelectionKeyPressed: Boolean
        get() = isMetaPressed
}

