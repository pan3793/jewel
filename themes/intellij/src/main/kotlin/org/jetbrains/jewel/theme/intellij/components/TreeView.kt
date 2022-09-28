package org.jetbrains.jewel.theme.intellij.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.times
import org.jetbrains.jewel.theme.intellij.appendIf
import org.jetbrains.jewel.theme.intellij.styles.LocalTreeKeybindings
import org.jetbrains.jewel.theme.intellij.styles.LocalTreeViewClickModifierHandler
import org.jetbrains.jewel.theme.intellij.styles.LocalTreeViewStyle
import org.jetbrains.jewel.theme.intellij.styles.TreeViewState
import org.jetbrains.jewel.theme.intellij.styles.TreeViewStyle
import org.jetbrains.jewel.theme.intellij.styles.updateTreeViewAppearanceTransition
import kotlin.math.max
import kotlin.math.min

@Composable
fun <T> BaseTreeLayout(
    tree: Tree<T>,
    modifier: Modifier = Modifier,
    state: FocusableLazyListState = rememberFocusableLazyListState(),
    style: TreeViewStyle = LocalTreeViewStyle.current,
    treeClickHandler: TreeViewClickModifierHandler = LocalTreeViewClickModifierHandler.current,
    treeKeybindings: TreeViewKeybindings = LocalTreeKeybindings.current,
    onElementClick: (Tree.Element<T>) -> Unit,
    onElementDoubleClick: (Tree.Element<T>) -> Unit,
    onElementSelected: (Tree.Element<T>) -> Unit,
    onMultipleElementSelected: (Set<Tree.Element<T>>) -> Unit,
    onNodeToggle: (Tree.Element.Node<T>) -> Unit,
    rowContent: @Composable RowScope.(Tree.Element<T>) -> Unit
) {
    var isFocused by remember { mutableStateOf(TreeViewState.fromBoolean(false)) }
    val appearance = style.appearance(isFocused)
    val appearanceTransitionState = updateTreeViewAppearanceTransition(appearance)

    FocusableLazyColumn(
        modifier = modifier.background(appearanceTransitionState.background)
            .onFocusChanged { isFocused = TreeViewState.fromBoolean(it.hasFocus) },
        state = state
    ) {
        itemsIndexed(
            items = tree.flattenedTree,
            key = { _, item -> item },
            contentType = { _, item -> if (item.treeElement is Tree.Element.Node) 0 else 1 }
        ) { index, treeElementWithDepth ->
            val (element, depth) = treeElementWithDepth
            Row(
                modifier = Modifier
                    .appendIf(element.isSelected) { background(appearanceTransitionState.selectedBackground) }
                    .onKeyEvent { keyEvent ->
                        keyEvent.handleOnKeyEvent(
                            onElementDoubleClick = onElementDoubleClick,
                            element = element,
                            index = index,
                            onElementSelected = onElementSelected,
                            onMultipleElementSelected = onMultipleElementSelected,
                            tree = tree,
                            onNodeToggle = onNodeToggle,
                            depth = depth,
                            treeKeybindings = treeKeybindings,
                            state = state
                        )
                    }
                    .onClick(
                        keyboardModifiers = { true },
                        onClick = { onElementClick(element) },
                        onDoubleClick = { onElementDoubleClick(element) }
                    )
                    .onClick(
                        keyboardModifiers = { treeClickHandler.getMultipleElementClickKeyboardModifier(this) },
                        onClick = { onMultipleElementSelected(setOf(element)) }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.padding(start = depth * appearance.indentWidth, end = appearance.arrowEndPadding))
                when (element) {
                    is Tree.Element.Leaf -> {
                        Box(modifier = Modifier.alpha(0f).paint(appearance.arrowPainter()))
                        rowContent(element)
                    }

                    is Tree.Element.Node -> {
                        Box(
                            modifier = Modifier.rotate(if (element.isOpen) 90f else 0f)
                                .alpha(if (element.children.isEmpty()) 0f else 1f)
                                .paint(appearance.arrowPainter())
                                .onClick { onNodeToggle(element) }
                        )
                        rowContent(element)
                    }
                }
            }
        }
    }
}

@Composable
fun <T> TreeView(
    tree: Tree<T>,
    onTreeChanged: (Tree<T>) -> Unit,
    modifier: Modifier = Modifier,
    state: FocusableLazyListState = rememberFocusableLazyListState(),
    style: TreeViewStyle = LocalTreeViewStyle.current,
    treeClickHandler: TreeViewClickModifierHandler = LocalTreeViewClickModifierHandler.current,
    treeKeybindings: TreeViewKeybindings = LocalTreeKeybindings.current,
    onLeafDoubleClick: (Tree.Element<T>) -> Unit = { },
    onElementClick: (Tree.Element<T>) -> Unit = { onTreeChanged(tree.selectOnly(it)) },
    onElementDoubleClick: (Tree.Element<T>) -> Unit = {
        when (it) {
            is Tree.Element.Leaf -> onLeafDoubleClick(it)
            is Tree.Element.Node -> onTreeChanged(tree.replaceElement(it, it.copy(isOpen = !it.isOpen)))
        }
    },
    onElementSelected: (Tree.Element<T>) -> Unit = { onTreeChanged(tree.selectElements(setOf(it))) },
    onMultipleElementSelected: (Set<Tree.Element<T>>) -> Unit = { onTreeChanged(tree.selectElements(it.toSet())) },
    onNodeToggle: (Tree.Element.Node<T>) -> Unit = { onTreeChanged(tree.replaceElement(it, it.copy(isOpen = !it.isOpen))) },
    rowContent: @Composable RowScope.(Tree.Element<T>) -> Unit
) {
    BaseTreeLayout(
        tree = tree,
        modifier = modifier,
        state = state,
        style = style,
        treeClickHandler = treeClickHandler,
        treeKeybindings = treeKeybindings,
        onElementClick = onElementClick,
        onElementDoubleClick = onElementDoubleClick,
        onElementSelected = onElementSelected,
        onMultipleElementSelected = onMultipleElementSelected,
        onNodeToggle = onNodeToggle,
        rowContent = rowContent
    )
}

private fun <T> KeyEvent.handleOnKeyEvent(
    onElementDoubleClick: (Tree.Element<T>) -> Unit,
    element: Tree.Element<T>,
    index: Int,
    onElementSelected: (Tree.Element<T>) -> Unit,
    onMultipleElementSelected: (Set<Tree.Element<T>>) -> Unit,
    tree: Tree<T>,
    onNodeToggle: (Tree.Element.Node<T>) -> Unit,
    depth: Int,
    treeKeybindings: TreeViewKeybindings,
    state: FocusableLazyListState
): Boolean {

    fun onElementSelected(index: Int): Boolean {
        onElementSelected(tree.flattenedTree[index].treeElement)
        return true
    }

    fun onMultipleElementSelected(from: Int, to: Int): Boolean {
        onMultipleElementSelected(tree.flattenedTree.subList(from, to).map { it.treeElement }.toSet())
        return true
    }

    fun selectParent(): Boolean {
        var currentIndex = index
        val parentDepth = depth - 1
        while (currentIndex > 0) {
            currentIndex--
            if (tree.flattenedTree[currentIndex].depth == parentDepth) {
                break
            }
        }
        check(currentIndex >= 0) { "Cannot find parent of $element" }
        return onElementSelected(currentIndex)
    }

    fun onToggle(node: Tree.Element.Node<T>): Boolean {
        onNodeToggle(node)
        return true
    }

    return when {
        type != KeyEventType.KeyDown -> false

        key == Key.Enter -> when (element) {
            is Tree.Element.Leaf -> {
                onElementDoubleClick(element)
                true
            }

            is Tree.Element.Node -> onToggle(element)
        }

        index > 0 && key == Key.DirectionUp -> onElementSelected(index - 1)

        index < tree.flattenedTree.lastIndex && key == Key.DirectionDown -> onElementSelected(index + 1)

        key == Key.DirectionRight -> when {
            element is Tree.Element.Node<T> && !element.isOpen -> onToggle(element)
            index < tree.flattenedTree.lastIndex -> onElementSelected(index + 1)
            else -> false
        }

        key == Key.DirectionLeft -> when {
            element is Tree.Element.Node<T> && element.isOpen -> onToggle(element)
            index > 0 -> when (element) {
                !in tree.heads -> selectParent()
                else -> onElementSelected(0)
            }

            else -> false
        }

        treeKeybindings.selectFirstElement(this) == true -> onElementSelected(0)

        treeKeybindings.selectLastElement(this) == true -> onElementSelected(tree.flattenedTree.lastIndex)

        treeKeybindings.scrollPageDownAndSelectElement(this) == true ->
            onElementSelected(min(index + state.layoutInfo.visibleItemsInfo.size, tree.flattenedTree.size))

        treeKeybindings.scrollPageUpAndExtendSelection(this) == true ->
            onElementSelected(max(state.layoutInfo.visibleItemsInfo.size - index, 0))

        treeKeybindings.extendSelectionToFirstElement(this) == true ->
            onMultipleElementSelected(0, index)

        treeKeybindings.extendSelectionToLastElement(this) == true ->
            onMultipleElementSelected(index, tree.flattenedTree.lastIndex)

        else -> false
    }
}
