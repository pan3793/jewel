@file:OptIn(ExperimentalFoundationApi::class)

package org.jetbrains.jewel.theme.idea.demo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.jewel.Orientation
import org.jetbrains.jewel.theme.idea.IntelliJTheme
import org.jetbrains.jewel.theme.idea.addComposePanel
import org.jetbrains.jewel.theme.intellij.components.Separator
import org.jetbrains.jewel.theme.intellij.components.Surface
import org.jetbrains.jewel.theme.intellij.components.Text
import org.jetbrains.jewel.theme.intellij.components.Tree
import org.jetbrains.jewel.theme.intellij.components.TreeLayout
import org.jetbrains.jewel.theme.intellij.components.asTree
import java.nio.file.Paths
import kotlin.math.roundToInt

@ExperimentalCoroutinesApi
internal class PKGSDemo : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.addComposePanel("Packages") {
            IntelliJTheme {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {

                        var tree by remember { mutableStateOf(Paths.get(project.basePath ?: System.getProperty("user.dir")).asTree(true)) }

                        Column(Modifier.fillMaxWidth(0.5f)) {
                            Box {
                                val listState = rememberLazyListState()
                                TreeLayout(
                                    tree = tree,
                                    state = listState,
                                    onTreeChanged = { tree = it },
                                    onTreeElementDoubleClick = {
                                        when (it) {
                                            is Tree.Element.Leaf -> println("CIAO ${it.data.absolutePath}")
                                            is Tree.Element.Node -> tree = tree.replaceElement(it, it.copy(isOpen = !it.isOpen))
                                        }
                                    },
                                    content = {
                                        when (it) {
                                            is Tree.Element.Leaf -> Text(it.data.name)
                                            is Tree.Element.Node -> Text("[${it.data.name}]")
                                        }
                                    },
                                )
                                if (listState.layoutInfo.totalItemsCount > listState.layoutInfo.visibleItemsInfo.size) {
                                    VerticalScrollbar(
                                        modifier = Modifier.align(Alignment.CenterEnd).padding(horizontal = 2.dp),
                                        adapter = rememberScrollbarAdapter(listState)
                                    )
                                }
                            }
                        }
                        var offsetX by remember { mutableStateOf(0f) }
                        Separator(
                            modifier = Modifier
                                .offset { IntOffset(offsetX.roundToInt(), 0) }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consumeAllChanges()
                                        offsetX += dragAmount.x
                                    }
                                },
                            orientation = Orientation.Vertical
                        )
                        Column {
                            Box {
                                val listState = rememberLazyListState()
                                TreeLayout(
                                    tree = tree,
                                    state = listState,
                                    onTreeChanged = { tree = it },
                                    onTreeElementDoubleClick = {
                                        when (it) {
                                            is Tree.Element.Leaf -> println("CIAO ${it.data.absolutePath}")
                                            is Tree.Element.Node -> tree = tree.replaceElement(it, it.copy(isOpen = !it.isOpen))
                                        }
                                    },
                                    content = {
                                        when (it) {
                                            is Tree.Element.Leaf -> Text(it.data.name)
                                            is Tree.Element.Node -> Text("[${it.data.name}]")
                                        }
                                    },
                                )
                                if (listState.layoutInfo.totalItemsCount > listState.layoutInfo.visibleItemsInfo.size) {
                                    VerticalScrollbar(
                                        modifier = Modifier.align(Alignment.CenterEnd).padding(horizontal = 2.dp),
                                        adapter = rememberScrollbarAdapter(listState)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
