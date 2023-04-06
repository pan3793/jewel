package org.jetbrains.jewel.themes.expui.standalone.control.lazy

import org.jetbrains.jewel.themes.expui.standalone.control.lazy.SelectableKey.Selectable
import org.jetbrains.jewel.themes.expui.standalone.control.tree.SelectableColumnOnKeyEvent

open class DefaultSelectableOnKeyEvent(
    override val keybindings: SelectableColumnKeybindings,
    private val selectableState: SelectableLazyListState,
    private val animate: Boolean = false,
    private val scrollOffset: Int = 0
) : SelectableColumnOnKeyEvent {

    fun indexOfNextSelectable(currentIndex: Int): Int? {
        for (i in currentIndex..selectableState.keys.lastIndex) {
            if (selectableState.keys[i] is Selectable) return i
        }
        return null
    }

    fun indexOfPreviousSelectable(currentIndex: Int): Int? {
        for (i in currentIndex downTo 0) {
            if (selectableState.keys[i] is Selectable) return i
        }
        return null
    }

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
            val prevIndex=indexOfPreviousSelectable(currentIndex)?: return

            if (selectableState.lastKeyEventUsedMouse) {
                selectableState.selectedItemIndexes.contains(currentIndex)
                if(selectableState.selectedItemIndexes.contains(selectableState.keys[prevIndex])){
                    selectableState.deselectSingleElement(currentIndex)
                }else{
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

    override suspend fun onSelectNextItem(itemIndex: Int) {
        treeState.flattenedTree.getOrNull(itemIndex + 1)?.let {
            treeState.selectSingleElement(it)
            treeState.focusableLazyListState.focusItem(itemIndex + 1, animate, scrollOffset)
        }
    }

    override suspend fun onExtendSelectionWithNextItem(itemIndex: Int) {
        val nextFlattenIndex = itemIndex + 1
        if (treeState.flattenedTree.isNotEmpty() && nextFlattenIndex <= treeState.flattenedTree.lastIndex) {
            if (treeState.lastKeyEventUsedMouse) {
                val nextElem = treeState.flattenedTree[nextFlattenIndex]
                val nextId = nextElem.idPath()

                if (treeState.selectedElementsMap.containsKey(nextId)) {
                    //we are are changing direction so we needs just deselect the current element
                    treeState.selectedElementsMap.remove(treeState.flattenedTree[itemIndex].idPath())
                } else {
                    treeState.selectedElementsMap[nextId] = nextElem
                }
            } else {
                treeState.deselectAll()
                treeState.addElementsToSelection(
                    listOf(
                        treeState.flattenedTree[itemIndex],
                        treeState.flattenedTree[nextFlattenIndex]
                    )
                )
                treeState.lastKeyEventUsedMouse = true
            }
            treeState.focusItem(nextFlattenIndex, animate, scrollOffset)
        }
    }

    override suspend fun onSelectParent(itemIndex: Int) {
        treeState.flattenedTree[itemIndex].let {
            if (it is Tree.Element.Node && treeState.isNodeOpen(it)) {
                treeState.toggleNode(it)
                treeState.focusableLazyListState.focusItem(itemIndex, animate, scrollOffset)
            } else {
                treeState.flattenedTree.getOrNull(itemIndex)?.parent?.let {
                    treeState.selectSingleElement(it)
                    treeState.focusableLazyListState.focusItem(treeState.flattenedTree.indexOf(it), animate, scrollOffset)
                }
            }
        }
    }

    override suspend fun onExtendSelectionToParent(itemIndex: Int) {
        treeState.flattenedTree.getOrNull(itemIndex)?.parent?.let {
            val parentIndex = treeState.flattenedTree.indexOf(it)
            for (index in parentIndex..itemIndex) {
                treeState.toggleElementSelection(it)
            }
            treeState.focusableLazyListState.focusItem(parentIndex, animate, scrollOffset)
        }
    }

    override suspend fun onSelectChild(itemIndex: Int) {
        treeState.flattenedTree.getOrNull(itemIndex)?.let {
            if (it is Tree.Element.Node && !treeState.isNodeOpen(it)) {
                treeState.toggleNode(it)
                treeState.focusableLazyListState.focusItem(itemIndex, animate, scrollOffset)
            } else {
                onSelectNextItem(itemIndex)
            }
        }
    }

    override suspend fun onExtendSelectionToChild(itemIndex: Int) {
        val lastSelectedElementId = treeState.lastSelectedElementId.value
        val lastSelectedIndex = treeState.lastSelectedElementId.value?.let {
            treeState.flattenedTree.indexOf(treeState.selectedElements[it])
        } ?: 0
        treeState.deselectAll()
        for (currentIndex in min(lastSelectedIndex, itemIndex)..max(lastSelectedIndex, itemIndex)) {
            treeState.addElementToSelection(treeState.flattenedTree[currentIndex])
        }
        treeState.lastSelectedElementId.value = lastSelectedElementId
        //maybe we need to save the last selected element in treeState
        //    val lastSelectedElement= mutableStateOf<Element....
//        val startIndex = treeState.selectedElements.lastOrNull()?.let {
//            treeState.flattenedTree.indexOf(it)
//        } ?: 0
//        if (startIndex < itemIndex) {
//            //selecting up
//            for (i in startIndex..itemIndex) {
//                val currentElement = treeState.flattenedTree[i]
//                if (!treeState.selectedElements.contains(currentElement))
//                    treeState.selectedElements.add(currentElement)
//            }
//        } else {
//            for (i in startIndex downTo itemIndex) {
//                val currentElement = treeState.flattenedTree[i]
//                if (!treeState.selectedElements.contains(currentElement))
//                    treeState.selectedElements.add(currentElement)
//            }
//        }
    }

    override suspend fun onScrollPageUpAndSelectItem(itemIndex: Int) {
        val visibleSize = treeState.focusableLazyListState.layoutInfo.visibleItemsInfo.size
        val targetIndex = max(itemIndex - visibleSize, 0)
        treeState.selectSingleElement(treeState.flattenedTree[targetIndex])
        treeState.focusableLazyListState.focusItem(targetIndex, animate, scrollOffset)
    }

    override suspend fun onScrollPageUpAndExtendSelection(itemIndex: Int) {
        val visibleSize = treeState.focusableLazyListState.layoutInfo.visibleItemsInfo.size
        val targetIndex = max(itemIndex - visibleSize, 0)
        treeState.flattenedTree.subList(targetIndex, itemIndex).forEach {
            treeState.toggleElementSelection(it)
        }
        treeState.focusableLazyListState.focusItem(targetIndex, animate, scrollOffset)
    }

    override suspend fun onScrollPageDownAndSelectItem(itemIndex: Int) {
        val firstVisible = treeState.focusableLazyListState.firstVisibleItemIndex
        val visibleSize = treeState.focusableLazyListState.layoutInfo.visibleItemsInfo.size
        val targetIndex = min(firstVisible + visibleSize, treeState.flattenedTree.lastIndex)
        treeState.selectSingleElement(treeState.flattenedTree[targetIndex])
        treeState.focusableLazyListState.focusItem(targetIndex, animate, scrollOffset)
    }

    override suspend fun onScrollPageDownAndExtendSelection(itemIndex: Int) {
        val firstVisible = treeState.focusableLazyListState.firstVisibleItemIndex
        val visibleSize = treeState.focusableLazyListState.layoutInfo.visibleItemsInfo.size
        val targetIndex = min(firstVisible + visibleSize, treeState.flattenedTree.lastIndex)
        treeState.flattenedTree.subList(itemIndex, targetIndex).forEach {
            treeState.toggleElementSelection(it)
        }
        treeState.focusableLazyListState.focusItem(targetIndex, animate, scrollOffset)
    }

    override suspend fun onSelectNextSibling(itemIndex: Int) {
        treeState.flattenedTree.listIterator(itemIndex)
            .asSequence()
            .firstOrNull { it.depth == treeState.flattenedTree[itemIndex].depth }
    }

    override suspend fun onSelectPreviousSibling(itemIndex: Int) {
        treeState.flattenedTree.subList(0, itemIndex)
            .reversed()
            .firstOrNull { it.depth == treeState.flattenedTree[itemIndex].depth }
    }

    override suspend fun onEdit(itemIndex: Int) {
        //ij with this shortcut just focus the first element with issue
        //unavailable here
    }
}
