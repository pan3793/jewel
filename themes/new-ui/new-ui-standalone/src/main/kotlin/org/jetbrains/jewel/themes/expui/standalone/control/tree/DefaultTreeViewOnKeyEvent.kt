package org.jetbrains.jewel.themes.expui.standalone.control.tree

import kotlin.math.max
import kotlin.math.min

open class DefaultTreeViewOnKeyEvent(
    override val keybindings: TreeViewKeybindings,
    private val treeState: TreeState,
    private val animate: Boolean = false,
    private val scrollOffset: Int = 0
) : TreeViewOnKeyEvent {

    override suspend fun onSelectFirstItem(currentIndex: Int) {
        treeState.flattenedTree.firstOrNull()?.let {
            treeState.selectSingleElement(it)
            treeState.focusableLazyListState.focusItem(0, animate, scrollOffset)
        }
    }

    override suspend fun onExtendSelectionToFirst(currentIndex: Int) {
        if (treeState.flattenedTree.isNotEmpty()) {
            buildList {
                for (i in currentIndex downTo 0) {
                    add(treeState.flattenedTree[i])
                }
            }.let {
                treeState.addElementsToSelection(it)
            }

            treeState.focusableLazyListState.focusItem(0, animate, scrollOffset)
        }
    }

    override suspend fun onSelectLastItem(currentIndex: Int) {
        treeState.flattenedTree.lastOrNull()?.let {
            treeState.selectSingleElement(it)
            treeState.focusableLazyListState.focusItem(treeState.flattenedTree.lastIndex, animate, scrollOffset)
        }
    }

    override suspend fun onExtendSelectionToLastItem(currentIndex: Int) {
        if (treeState.flattenedTree.isNotEmpty()) {
            buildList {
                treeState.flattenedTree.listIterator(currentIndex).forEachRemaining {
                    add(element = it)
                }
            }.let {
                treeState.addElementsToSelection(it)
            }
            treeState.focusableLazyListState.focusItem(treeState.flattenedTree.lastIndex, animate, scrollOffset)
        }
    }

    override suspend fun onSelectPreviousItem(currentIndex: Int) {
        treeState.flattenedTree.getOrNull(currentIndex - 1)?.let {
            treeState.selectSingleElement(it)
            treeState.focusableLazyListState.focusItem(currentIndex - 1, animate, scrollOffset)
        }
    }

    override suspend fun onExtendSelectionWithPreviousItem(currentIndex: Int) {
        if (treeState.flattenedTree.isNotEmpty() && currentIndex - 1 >= 0) {
            if (treeState.lastKeyEventUsedMouse) {
                val prevElement = treeState.flattenedTree[currentIndex - 1]
                val prevId = prevElement.idPath()

                if (treeState.selectedElementsMap.containsKey(prevElement.idPath())) {
                    //we are are changing direction so we needs just deselect the current element
                    treeState.selectedElementsMap.remove(treeState.flattenedTree[currentIndex].idPath())
                } else {
                    treeState.selectedElementsMap[prevId] = prevElement
                }
//
            } else {
                treeState.deselectAll()
                treeState.addElementsToSelection(
                    listOf(
                        treeState.flattenedTree[currentIndex],
                        treeState.flattenedTree[currentIndex - 1]
                    )
                )
                treeState.lastKeyEventUsedMouse = true
            }
            treeState.focusItem(currentIndex - 1, animate, scrollOffset)
        }
    }

    override suspend fun onSelectNextItem(currentIndex: Int) {
        treeState.flattenedTree.getOrNull(currentIndex + 1)?.let {
            treeState.selectSingleElement(it)
            treeState.focusableLazyListState.focusItem(currentIndex + 1, animate, scrollOffset)
        }
    }

    override suspend fun onExtendSelectionWithNextItem(currentIndex: Int) {
        val nextFlattenIndex = currentIndex + 1
        if (treeState.flattenedTree.isNotEmpty() && nextFlattenIndex <= treeState.flattenedTree.lastIndex) {
            if (treeState.lastKeyEventUsedMouse) {
                val nextElem = treeState.flattenedTree[nextFlattenIndex]
                val nextId = nextElem.idPath()

                if (treeState.selectedElementsMap.containsKey(nextId)) {
                    //we are are changing direction so we needs just deselect the current element
                    treeState.selectedElementsMap.remove(treeState.flattenedTree[currentIndex].idPath())
                } else {
                    treeState.selectedElementsMap[nextId] = nextElem
                }
            } else {
                treeState.deselectAll()
                treeState.addElementsToSelection(
                    listOf(
                        treeState.flattenedTree[currentIndex],
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
    }

    override suspend fun onScrollPageUpAndSelectItem(currentIndex: Int) {
        val visibleSize = treeState.focusableLazyListState.layoutInfo.visibleItemsInfo.size
        val targetIndex = max(currentIndex - visibleSize, 0)
        treeState.selectSingleElement(treeState.flattenedTree[targetIndex])
        treeState.focusableLazyListState.focusItem(targetIndex, animate, scrollOffset)
    }

    override suspend fun onScrollPageUpAndExtendSelection(currentIndex: Int) {
        val visibleSize = treeState.focusableLazyListState.layoutInfo.visibleItemsInfo.size
        val targetIndex = max(currentIndex - visibleSize, 0)
        treeState.flattenedTree.subList(targetIndex, currentIndex).forEach {
            treeState.toggleElementSelection(it)
        }
        treeState.focusableLazyListState.focusItem(targetIndex, animate, scrollOffset)
    }

    override suspend fun onScrollPageDownAndSelectItem(currentIndex: Int) {
        val firstVisible = treeState.focusableLazyListState.firstVisibleItemIndex
        val visibleSize = treeState.focusableLazyListState.layoutInfo.visibleItemsInfo.size
        val targetIndex = min(firstVisible + visibleSize, treeState.flattenedTree.lastIndex)
        treeState.selectSingleElement(treeState.flattenedTree[targetIndex])
        treeState.focusableLazyListState.focusItem(targetIndex, animate, scrollOffset)
    }

    override suspend fun onScrollPageDownAndExtendSelection(currentIndex: Int) {
        val firstVisible = treeState.focusableLazyListState.firstVisibleItemIndex
        val visibleSize = treeState.focusableLazyListState.layoutInfo.visibleItemsInfo.size
        val targetIndex = min(firstVisible + visibleSize, treeState.flattenedTree.lastIndex)
        treeState.flattenedTree.subList(currentIndex, targetIndex).forEach {
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

    override suspend fun onEdit(currentIndex: Int) {
        //ij with this shortcut just focus the first element with issue
        //unavailable here
    }
}
