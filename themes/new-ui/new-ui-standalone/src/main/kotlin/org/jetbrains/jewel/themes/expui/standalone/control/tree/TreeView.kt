package org.jetbrains.jewel.themes.expui.standalone.control.tree

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.jewel.themes.expui.standalone.control.Icon
import org.jetbrains.jewel.themes.expui.standalone.control.lazy.FocusableLazyColumn
import org.jetbrains.jewel.themes.expui.standalone.control.lazy.SelectableColumnKeybindings
import org.jetbrains.jewel.themes.expui.standalone.control.lazy.handleSelectableColumnOnKeyEvent
import org.jetbrains.jewel.themes.expui.standalone.control.lazy.itemsIndexed
import org.jetbrains.jewel.themes.expui.standalone.style.LocalAreaColors
import org.jetbrains.jewel.themes.expui.standalone.style.LocalFocusAreaColors
import kotlin.math.max
import kotlin.math.min

@Composable
fun <T> TreeView(
    modifier: Modifier = Modifier,
    tree: Tree<T>,
    treeState: TreeState = rememberTreeState(),
    onElementClick: (Tree.Element<T>) -> Unit = { println("click") },
    onElementDoubleClick: (Tree.Element<T>) -> Unit = { println("double click") },
    keybindings: TreeViewKeybindings = DefaultTreeViewKeybindings(),
    actions: TreeViewOnKeyEvent = DefaultTreeViewOnKeyEvent(keybindings, treeState),
    elementContent: @Composable (Tree.Element<T>) -> Unit,
) {

    LaunchedEffect(tree) {
        treeState.attachTree(tree)
    }

    val scope = rememberCoroutineScope()
    val flattenedTree = treeState.flattenedTree
    FocusableLazyColumn(
        modifier = modifier.background(LocalAreaColors.current.startBackground),
        verticalScroll = true,
        state = treeState.focusableLazyListState,
        onKeyPressed = scope.handleTreeOnKeyEvent(keybindings, actions)
    ) {
        itemsIndexed(
            items = flattenedTree,
            key = { it.idPath() },
            contentType = { it.data }
        ) { element, itemIndex ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(if (element.idPath() in treeState.selectedElementsMap) LocalFocusAreaColors.current.focusColor.copy(alpha = .3f) else Color.Unspecified)
                    .onPointerEvent(PointerEventType.Press) {
                        with(keybindings) {
                            when {
                                it.keyboardModifiers.isKeyboardMultiSelectionKeyPressed && it.keyboardModifiers.isCtrlPressed -> {
                                    println("ctrl and shift pressed on click")
                                }

                                it.keyboardModifiers.isKeyboardMultiSelectionKeyPressed -> {
                                    println("ShiftClicked ")
                                    scope.launch {
                                        actions.onExtendSelectionToChild(itemIndex)
                                    }
                                }

                                it.keyboardModifiers.isCtrlPressed -> {
                                    println("controll pressed")
                                    treeState.lastKeyEventUsedMouse = false
                                    treeState.toggleElementSelection(element)
                                }

                                else -> {
                                    treeState.selectSingleElement(element)
                                    onElementClick(element as Tree.Element<T>)
                                    println("single click")
                                }
                            }
                        }
                    }
                    .onClick(
                        onClick = {},
                        onDoubleClick = {
                            if (element is Tree.Element.Node) {
                                treeState.toggleNode(element)
                            }
                            onElementDoubleClick(element as Tree.Element<T>)
                            println("Double click")
                        }
                    )
            ) {
                when (element) {
                    is Tree.Element.Leaf -> {
                        Box(modifier = Modifier.alpha(0f).width((element.depth * 20).dp))
                        elementContent(element as Tree.Element<T>)
                    }

                    is Tree.Element.Node -> {
                        Box(modifier = Modifier.alpha(0f).width((element.depth * 20).dp))
                        Box(
                            modifier = Modifier.rotate(if (treeState.isElementSelected(element)) 90f else 0f)
                                .alpha(if (element.children?.isEmpty() == true) 0f else 1f)
                                .pointerInput(Unit) {
                                    while (true) {
                                        awaitPointerEventScope {
                                            awaitFirstDown(false)
                                            treeState.openNode(element)
                                            treeState.selectSingleElement(element)
                                            onElementDoubleClick(element as Tree.Element<T>)
                                        }
                                    }
                                }
                        ) {
                            Icon("icons/nodeDropTriangle.svg")
                        }
                        elementContent(element as Tree.Element<T>)
                    }
                }
            }

        }
    }
}

fun CoroutineScope.handleTreeOnKeyEvent(
    keybinding: TreeViewKeybindings,
    actions: TreeViewOnKeyEvent
): KeyEvent.(Int) -> Boolean = lambda@{ focusedIndex ->
    if (type == KeyEventType.KeyUp) return@lambda false
    with(keybinding) {
        with(actions) {
            when {
                handleSelectableColumnOnKeyEvent(keybinding, actions)(focusedIndex) -> return@lambda true
                extendSelectionToChild() ?: false -> launch { onExtendSelectionToChild(focusedIndex) }
                extendSelectionToParent() ?: false -> launch { onExtendSelectionToParent(focusedIndex) }
                selectNextSibling() ?: false -> launch { onSelectNextSibling(focusedIndex) }
                selectPreviousSibling() ?: false -> launch { onSelectPreviousSibling(focusedIndex) }
                selectParent() ?: false -> launch { onSelectParent(focusedIndex) }
                selectChild() ?: false -> launch { onSelectChild(focusedIndex) }
                else -> return@lambda false
            }
        }
    }
    return@lambda true
}
