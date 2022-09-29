package org.jetbrains.jewel.theme.intellij.components

import androidx.compose.ui.graphics.vector.addPathNodes
import java.io.File
import java.nio.file.Path

fun <T> buildTree(builder: TreeBuilder<T>.() -> Unit): Tree<T> =
    TreeBuilder<T>().apply(builder).build()

class TreeBuilder<T> {

    sealed class Element<T> {
        data class Leaf<T>(val data: T) : Element<T>()
        data class Node<T>(
            val data: T,
            val isOpen: Boolean,
            val childrenGenerator: ChildrenGeneratorScope<T>.() -> Unit
        ) : Element<T>()
    }

    private val heads = mutableListOf<Element<T>>()

    fun addLeaf(data: T) {
        heads.add(Element.Leaf(data))
    }

    fun addNode(data: T, isOpen: Boolean, childrenGenerator: ChildrenGeneratorScope<T>.() -> Unit) {
        heads.add(Element.Node(data, isOpen, childrenGenerator))
    }

    fun add(element: Element<T>) {
        heads.add(element)
    }

    fun build(): Tree<T> {
        val parent = Tree.Parent.Heads<T>()
        val elements = heads.mapIndexed { index, elementBuilder ->
            when (elementBuilder) {
                is Element.Leaf -> Tree.Element.Leaf(elementBuilder.data, 0, index, parent)
                is Element.Node -> Tree.Element.Node(elementBuilder.data, 0, index, parent) { parent ->
                    generateElements(parent, elementBuilder)
                }
            }
        }
        parent.heads = elements
        return Tree(elements)
    }
}

private fun <T> generateElements(
    parent: Tree.Element.Node<T>,
    elementBuilder: TreeBuilder.Element.Node<T>,
): List<Tree.Element<T>> {
    val childrenGeneratorScope = ChildrenGeneratorScope(parent)
    elementBuilder.childrenGenerator(childrenGeneratorScope)
    return childrenGeneratorScope.elements.mapIndexed { index, elementBuilder ->
        when (elementBuilder) {
            is TreeBuilder.Element.Leaf -> Tree.Element.Leaf(
                data = elementBuilder.data,
                depth = parent.depth + 1,
                childIndex = index,
                parent = Tree.Parent.Node(parent)
            )

            is TreeBuilder.Element.Node -> Tree.Element.Node(
                data = elementBuilder.data,
                depth = parent.depth + 1,
                childIndex = index,
                parent = Tree.Parent.Node(parent)
            ) { generateElements(it, elementBuilder) }
                .also { it.isOpen = elementBuilder.isOpen }
        }
    }
}

class ChildrenGeneratorScope<T>(private val _parent: Tree.Element.Node<T>) {

    data class ParentInfo<T>(val data: T, val depth: Int, val index: Int)

    val parent by lazy { ParentInfo(_parent.data, _parent.depth, _parent.childIndex) }

    internal val elements = mutableListOf<TreeBuilder.Element<T>>()

    fun addLeaf(data: T) {
        elements.add(TreeBuilder.Element.Leaf(data))
    }

    fun addNode(data: T, isOpen: Boolean = false, childrenGenerator: ChildrenGeneratorScope<T>.() -> Unit = { }) {
        elements.add(TreeBuilder.Element.Node(data, isOpen, childrenGenerator))
    }

    fun add(element: TreeBuilder.Element<T>) {
        elements.add(element)
    }
}

fun Path.asTree(isOpen: (File) -> Boolean = { false }) = toFile().asTree(isOpen)

fun File.asTree(isOpen: (File) -> Boolean = { false }) = buildTree {
    addNode(this@asTree, isOpen(this@asTree)) {
        generateFileNodes(isOpen)
    }
}

private fun ChildrenGeneratorScope<File>.generateFileNodes(isOpen: (File) -> Boolean) {
    val files = parent.data.listFiles() ?: return
    files.sortedBy { if (it.isDirectory) "a" else "b" }
        .forEach { file ->
            when {
                file.isFile -> addLeaf(file)
                else -> addNode(file, isOpen(file)) { generateFileNodes(isOpen) }
            }
        }
}

fun test() = buildTree {
    addNode(20, false) {
        addLeaf(2)
        addNode(-3, isOpen = false)
    }
}