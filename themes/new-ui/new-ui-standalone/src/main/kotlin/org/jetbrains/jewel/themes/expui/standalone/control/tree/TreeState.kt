package org.jetbrains.jewel.themes.expui.standalone.control.tree

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import org.jetbrains.jewel.themes.expui.standalone.control.lazy.FocusableLazyListStateImpl
import org.jetbrains.jewel.themes.expui.standalone.control.lazy.FocusableState
import kotlin.properties.Delegates

class TreeState(
    val focusableLazyListState: FocusableLazyListStateImpl = FocusableLazyListStateImpl()
) : FocusableState by focusableLazyListState {

    val flattenedTree = mutableStateListOf<Tree.Element<*>>()

    //map of <NodeId,NodeReference>
    internal val selectedElementsMap = mutableStateMapOf<List<Any>, Tree.Element<*>>()
    internal val openNodesMap = mutableStateMapOf<List<Any>, Tree.Element.Node<*>>()

    internal var lastKeyEventUsedMouse = false

    internal var tree: Tree<*> by Delegates.notNull()

    val selectedElements
        get() = selectedElementsMap.toMap()

    val openNodes
        get() = openNodesMap.toMap()

    internal var lastSelectedElementId= mutableStateOf<List<Any>?>(null)

    fun attachTree(tree: Tree<*>) {
        this.tree = tree
        // todo check

        // clear selected element that are not anymore inside the tree
        // refresh open state for nodesv
        refreshFlattenTree()
    }

    fun reloadChildren(node: Tree.Element.Node<*>) {
    }

    // TODO openNodes, openNode, closeNodes, closeNode, selectedElement, ...
    fun openNode(node: Tree.Element.Node<*>, reloadChildren: Boolean = false): Boolean {
        val indexInFlattenTree = flattenedTree.indexOf(node)
        if (indexInFlattenTree < 0) return false
        return doOpenNode(node, reloadChildren)
    }

    fun openNode(nodeId: Int, reloadChildren: Boolean = false): Boolean {
        val indexInFlattenTree = flattenedTree.indexOfFirst { it is Tree.Element.Node && it.id == nodeId }
        if (indexInFlattenTree < 0) return false
        return doOpenNode(flattenedTree[indexInFlattenTree] as Tree.Element.Node<*>, reloadChildren)
    }

    fun closeNode(node: Tree.Element.Node<*>): Boolean {
        val indexInFlattenTree = flattenedTree.indexOf(node)
        if (indexInFlattenTree < 0) return false
        return doCloseNode(node)
    }

    fun closeNode(nodeId: Int): Boolean {
        val indexInFlattenTree = flattenedTree.indexOfFirst { it is Tree.Element.Node && it.id == nodeId }
        if (indexInFlattenTree < 0) return false
        return doCloseNode(flattenedTree[indexInFlattenTree] as Tree.Element.Node<*>)
    }

    private fun doCloseNode(node: Tree.Element.Node<*>): Boolean {
        println("request node close")
        val nodeWasOpen = openNodesMap.remove(node.idPath()) != null
        node.close()
        if (nodeWasOpen) refreshFlattenTree()
        return nodeWasOpen
    }

    private fun doOpenNode(node: Tree.Element.Node<*>, reloadChildren: Boolean): Boolean {
        println("request node opening")
        return if (node in flattenedTree) {
            openNodesMap[node.idPath()] = node
            node.open(reloadChildren)
            refreshFlattenTree()
            true
        } else false
    }

    internal fun refreshFlattenTree() {
        println("REFREEEEEEEEEEEEEEEEEEEEESH")
        flattenedTree.clear()
        flattenedTree.addAll(tree.roots.flatMap { flattenTree(it) })
    }

    private val Tree.Element.Node<*>.isOpen: Boolean
        get() = idPath() in openNodesMap

    private val Tree.Element.Node<*>.isClosed
        get() = !isOpen

    private fun flattenTree(element: Tree.Element<*>): MutableList<Tree.Element<*>> {
        val orderedChildren = mutableListOf<Tree.Element<*>>()
        when (element) {
            is Tree.Element.Node<*> -> {
                orderedChildren.add(element)
                if (!element.isOpen) return orderedChildren
                element.children?.forEach { child ->
                    orderedChildren.addAll(flattenTree(child))
                }
            }

            is Tree.Element.Leaf<*> -> {
                orderedChildren.add(element)
            }
        }
        return orderedChildren
    }

    fun selectSingleElement(element: Tree.Element<*>): Boolean {
        selectedElementsMap.clear()
        selectedElementsMap.put(element.idPath(), element)
        lastSelectedElementId.value=element.idPath()
        return true
    }

    fun addElementsToSelection(elements: List<Tree.Element<*>>) {
        elements.filter { it in flattenedTree }
            .forEach {
                selectedElementsMap[it.idPath()] = it
            }
    }

    fun addElementToSelection(element: Tree.Element<*>) {
        if (element in flattenedTree) {
            selectedElementsMap[element.idPath()] = element
            lastSelectedElementId.value=element.idPath()
        }
    }

    fun toggleElementSelection(element: Tree.Element<*>) {
        if (element.idPath() in selectedElementsMap) {
            deselectElement(element)
        } else {
            addElementToSelection(element)
        }
    }

    fun deselectElement(element: Tree.Element<*>) {
        selectedElementsMap.remove(element.idPath())
    }

    fun deselectAll() {
        selectedElementsMap.clear()
        lastSelectedElementId.value=null
    }

    fun isElementSelected(element: Tree.Element.Node<*>): Boolean {
        return element.idPath() in selectedElementsMap
    }

    fun isNodeOpen(node: Tree.Element.Node<*>) = node.isOpen

    fun toggleNode(node: Tree.Element.Node<*>) = if (node.isOpen) closeNode(node) else openNode(node)
}
