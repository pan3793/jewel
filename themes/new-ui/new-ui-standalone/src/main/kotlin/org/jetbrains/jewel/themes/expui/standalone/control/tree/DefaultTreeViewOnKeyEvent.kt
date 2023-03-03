package org.jetbrains.jewel.themes.expui.standalone.control.tree

import kotlin.math.max
import kotlin.math.min

open class DefaultTreeViewOnKeyEvent(
    override val keybindings: TreeViewKeybindings,
    private val treeState: TreeState,
    private val animate: Boolean = false,
    private val scrollOffset: Int = 0
) : TreeViewOnKeyEvent {

    override suspend fun onSelectFirstItem(flattenedIndex: Int) {
        treeState.flattenedTree.firstOrNull()?.let {
            treeState.selectSingleElement(it)
            treeState.focusableLazyListState.focusItem(0, animate, scrollOffset)
        }
    }

    override suspend fun onExtendSelectionToFirst(flattenedIndex: Int) {
        if (treeState.flattenedTree.isNotEmpty()) {
            buildList {
                for (i in flattenedIndex downTo 0) {
                    add(treeState.flattenedTree[i])
                }
            }.let {
                treeState.addElementsToSelection(it)
            }

            treeState.focusableLazyListState.focusItem(0, animate, scrollOffset)
        }
    }

    override suspend fun onSelectLastItem(flattenedIndex: Int) {
        treeState.flattenedTree.lastOrNull()?.let {
            treeState.selectSingleElement(it)
            treeState.focusableLazyListState.focusItem(treeState.flattenedTree.lastIndex, animate, scrollOffset)
        }
    }

    override suspend fun onExtendSelectionToLastItem(flattenedIndex: Int) {
        if (treeState.flattenedTree.isNotEmpty()) {
            buildList {
                treeState.flattenedTree.listIterator(flattenedIndex).forEachRemaining {
                    add(element = it)
                }
            }.let {
                treeState.addElementsToSelection(it)
            }
            treeState.focusableLazyListState.focusItem(treeState.flattenedTree.lastIndex, animate, scrollOffset)
        }
    }

    override suspend fun onSelectPreviousItem(flattenedIndex: Int) {
        treeState.flattenedTree.getOrNull(flattenedIndex - 1)?.let {
            treeState.selectSingleElement(it)
            treeState.focusableLazyListState.focusItem(flattenedIndex - 1, animate, scrollOffset)
        }
    }

    override suspend fun onExtendSelectionWithPreviousItem(flattenedIndex: Int) {
        if (treeState.flattenedTree.isNotEmpty() && flattenedIndex - 1 >= 0) {
            if (treeState.lastKeyEventUsedMouse) {
                val prevElement = treeState.flattenedTree[flattenedIndex - 1]
                val prevId = prevElement.idPath()

                if (treeState.selectedElementsMap.containsKey(prevElement.idPath())) {
                    //we are are changing direction so we needs just deselect the current element
                    treeState.selectedElementsMap.remove(treeState.flattenedTree[flattenedIndex].idPath())
                } else {
                    treeState.selectedElementsMap[prevId] = prevElement
                }
//
            } else {
                treeState.deselectAll()
                treeState.addElementsToSelection(
                    listOf(
                        treeState.flattenedTree[flattenedIndex],
                        treeState.flattenedTree[flattenedIndex - 1]
                    )
                )
                treeState.lastKeyEventUsedMouse = true
            }
            treeState.focusItem(flattenedIndex - 1, animate, scrollOffset)
        }
    }

    override suspend fun onSelectNextItem(flattenedIndex: Int) {
        treeState.flattenedTree.getOrNull(flattenedIndex + 1)?.let {
            treeState.selectSingleElement(it)
            treeState.focusableLazyListState.focusItem(flattenedIndex + 1, animate, scrollOffset)
        }
    }

    override suspend fun onExtendSelectionWithNextItem(flattenedIndex: Int) {
        val nextFlattenIndex = flattenedIndex + 1
        if (treeState.flattenedTree.isNotEmpty() && nextFlattenIndex <= treeState.flattenedTree.lastIndex) {
            if (treeState.lastKeyEventUsedMouse) {
                val nextElem = treeState.flattenedTree[nextFlattenIndex]
                val nextId = nextElem.idPath()

                if (treeState.selectedElementsMap.containsKey(nextId)) {
                    //we are are changing direction so we needs just deselect the current element
                    treeState.selectedElementsMap.remove(treeState.flattenedTree[flattenedIndex].idPath())
                } else {
                    treeState.selectedElementsMap[nextId] = nextElem
                }
            } else {
                treeState.deselectAll()
                treeState.addElementsToSelection(
                    listOf(
                        treeState.flattenedTree[flattenedIndex],
                        treeState.flattenedTree[nextFlattenIndex]
                    )
                )
                treeState.lastKeyEventUsedMouse = true
            }
            treeState.focusItem(nextFlattenIndex, animate, scrollOffset)
        }
    }

    override suspend fun onSelectParent(flattenedIndex: Int) {
        treeState.flattenedTree[flattenedIndex].let {
            if (it is Tree.Element.Node && treeState.isNodeOpen(it)) {
                treeState.toggleNode(it)
                treeState.focusableLazyListState.focusItem(flattenedIndex, animate, scrollOffset)
            } else {
                treeState.flattenedTree.getOrNull(flattenedIndex)?.parent?.let {
                    treeState.selectSingleElement(it)
                    treeState.focusableLazyListState.focusItem(treeState.flattenedTree.indexOf(it), animate, scrollOffset)
                }
            }
        }
    }

    override suspend fun onExtendSelectionToParent(flattenedIndex: Int) {
        treeState.flattenedTree.getOrNull(flattenedIndex)?.parent?.let {
            val parentIndex = treeState.flattenedTree.indexOf(it)
            for (index in parentIndex..flattenedIndex) {
                treeState.toggleElementSelection(it)
            }
            treeState.focusableLazyListState.focusItem(parentIndex, animate, scrollOffset)
        }
    }

    override suspend fun onSelectChild(flattenedIndex: Int) {
        treeState.flattenedTree.getOrNull(flattenedIndex)?.let {
            if (it is Tree.Element.Node && !treeState.isNodeOpen(it)) {
                treeState.toggleNode(it)
                treeState.focusableLazyListState.focusItem(flattenedIndex, animate, scrollOffset)
            } else {
                onSelectNextItem(flattenedIndex)
            }
        }
    }

    override suspend fun onExtendSelectionToChild(flattenedIndex: Int) {
        val lastSelectedElementId = treeState.lastSelectedElementId.value
        val lastSelectedIndex = treeState.lastSelectedElementId.value?.let {
            treeState.flattenedTree.indexOf(treeState.selectedElements[it])
        } ?: 0
        treeState.deselectAll()
        for (currentIndex in min(lastSelectedIndex, flattenedIndex)..max(lastSelectedIndex, flattenedIndex)) {
            treeState.addElementToSelection(treeState.flattenedTree[currentIndex])
        }
        treeState.lastSelectedElementId.value = lastSelectedElementId
        //maybe we need to save the last selected element in treeState
        //    val lastSelectedElement= mutableStateOf<Element....
//        val startIndex = treeState.selectedElements.lastOrNull()?.let {
//            treeState.flattenedTree.indexOf(it)
//        } ?: 0
//        if (startIndex < flattenedIndex) {
//            //selecting up
//            for (i in startIndex..flattenedIndex) {
//                val currentElement = treeState.flattenedTree[i]
//                if (!treeState.selectedElements.contains(currentElement))
//                    treeState.selectedElements.add(currentElement)
//            }
//        } else {
//            for (i in startIndex downTo flattenedIndex) {
//                val currentElement = treeState.flattenedTree[i]
//                if (!treeState.selectedElements.contains(currentElement))
//                    treeState.selectedElements.add(currentElement)
//            }
//        }
    }

    override suspend fun onScrollPageUpAndSelectItem(flattenedIndex: Int) {
        val visibleSize = treeState.focusableLazyListState.layoutInfo.visibleItemsInfo.size
        val targetIndex = max(flattenedIndex - visibleSize, 0)
        treeState.selectSingleElement(treeState.flattenedTree[targetIndex])
        treeState.focusableLazyListState.focusItem(targetIndex, animate, scrollOffset)
    }

    override suspend fun onScrollPageUpAndExtendSelection(flattenedIndex: Int) {
        val visibleSize = treeState.focusableLazyListState.layoutInfo.visibleItemsInfo.size
        val targetIndex = max(flattenedIndex - visibleSize, 0)
        treeState.flattenedTree.subList(targetIndex, flattenedIndex).forEach {
            treeState.toggleElementSelection(it)
        }
        treeState.focusableLazyListState.focusItem(targetIndex, animate, scrollOffset)
    }

    override suspend fun onScrollPageDownAndSelectItem(flattenedIndex: Int) {
        val firstVisible = treeState.focusableLazyListState.firstVisibleItemIndex
        val visibleSize = treeState.focusableLazyListState.layoutInfo.visibleItemsInfo.size
        val targetIndex = min(firstVisible + visibleSize, treeState.flattenedTree.lastIndex)
        treeState.selectSingleElement(treeState.flattenedTree[targetIndex])
        treeState.focusableLazyListState.focusItem(targetIndex, animate, scrollOffset)
    }

    override suspend fun onScrollPageDownAndExtendSelection(flattenedIndex: Int) {
        val firstVisible = treeState.focusableLazyListState.firstVisibleItemIndex
        val visibleSize = treeState.focusableLazyListState.layoutInfo.visibleItemsInfo.size
        val targetIndex = min(firstVisible + visibleSize, treeState.flattenedTree.lastIndex)
        treeState.flattenedTree.subList(flattenedIndex, targetIndex).forEach {
            treeState.toggleElementSelection(it)
        }
        treeState.focusableLazyListState.focusItem(targetIndex, animate, scrollOffset)
    }

    override suspend fun onSelectNextSibling(flattenedIndex: Int) {
        treeState.flattenedTree.listIterator(flattenedIndex)
            .asSequence()
            .firstOrNull { it.depth == treeState.flattenedTree[flattenedIndex].depth }
    }

    override suspend fun onSelectPreviousSibling(flattenedIndex: Int) {
        treeState.flattenedTree.subList(0, flattenedIndex)
            .reversed()
            .firstOrNull { it.depth == treeState.flattenedTree[flattenedIndex].depth }
    }

    override suspend fun onEdit(flattenedIndex: Int) {
        //ij with this shortcut just focus the first element with issue
        //unavailable here
    }
}
