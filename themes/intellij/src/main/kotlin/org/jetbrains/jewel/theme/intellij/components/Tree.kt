package org.jetbrains.jewel.theme.intellij.components

import androidx.compose.runtime.Immutable
import java.io.File
import java.nio.file.Path

@Immutable
data class Tree<T>(val heads: List<Element<T>>) {

    sealed class Element<T> {

        abstract val data: T
        abstract val isSelected: Boolean

        abstract fun withSelection(isSelected: Boolean = this.isSelected): Element<T>

        @Immutable
        data class Leaf<T>(override val data: T, override val isSelected: Boolean) : Element<T>() {

            override fun withSelection(isSelected: Boolean) = copy(isSelected = isSelected)
        }

        @Immutable
        data class Node<T>(
            override val data: T,
            override val isSelected: Boolean,
            val isOpen: Boolean = false,
            val children: List<Element<T>>
        ) : Element<T>() {

            override fun withSelection(isSelected: Boolean) = copy(isSelected = isSelected)
        }
    }

    data class ElementWithDepth<T>(val treeElement: Element<T>, val depth: Int)

    val flattenedTree = buildList {
        val stack = heads.map { it.withDepth(0) }.toMutableList()
        while (stack.isNotEmpty()) {
            val next = stack.removeAt(0)
            add(next)
            if (next.treeElement is Element.Node<T> && next.treeElement.isOpen) {
                stack.addAll(0, next.treeElement.children.map { it.withDepth(next.depth + 1) })
            }
        }
    }

    /**
     * Replaces the first occurrence of [old] in this [Tree] while traversing in depth first.
     *
     * @return Always a new [Tree], eventually with [old] replaced with [new].
     */
    fun replaceElement(old: Element<T>, new: Element<T>): Tree<T> = if (old != new)
        Tree(heads.map { replaceRecursive(old, new, it, ItemFound(false)) })
    else this

    fun selectOnly(element: Element<T>) =
        Tree(heads.map { replaceAndApplyOnAllRecursive(it) { if (it == element) it.withSelection(true) else it.withSelection(false) } })

    fun selectElements(elements: Set<Element<T>>) = if (elements.isNotEmpty())
        Tree(heads.map { replaceAndApplyOnAllRecursive(it) { if (it in elements) it.withSelection(true) else it } })
    else this

    private data class ItemFound(var value: Boolean)

    private fun replaceAndApplyOnAllRecursive(
        current: Element<T>,
        action: (Element<T>) -> Element<T>
    ): Element<T> = action(current).let {
        when (it) {
            is Element.Leaf -> it
            is Element.Node -> it.copy(children = it.children.map { replaceAndApplyOnAllRecursive(it, action) })
        }
    }

    private fun replaceRecursive(
        old: Element<T>,
        new: Element<T>,
        current: Element<T>,
        found: ItemFound
    ): Element<T> = when {
        found.value -> current
        current == old -> {
            found.value = true
            new
        }

        current is Element.Node<T> -> current.copy(children = current.children.map { replaceRecursive(old, new, it, found) })
        else -> current
    }
}

fun <T> Tree(head: Tree.Element<T>) = Tree(listOf(head))
fun <T> Tree.Element<T>.withDepth(depth: Int) =
    Tree.ElementWithDepth(this, depth)

fun File.asTree(isOpen: Boolean = false) = Tree(asTreeElement(isOpen))
fun Path.asTree(isOpen: Boolean = false) = Tree(toFile().asTreeElement(isOpen))
fun File.asTreeElement(isOpen: Boolean = false): Tree.Element<File> =
    if (isFile) Tree.Element.Leaf(this, false) else Tree.Element.Node(
        data = this,
        isSelected = false,
        isOpen = isOpen,
        children = listFiles()?.sortedBy {
            when {
                it.isDirectory -> "a"
                else -> "b"
            } + it.name
        }?.map { it.asTreeElement(isOpen) } ?: emptyList()
    )