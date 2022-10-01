package org.jetbrains.jewel.theme.intellij.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.rotate
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

@Composable
fun <T> BaseTreeLayout(
    tree: Tree<T>,
    modifier: Modifier = Modifier,
    state: FocusableLazyListState = rememberFocusableLazyListState(),
    style: TreeViewStyle = LocalTreeViewStyle.current,
    treeClickHandler: TreeViewClickModifierHandler = LocalTreeViewClickModifierHandler.current,
    treeKeybindings: TreeViewKeybindings = LocalTreeKeybindings.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onElementClick: (Tree.Element<T>) -> Unit,
    onElementDoubleClick: (Tree.Element<T>) -> Unit,
    onElementSelected: (Tree.Element<T>) -> Unit,
    onMultipleElementSelected: (Set<Tree.Element<T>>) -> Unit,
    onNodeToggle: (Tree.Element.Node<T>) -> Unit,
    rowContent: @Composable RowScope.(Tree.Element<T>) -> Unit
) {
    val isFocused by interactionSource.collectIsFocusedAsState()

    val appearance = style.appearance(TreeViewState.fromBoolean(isFocused))
    val appearanceTransitionState = updateTreeViewAppearanceTransition(appearance)

    FocusableLazyColumn(
        modifier = modifier.background(appearanceTransitionState.background),
        interactionSource = interactionSource,
        state = state
    ) {
        tree.heads.forEach { element ->
            Row(
                modifier = Modifier
                    .appendIf(element.isSelected) { background(appearanceTransitionState.selectedBackground) }
                    .onKeyEvent { keyEvent ->
                        keyEvent.handleOnKeyEvent(
                            onElementDoubleClick = onElementDoubleClick,
                            element = element,
                            onElementSelected = onElementSelected,
                            onMultipleElementSelected = onMultipleElementSelected,
                            tree = tree,
                            onNodeToggle = onNodeToggle,
                            depth = element.depth,
                            treeKeybindings = treeKeybindings,
                            state = state
                        )
                    }
//                    .pointerInput(Unit) {
//                        forEachGesture {
//                            awaitPointerEventScope {
//                                awaitFirstDown(false)
//                                onElementClick(element)
//                            }
//                        }
//                    }
                    .onClick(
                        keyboardModifiers = { true },
                        onClick = { onElementClick(element) },
                        onDoubleClick = { onElementDoubleClick(element) }
                    )
                    .onClick(
                        keyboardModifiers = treeClickHandler,
                        onClick = { onMultipleElementSelected(setOf(element)) }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.padding(start = element.depth * appearance.indentWidth, end = appearance.arrowEndPadding))
                when (element) {
                    is Tree.Element.Leaf -> {
                        Box(modifier = Modifier.alpha(0f).paint(appearance.arrowPainter()))
                        rowContent(element)
                    }

                    is Tree.Element.Node -> {
                        Box(
                            modifier = Modifier.rotate(if (element.isOpen) 90f else 0f)
                                .alpha(if (element.children?.isEmpty() == true) 0f else 1f)
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
    modifier: Modifier = Modifier,
    state: FocusableLazyListState = rememberFocusableLazyListState(),
    style: TreeViewStyle = LocalTreeViewStyle.current,
    treeClickHandler: TreeViewClickModifierHandler = LocalTreeViewClickModifierHandler.current,
    treeKeybindings: TreeViewKeybindings = LocalTreeKeybindings.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onLeafDoubleClick: (Tree.Element<T>) -> Unit = { },
    onElementClick: (Tree.Element<T>) -> Unit = { tree.selectOnly(it) },
    onElementDoubleClick: (Tree.Element<T>) -> Unit = {
        when (it) {
            is Tree.Element.Leaf -> onLeafDoubleClick(it)
            is Tree.Element.Node -> it.toggle()
        }
    },
    onElementSelected: (Tree.Element<T>) -> Unit = onElementClick,
    onMultipleElementSelected: (Set<Tree.Element<T>>) -> Unit = { tree.select(it) },
    onNodeToggle: (Tree.Element.Node<T>) -> Unit = { it.toggle() },
    rowContent: @Composable RowScope.(Tree.Element<T>) -> Unit
) {
    BaseTreeLayout(
        tree = tree,
        modifier = modifier,
        state = state,
        style = style,
        treeClickHandler = treeClickHandler,
        treeKeybindings = treeKeybindings,
        interactionSource = interactionSource,
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

    fun selectParent(): Boolean {
        onElementSelected(element.parent!!)
        return true
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

        key == Key.DirectionUp -> {
            element.previous?.let(onElementSelected)
            true
        }

        key == Key.DirectionDown -> {
            element.next?.let(onElementSelected)
            true
        }

        key == Key.DirectionRight -> {
            when {
                element is Tree.Element.Node<T> && !element.isOpen -> onToggle(element)
                else -> element.next?.let(onElementSelected)
            }
            true
        }

        key == Key.DirectionLeft -> {
            when {
                element is Tree.Element.Node<T> && element.isOpen -> onToggle(element)
                else -> element.parent?.let(onElementSelected)
            }
            true
        }

        treeKeybindings.selectFirstElement(this) == true -> {
            onElementSelected(tree.first())
            true
        }

        treeKeybindings.selectLastElement(this) == true -> {
            onElementSelected(element.nextElementsIterable().last())
            true
        }

        treeKeybindings.scrollPageDownAndSelectElement(this) == true -> {
            onElementSelected(element.nextElementsIterable().take(index + state.layoutInfo.visibleItemsInfo.size).last())
            true
        }

        treeKeybindings.scrollPageUpAndExtendSelection(this) == true -> {
            state.layoutInfo.visibleItemsInfo.size - index
            onMultipleElementSelected(max(state.layoutInfo.visibleItemsInfo.size - index, 0))
            true
        }

        treeKeybindings.extendSelectionToFirstElement(this) == true -> {
            onMultipleElementSelected(
                buildSet {

                }
            )
            true
        }

        treeKeybindings.extendSelectionToLastElement(this) == true ->
            onMultipleElementSelected(index, tree.flattenedTree.lastIndex)

        else -> false
    }
}
