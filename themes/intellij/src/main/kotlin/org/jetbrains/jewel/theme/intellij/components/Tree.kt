package org.jetbrains.jewel.theme.intellij.components

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Immutable
data class Tree<T> internal constructor(val heads: List<Element<T>>) {

    internal var selectedElements = emptySet<Element<T>>()

    sealed class Parent<T> {
        data class Heads<T>(var heads: List<Element<T>>? = null) : Parent<T>()
        data class Node<T>(val node: Element.Node<T>) : Parent<T>()
    }

    sealed class Element<T> {

        abstract val data: T
        abstract val depth: Int
        abstract val parent: Parent<T>
        internal abstract val childIndex: Int
        var isSelected by mutableStateOf(false)

        fun nextElement() = nextElement(this)

        internal fun nextElement(original: Element<T>): Element<T> = when (this) {
            is Leaf -> nextIntoParent(original)
            is Node -> when (val children = children) {
                null -> nextIntoParent(original)
                else -> children[0]
            }
        }

        private fun nextIntoParent(original: Element<T>) = when (val parent = parent) {
            is Parent.Heads -> {
                val heads = parent.heads ?: error("Heads have not been initialized.")
                when {
                    childIndex < heads.lastIndex -> heads[childIndex + 1]
                    else -> original
                }
            }

            is Parent.Node -> {
                val children = parent.node.children ?: error("WTF")
                when {
                    childIndex < children.lastIndex -> children[childIndex + 1]
                    else -> parent.node.nextElement(original)
                }
            }
        }

        fun previousElement() = previousElement(this)

        internal fun previousElement(original: Element<T>): Element<T> = when (val parent = parent) {
            is Parent.Heads -> {
                val heads = parent.heads ?: error("Heads have not been initialized.")
                when {
                    childIndex > 0 -> heads[childIndex - 1].lastInto()
                    else -> original
                }
            }
            is Parent.Node -> {
                val children = parent.node.children ?: error("Children have not been initialized")
                when {
                    childIndex > 0 -> children[childIndex - 1].lastInto()
                    else -> parent.node.previousElement(original)
                }
            }
        }

        private fun lastInto(): Element<T> = when (this) {
            is Leaf -> this
            is Node -> children?.lastOrNull()?.lastInto() ?: this
        }

        @Immutable
        data class Leaf<T>(
            override val data: T,
            override val depth: Int,
            override val childIndex: Int,
            override val parent: Parent<T>
        ) : Element<T>()

        @Immutable
        class Node<T> internal constructor(
            override val data: T,
            override val depth: Int,
            override val childIndex: Int,
            override val parent: Parent<T>,
            private val childrenGenerator: (parent: Node<T>) -> List<Element<T>>
        ) : Element<T>() {

            var isOpen by mutableStateOf(false)
            var children by mutableStateOf<List<Element<T>>?>(null)

            fun evaluateChildren() {
                children = childrenGenerator(this)
            }

            fun toggle() {
                isOpen = !isOpen
            }
        }
    }

    fun selectOnly(element: Element<T>) {
        // TODO the element might not be in the tree!
        selectedElements.forEach { it.isSelected = false }
        selectedElements = setOf(element.apply { isSelected = true })
    }

    fun selectOnly(elements: Set<Element<T>>) {
        selectedElements.minus(elements).forEach { it.isSelected = false }
        selectedElements = elements.onEach { it.isSelected = true }
    }

    fun select(element: Element<T>) {
        // TODO the element might not be in the tree!
        selectedElements = selectedElements + element.also { it.isSelected = true }
    }

    fun select(elements: Set<Element<T>>) {
        selectedElements = selectedElements + elements.onEach { it.isSelected = true }
    }

    fun deselectAll() {
        selectedElements.forEach { it.isSelected = false }
        selectedElements = emptySet()
    }
}

