package org.jetbrains.jewel.themes.expui.standalone.control.tree

import org.jetbrains.jewel.themes.expui.standalone.control.lazy.SelectableColumnKeybindings

interface SelectableColumnOnKeyEvent {

    val keybindings: SelectableColumnKeybindings


    /**
     * Select First Node
     */
    suspend fun onSelectFirstItem(currentIndex: Int)

    /**
     * Extend Selection to First Node inherited from Move Caret to Text Start with Selection
     */
    suspend fun onExtendSelectionToFirst(currentIndex: Int)

    /**
     * Select Last Node inherited from Move Caret to Text End
     */
    suspend fun onSelectLastItem(currentIndex: Int)

    /**
     * Extend Selection to Last Node inherited from Move Caret to Text End with Selection
     */
    suspend fun onExtendSelectionToLastItem(currentIndex: Int)

    /**
     * Select Previous Node inherited from Up
     */
    suspend fun onSelectPreviousItem(currentIndex: Int)

    /**
     * Extend Selection with Previous Node inherited from Up with Selection
     */
    suspend fun onExtendSelectionWithPreviousItem(currentIndex: Int)

    /**
     * Select Next Node inherited from Down
     */
    suspend fun onSelectNextItem(currentIndex: Int)

    /**
     * Extend Selection with Next Node inherited from Down with Selection
     */
    suspend fun onExtendSelectionWithNextItem(currentIndex: Int)

    /**
     * Scroll Page Up and Select Node inherited from Page Up
     */
    suspend fun onScrollPageUpAndSelectItem(currentIndex: Int)

    /**
     * Scroll Page Up and Extend Selection inherited from Page Up with Selection
     */
    suspend fun onScrollPageUpAndExtendSelection(currentIndex: Int)

    /**
     * Scroll Page Down and Select Node inherited from Page Down
     */
    suspend fun onScrollPageDownAndSelectItem(currentIndex: Int)

    /**
     * Scroll Page Down and Extend Selection inherited from Page Down with Selection
     */
    suspend fun onScrollPageDownAndExtendSelection(currentIndex: Int)


    /**
     * Edit In Item
     */
    suspend fun onEdit(currentIndex: Int)

}
