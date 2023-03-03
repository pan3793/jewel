package org.jetbrains.jewel.themes.expui.standalone.control.lazy

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.jewel.themes.expui.standalone.control.tree.SelectableColumnOnKeyEvent
import java.util.UUID

@Composable
fun SelectableLazyColumn(
    modifier: Modifier = Modifier,
    verticalScroll: Boolean = false,
    state: SelectableLazyListState = rememberSelectableLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    keybindings: SelectableColumnKeybindings = DefaultSelectableColumnKeybindings,
    actions: SelectableColumnOnKeyEvent,
    content: SelectableLazyListScope.() -> Unit
) {
    val container = remember(content) { SelectableLazyListScopeDelegate(state).apply(content) }
    val uiId = remember { UUID.randomUUID().toString() }
    SideEffect { state.attachKeys(container.keys, uiId) }
    FocusableLazyColumn(
        modifier = modifier,
        verticalScroll = verticalScroll,
        state = state.delegate,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        interactionSource = interactionSource,
        onKeyPressed = rememberCoroutineScope().handleSelectableColumnOnKeyEvent(keybindings, actions),
        content = container.entries
    )
}

class SelectableLazyListState(
    internal val delegate: FocusableLazyListState = FocusableLazyListState()
) : FocusableState by delegate {

    internal val selectedIdsMap = mutableStateMapOf<Any?, Int>()
    internal var keys = emptyList<Any?>()

    internal val selectedItemIndexesState = derivedStateOf { selectedIdsMap.values }

    private var uiId: String? = null

    val selectedItemIndexes: Collection<Int>
        get() = selectedItemIndexesState.value

    val lastFocusedIndex
        get() = delegate.lastFocusedIndex

    val layoutInfo
        get() = delegate.layoutInfo

    /** The index of the first item that is visible */
    val firstVisibleItemIndex
        get() = delegate.firstVisibleItemIndex

    /**
     * The scroll offset of the first visible item. Scrolling forward is
     * positive - i.e., the amount that the item is offset backwards
     */
    val firstVisibleItemScrollOffset
        get() = delegate.firstVisibleItemScrollOffset

    /**
     * [InteractionSource] that will be used to dispatch drag events when
     * this list is being dragged. If you want to know whether the fling (or
     * animated scroll) is in progress, use [isScrollInProgress].
     */
    val interactionSource
        get() = delegate.interactionSource

    suspend fun selectSingleItem(itemIndex: Int) {
        focusItem(itemIndex)
        deselectAll()
        selectedIdsMap[keys[itemIndex]] = itemIndex
    }

    suspend fun deselectSingleElement(itemIndex: Int) {
        focusItem(itemIndex)
        selectedIdsMap.remove(keys[itemIndex])
    }

    suspend fun toggleSelection(itemIndex: Int) {
        selectedIdsMap[keys[itemIndex]]?.let {
            deselectSingleElement(itemIndex)
        } ?: addElementToSelection(itemIndex)
    }

    suspend fun addElementToSelection(itemIndex: Int) {
        focusItem(itemIndex)
        selectedIdsMap[keys[itemIndex]] = itemIndex
    }

    suspend fun deselectAll() = selectedIdsMap.clear()

    suspend fun addElementsToSelection(itemIndexes: List<Int>, itemToFocus: Int= itemIndexes.last()) {
        itemIndexes.forEach {
            selectedIdsMap[keys[it]] = it
        }
        focusItem(itemToFocus)
    }

    suspend fun removeElementsToSelection(itemIndexes: List<Int>, itemToFocus: Int= itemIndexes.last()) {
        itemIndexes.forEach {
            selectedIdsMap.remove(keys[it])
        }
        focusItem(itemToFocus)
    }

    suspend fun toggleElementsToSelection(itemIndexes: List<Int>, itemToFocus: Int= itemIndexes.last()) {
        itemIndexes.forEach { index ->
            selectedIdsMap[keys[index]]?.let {
                selectedIdsMap.remove(keys[index])
            } ?: { selectedIdsMap[keys[index]] = index }
        }
        focusItem(itemToFocus)
    }
/** ------>    CONTROL ALT L PLOXX <----------**/
    internal fun attachKeys(keys: List<Any?>, uiId: String) {

        if (this.uiId == null) {
            this.uiId = uiId
        } else require(this.uiId == uiId) {
            "Do not attach the same ${this::class.simpleName} to different SelectableLazyColumns."
        }

        this.keys = keys
        keys.forEachIndexed { index, key ->
            selectedIdsMap.computeIfPresent(key) { _, _ -> index }
        }
    }
}

interface SelectableLazyItemScope : FocusableLazyItemScope {

    val isSelected: Boolean
}

internal class SelectableLazyItemScopeImpl(
    delegate: FocusableLazyItemScope,
    isSelectedSate: State<Boolean>
) : SelectableLazyItemScope, FocusableLazyItemScope by delegate {
    override val isSelected by isSelectedSate
}


context(FocusableLazyItemScope, BoxWithConstraintsScope)
internal fun SelectableLazyItemScope(isSelectedSate: State<Boolean>): SelectableLazyItemScope =
    SelectableLazyItemScopeImpl(FocusableLazyItemScope(derivedStateOf { isFocused }), isSelectedSate)

internal fun FocusableLazyItemScope.SelectableLazyItemScope(): SelectableLazyItemScope =
    object : SelectableLazyItemScope, FocusableLazyItemScope by this {
        override val isSelected = false
    }

interface SelectableLazyListScope {

    fun item(
        key: Any,
        contentType: Any? = null,
        focusable: Boolean = true,
        selectable: Boolean = true,
        content: @Composable SelectableLazyItemScope.() -> Unit
    )

    fun items(
        count: Int,
        key: (index: Int) -> Any,
        contentType: (index: Int) -> Any? = { null },
        focusable: (index: Int) -> Boolean = { true },
        selectable: (index: Int) -> Boolean = { true },
        itemContent: SelectableLazyItemScope.(index: Int) -> Unit
    )

    fun stickyHeader(
        key: Any,
        contentType: Any? = null,
        focusable: Boolean = false,
        selectable: Boolean = false,
        content: SelectableLazyItemScope.() -> Unit
    )
}

internal fun FocusableLazyListScope.SelectableLazyListScope(state: SelectableLazyListState) =
    SelectableLazyListScopeDelegate(state, this)

internal class SelectableLazyListScopeDelegate(private val state: SelectableLazyListState) : SelectableLazyListScope {

    private val _keys = mutableListOf<Any?>()

    private val delegate = FocusableLazyListScopeContainer()

    val keys: List<Any?>
        get() = _keys

    val entries: List<FocusableLazyListScopeContainer.Entry>
        get() = delegate.entries

    override fun item(
        key: Any,
        contentType: Any?,
        focusable: Boolean,
        selectable: Boolean,
        content: SelectableLazyItemScope.() -> Unit
    ) {
        _keys.add(key)
        delegate.item(key, contentType, focusable) {
            val scope = rememberCoroutineScope()
            if (selectable) {
                BoxWithConstraints(
                    modifier = Modifier.onPointerEvent(PointerEventType.Press) {
                        with(keybindings) {
                            when {
                                it.keyboardModifiers.isKeyboardMultiSelectionKeyPressed && it.keyboardModifiers.isCtrlPressed -> {
                                    println("ctrl and shift pressed on click")
                                }

                                it.keyboardModifiers.isKeyboardMultiSelectionKeyPressed -> {
                                    println("ShiftClicked ")
                                    scope.launch {
                                        actions.onExtendSelectionTo(itemIndex)
                                    }
                                }

                                it.keyboardModifiers.isCtrlPressed -> {
                                    println("controll pressed")
                                    treeState.lastKeyEventUsedMouse = false
                                    treeState.toggleElementSelection(element)
                                }

                                else -> {
                                    treeState.selectSingleElement(element)
                                    onElementClick(element)
                                    println("single click")
                                }
                            }
                        }
                    }
                ) {
                    SelectableLazyItemScope(derivedStateOf { key in state.selectedIdsMap }).content()
                }
            } else SelectableLazyItemScope().content()
        }
    }

    override fun items(
        count: Int,
        key: (index: Int) -> Any,
        contentType: (index: Int) -> Any?,
        focusable: (index: Int) -> Boolean,
        selectable: (index: Int) -> Boolean,
        itemContent: SelectableLazyItemScope.(index: Int) -> Unit
    ) {
        TODO("Not yet implemented")
    }

    override fun stickyHeader(
        key: Any,
        contentType: Any?,
        focusable: Boolean,
        selectable: Boolean,
        content: SelectableLazyItemScope.() -> Unit
    ) {
        TODO("Not yet implemented")
    }
}
@Composable
fun rememberSelectableLazyListState(delegate: FocusableLazyListState = FocusableLazyListState()) =
    remember { SelectableLazyListState(delegate) }

fun CoroutineScope.handleSelectableColumnOnKeyEvent(
    keybinding: SelectableColumnKeybindings,
    actions: SelectableColumnOnKeyEvent
): KeyEvent.(Int) -> Boolean = lambda@{ focusedIndex ->
    if (type == KeyEventType.KeyUp) return@lambda false
    with(keybinding) {
        with(actions) {
            when {
                selectNextItem() ?: false -> launch { onSelectNextItem(focusedIndex) }
                selectPreviousItem() ?: false -> launch { onSelectPreviousItem(focusedIndex) }
                selectFirstItem() ?: false -> launch { onSelectFirstItem(focusedIndex) }
                selectLastItem() ?: false -> launch { onSelectLastItem(focusedIndex) }
                edit() ?: false -> launch { onEdit(focusedIndex) }
                extendSelectionToFirstItem() ?: false -> launch { onExtendSelectionToFirst(focusedIndex) }
                extendSelectionToLastItem() ?: false -> launch { onExtendSelectionToLastItem(focusedIndex) }
                extendSelectionWithNextItem() ?: false -> launch { onExtendSelectionWithNextItem(focusedIndex) }
                extendSelectionWithPreviousItem() ?: false -> launch { onExtendSelectionWithPreviousItem(focusedIndex) }
                scrollPageDownAndExtendSelection() ?: false -> launch { onScrollPageDownAndExtendSelection(focusedIndex) }
                scrollPageDownAndSelectItem() ?: false -> launch { onScrollPageDownAndSelectItem(focusedIndex) }
                scrollPageUpAndExtendSelection() ?: false -> launch { onScrollPageUpAndExtendSelection(focusedIndex) }
                scrollPageUpAndSelectItem() ?: false -> launch { onScrollPageUpAndSelectItem(focusedIndex) }
                else -> return@lambda false
            }
        }
    }
    return@lambda true
}
