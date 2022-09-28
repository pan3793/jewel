package org.jetbrains.jewel.theme.intellij.components

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key

open class DefaultWindowsTreeViewKeybindings : TreeViewKeybindings {

    companion object : DefaultWindowsTreeViewKeybindings()

    override fun selectFirstElement(event: KeyEvent) =
        event.key == Key.Home

    override fun extendSelectionToFirstElement(event: KeyEvent) =
        event.key == Key.Home && event.isShiftPressed

    override fun selectLastElement(event: KeyEvent) =
        event.key == Key.MoveEnd

    override fun extendSelectionToLastElement(event: KeyEvent) =
        event.key == Key.MoveEnd && event.isShiftPressed

    override fun selectPreviousElement(event: KeyEvent) =
        event.key == Key.DirectionUp

    override fun extendSelectionWithPreviousElement(event: KeyEvent) =
        event.key == Key.DirectionUp && event.isShiftPressed

    override fun selectNextElement(event: KeyEvent) =
        event.key == Key.DirectionDown

    override fun extendSelectionWithNextElement(event: KeyEvent) =
        event.key == Key.DirectionDown && event.isShiftPressed

    override fun selectParentElement(event: KeyEvent) =
        event.key == Key.DirectionLeft

    override fun extendSelectionToParentElement(event: KeyEvent) =
        event.key == Key.DirectionLeft && event.isShiftPressed

    override fun selectChildElement(event: KeyEvent) =
        event.key == Key.DirectionRight

    override fun extendSelectionToChildElement(event: KeyEvent) =
        event.key == Key.DirectionRight && event.isShiftPressed

    override fun scrollPageUpAndSelectElement(event: KeyEvent) =
        event.key == Key.PageUp

    override fun scrollPageUpAndExtendSelection(event: KeyEvent) =
        event.key == Key.PageUp && event.isShiftPressed

    override fun scrollPageDownAndSelectElement(event: KeyEvent) =
        event.key == Key.PageDown

    override fun scrollPageDownAndExtendSelection(event: KeyEvent) =
        event.key == Key.PageDown && event.isShiftPressed

    override fun selectNextSiblingElement(event: KeyEvent) = null

    override fun selectPreviousSiblingElement(event: KeyEvent) = null

    override fun editElement(event: KeyEvent) =
        event.key == Key.F2
}

interface TreeViewKeybindings {

    /**
     * Select First Node
     */
    fun selectFirstElement(event: KeyEvent): Boolean?

    /**
     * Extend Selection to First Node inherited from Move Caret to Text Start with Selection
     */
    fun extendSelectionToFirstElement(event: KeyEvent): Boolean?

    /**
     * Select Last Node inherited from Move Caret to Text End
     */
    fun selectLastElement(event: KeyEvent): Boolean?

    /**
     * Extend Selection to Last Node inherited from Move Caret to Text End with Selection
     */
    fun extendSelectionToLastElement(event: KeyEvent): Boolean?

    /**
     * Select Previous Node inherited from Up
     */
    fun selectPreviousElement(event: KeyEvent): Boolean?

    /**
     * Extend Selection with Previous Node inherited from Up with Selection
     */
    fun extendSelectionWithPreviousElement(event: KeyEvent): Boolean?

    /**
     * Select Next Node inherited from Down
     */
    fun selectNextElement(event: KeyEvent): Boolean?

    /**
     * Extend Selection with Next Node inherited from Down with Selection
     */
    fun extendSelectionWithNextElement(event: KeyEvent): Boolean?

    /**
     * Select Parent Node
     */
    fun selectParentElement(event: KeyEvent): Boolean?

    /**
     * Extend Selection to Parent Node inherited from Left with Selection
     */
    fun extendSelectionToParentElement(event: KeyEvent): Boolean?

    /**
     * Select Child Node inherited from Right
     */
    fun selectChildElement(event: KeyEvent): Boolean?

    /**
     * Extend Selection to Child Node inherited from Right with Selection
     */
    fun extendSelectionToChildElement(event: KeyEvent): Boolean?

    /**
     * Scroll Page Up and Select Node inherited from Page Up
     */
    fun scrollPageUpAndSelectElement(event: KeyEvent): Boolean?

    /**
     * Scroll Page Up and Extend Selection inherited from Page Up with Selection
     */
    fun scrollPageUpAndExtendSelection(event: KeyEvent): Boolean?

    /**
     * Scroll Page Down and Select Node inherited from Page Down
     */
    fun scrollPageDownAndSelectElement(event: KeyEvent): Boolean?

    /**
     * Scroll Page Down and Extend Selection inherited from Page Down with Selection
     */
    fun scrollPageDownAndExtendSelection(event: KeyEvent): Boolean?

    /**
     * Select Next Sibling Node
     */
    fun selectNextSiblingElement(event: KeyEvent): Boolean?

    /**
     * Select Previous Sibling Node
     */
    fun selectPreviousSiblingElement(event: KeyEvent): Boolean?

    /**
     * Edit In Node
     */
    fun editElement(event: KeyEvent): Boolean?
}