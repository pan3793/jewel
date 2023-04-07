package org.jetbrains.jewel.themes.expui.standalone.control.lazy

import org.jetbrains.jewel.themes.expui.standalone.control.lazy.SelectableKey.Selectable
import org.jetbrains.jewel.themes.expui.standalone.control.tree.SelectableColumnOnKeyEvent
import kotlin.math.max
import kotlin.math.min

open class DefaultSelectableOnKeyEvent(
    override val keybindings: SelectableColumnKeybindings,
    private val selectableState: SelectableLazyListState,
) : SelectableColumnOnKeyEvent {

    override suspend fun onSelectFirstItem(currentIndex: Int) {
        val firstSelectable = selectableState.keys.indexOfFirst { it is Selectable }
        if (firstSelectable >= 0) selectableState.selectSingleItem(firstSelectable)
    }

    override suspend fun onExtendSelectionToFirst(currentIndex: Int) {
        if (selectableState.keys.isNotEmpty()) {
            buildList {
                for (i in currentIndex downTo 0) {
                    if (selectableState.keys[i] is Selectable) add(i)
                }
            }.let {
                selectableState.addElementsToSelection(it, it.last())
            }
        }
    }

    override suspend fun onSelectLastItem(currentIndex: Int) {
        val lastSelectable = selectableState.keys.indexOfLast { it is Selectable }
        if (lastSelectable >= 0) selectableState.selectSingleItem(lastSelectable)
    }

    override suspend fun onExtendSelectionToLastItem(currentIndex: Int) {
        if (selectableState.keys.isNotEmpty()) {
            val lastKey = selectableState.keys.lastIndex
            buildList {
                for (i in currentIndex..lastKey) {
                    if (selectableState.keys[i] is Selectable) add(element = i)
                }
            }.let {
                selectableState.addElementsToSelection(it)
            }
        }
    }

    override suspend fun onSelectPreviousItem(currentIndex: Int) {
        if (currentIndex - 1 >= 0) {
            for (i in currentIndex downTo 0) {
                if (selectableState.keys[i] is Selectable) {
                    selectableState.selectSingleItem(i)
                    break
                }
            }
        }
    }

    override suspend fun onExtendSelectionWithPreviousItem(currentIndex: Int) {
        if (currentIndex - 1 >= 0) {
            val prevIndex = selectableState.indexOfPreviousSelectable(currentIndex) ?: return

            if (selectableState.lastKeyEventUsedMouse) {
                selectableState.selectedItemIndexes.contains(currentIndex)
                if (selectableState.selectedItemIndexes.contains(selectableState.keys[prevIndex])) {
                    selectableState.deselectSingleElement(currentIndex)
                } else {
                    selectableState.addElementToSelection(prevIndex)
                }
            } else {
                selectableState.deselectAll()
                selectableState.addElementsToSelection(
                    listOf(
                        currentIndex,
                        prevIndex
                    )
                )
                selectableState.lastKeyEventUsedMouse = true
            }
        }
    }

    override suspend fun onSelectNextItem(currentIndex: Int) {
        selectableState.indexOfNextSelectable(currentIndex)?.let {
            selectableState.selectSingleItem(it)
        }
    }

    override suspend fun onExtendSelectionWithNextItem(currentIndex: Int) {
        val nextSelectableIndex = selectableState.indexOfNextSelectable(currentIndex)
        if (nextSelectableIndex != null) {
            if (selectableState.lastKeyEventUsedMouse) {
                if (selectableState.selectedItemIndexes.contains(selectableState.keys[nextSelectableIndex])) {
                    selectableState.deselectSingleElement(currentIndex)
                } else {
                    selectableState.addElementToSelection(nextSelectableIndex)
                }
            } else {
                selectableState.deselectAll()
                selectableState.deselectAll()
                selectableState.addElementsToSelection(
                    listOf(
                        currentIndex,
                        nextSelectableIndex
                    )
                )
                selectableState.lastKeyEventUsedMouse = true
            }
        }
    }

    override suspend fun onScrollPageUpAndSelectItem(currentIndex: Int) {
        val visibleSize = selectableState.delegate.layoutInfo.visibleItemsInfo.size
        val targetIndex = max(currentIndex - visibleSize, 0)
        if (selectableState.keys[targetIndex] !is Selectable) {
            selectableState.indexOfPreviousSelectable(currentIndex) ?: selectableState.indexOfNextSelectable(currentIndex)?.let {
                selectableState.selectSingleItem(it)
            }
        } else selectableState.selectSingleItem(targetIndex)
    }

    override suspend fun onScrollPageUpAndExtendSelection(currentIndex: Int) {

        val visibleSize = selectableState.delegate.layoutInfo.visibleItemsInfo.size
        val targetIndex = max(currentIndex - visibleSize, 0)
        val indexList =
            selectableState.keys.subList(targetIndex, currentIndex).withIndex().filter { it.value is Selectable }.map { it.index }.toList()
        selectableState.toggleElementsToSelection(indexList)
    }

    override suspend fun onScrollPageDownAndSelectItem(currentIndex: Int) {
        val firstVisible = selectableState.delegate.firstVisibleItemIndex
        val visibleSize = selectableState.delegate.layoutInfo.visibleItemsInfo.size
        val targetIndex = min(firstVisible + visibleSize, selectableState.keys.lastIndex)
        if (selectableState.keys[targetIndex] !is Selectable) {
            selectableState.indexOfNextSelectable(currentIndex) ?: selectableState.indexOfPreviousSelectable(currentIndex)?.let {
                selectableState.selectSingleItem(it)
            }
        } else selectableState.selectSingleItem(targetIndex)
    }

    override suspend fun onScrollPageDownAndExtendSelection(currentIndex: Int) {
        val firstVisible = selectableState.delegate.firstVisibleItemIndex
        val visibleSize = selectableState.delegate.layoutInfo.visibleItemsInfo.size
        val targetIndex = min(firstVisible + visibleSize, selectableState.keys.lastIndex)
        val indexList =
            selectableState.keys.subList(currentIndex, targetIndex).withIndex().filter { it.value is Selectable }.map { it.index }.toList()
        selectableState.toggleElementsToSelection(indexList)
    }

    override suspend fun onEdit(currentIndex: Int) {
        //ij with this shortcut just focus the first element with issue
        //unavailable here
    }
}
