package org.jetbrains.jewel.theme.intellij.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.LinkedList

data class Tree<T> internal constructor(val heads: List<Element<T>>) : Iterable<Tree.Element<T>> {

    override fun iterator(): Iterator<Element<T>> = elementIterator(heads.firstOrNull()) { it.next }

    internal var flattenedTree = toMutableSet()

    private var selectedElements = emptySet<Element<T>>()

    sealed class Element<T> {

        abstract val data: T
        abstract val depth: Int
        abstract val parent: Element<T>?
        internal abstract val childIndex: Int
        var isSelected by mutableStateOf(false)
        abstract var next: Element<T>?
        abstract var previous: Element<T>?

        fun previousElementsIterable() = Iterable { elementIterator(previous) { it.previous } }
        fun nextElementsIterable() = Iterable { elementIterator(next) { it.next } }

        class Leaf<T>(
            override val data: T,
            override val depth: Int,
            override val childIndex: Int,
            override val parent: Element<T>?,
            override var previous: Element<T>?,
            override var next: Element<T>?
        ) : Element<T>()

        class Node<T>(
            override val data: T,
            override val depth: Int,
            override val childIndex: Int,
            override val parent: Element<T>?,
            private val childrenGenerator: (parent: Node<T>) -> List<Element<T>>,
            isOpen: Boolean = false,
            override var next: Element<T>?,
            override var previous: Element<T>?
        ) : Element<T>() {

            internal var isOpen by mutableStateOf(false)
                private set

            var children by mutableStateOf<List<Element<T>>?>(null)
                private set

            init {
                if (isOpen) toggle()
            }

            fun evaluateChildren() {
                children = childrenGenerator(this)
            }

            private fun connectChildren() {
                val children = children ?: return
                if (children.isNotEmpty()) {
                    next?.also {
                        it.previous = children.last()
                        children.last().next = it
                    }
                    next = children.first()
                    children.first().previous = this
                }
            }

            private fun detachChildren() {
                val children = children ?: return
                if (children.isNotEmpty()) {
                    next = children.last().next
                    next?.previous = this
                }
            }

            fun toggle() {
                if (children == null) evaluateChildren()
                if (!isOpen) connectChildren() else detachChildren()
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
        // TODO the element might not be in the tree!
        selectedElements.minus(elements).forEach { it.isSelected = false }
        selectedElements = elements.onEach { it.isSelected = true }
    }

    fun select(element: Element<T>) {
        // TODO the element might not be in the tree!
        selectedElements = selectedElements + element.also { it.isSelected = true }
    }

    fun select(elements: Set<Element<T>>) {
        // TODO the element might not be in the tree!
        selectedElements = selectedElements + elements.onEach { it.isSelected = true }
    }

    fun deselectAll() {
        selectedElements.forEach { it.isSelected = false }
        selectedElements = emptySet()
    }
}

private fun <T> elementIterator(initial: Tree.Element<T>?, next: (Tree.Element<T>) -> Tree.Element<T>?) =
    iterator {
        var current = initial ?: return@iterator
        yield(current)
        while (true) {
            current = next(current) ?: break
            yield(current)
        }
    }