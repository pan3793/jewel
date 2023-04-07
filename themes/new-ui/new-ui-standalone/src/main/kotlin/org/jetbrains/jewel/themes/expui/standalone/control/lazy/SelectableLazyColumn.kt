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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
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
    actions: SelectableColumnOnKeyEvent = DefaultSelectableOnKeyEvent(keybindings, state),
    content: SelectableLazyListScope.() -> Unit
) {
    LaunchedEffect(keybindings) {
        state.attachKeybindings(keybindings, actions)
    }
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

    internal var lastKeyEventUsedMouse: Boolean = false
    internal val selectedIdsMap = mutableStateMapOf<Any?, Int>()
    internal var keys = emptyList<Any?>()

    internal val selectedItemIndexesState = derivedStateOf { selectedIdsMap.values }

    private var uiId: String? = null

    fun isKeySelected(key: Any?) = selectedIdsMap.containsKey(key)
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

    var keybindings: SelectableColumnKeybindings = DefaultSelectableColumnKeybindings.Companion

    //    var actions: SelectableColumnOnKeyEvent = DefaultSelectableOnKeyEvent(keybindings,DefaultSelectableOnKeyEvent)
    fun attachKeybindings(keybindings: SelectableColumnKeybindings, actions: SelectableColumnOnKeyEvent) {
        this.keybindings = keybindings
//        this.actions = actions
    }

    fun indexOfNextSelectable(currentIndex: Int): Int? {
        for (i in currentIndex..keys.lastIndex) {
            if (keys[i] is SelectableKey.Selectable) return i
        }
        return null
    }

    fun indexOfPreviousSelectable(currentIndex: Int): Int? {
        for (i in currentIndex downTo 0) {
            if (keys[i] is SelectableKey.Selectable) return i
        }
        return null
    }

    suspend fun selectSingleItem(itemIndex: Int) {
        focusItem(itemIndex)
        deselectAll()
        selectedIdsMap[keys[itemIndex]] = itemIndex
    }

    suspend fun selectSingleKey(key: SelectableKey) {
        val index = keys.indexOf(key.key)
        if (index > 0 && key is SelectableKey.Selectable) selectSingleItem(index)
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

    suspend fun toggleSelectionKey(key: SelectableKey) {
        val index = keys.indexOf(key.key)
        if (index > 0 && key is SelectableKey.Selectable) toggleSelection(index)
    }

    suspend fun onExtendSelectionToKey(key: SelectableKey) {
        val index = keys.indexOf(key.key)
        if (index > 0 && key is SelectableKey.Selectable) toggleSelection(index)
    }

    suspend fun addElementToSelection(itemIndex: Int) {
        focusItem(itemIndex)
        selectedIdsMap[keys[itemIndex]] = itemIndex
    }

    suspend fun deselectAll() = selectedIdsMap.clear()

    suspend fun addElementsToSelection(itemIndexes: List<Int>, itemToFocus: Int = itemIndexes.last()) {
        itemIndexes.forEach {
            selectedIdsMap[keys[it]] = it
        }
        focusItem(itemToFocus)
    }

    suspend fun removeElementsToSelection(itemIndexes: List<Int>, itemToFocus: Int = itemIndexes.last()) {
        itemIndexes.forEach {
            selectedIdsMap.remove(keys[it])
        }
        focusItem(itemToFocus)
    }

    suspend fun toggleElementsToSelection(itemIndexes: List<Int>, itemToFocus: Int = itemIndexes.last()) {
        itemIndexes.forEach { index ->
            selectedIdsMap[keys[index]]?.let {
                selectedIdsMap.remove(keys[index])
            } ?: { selectedIdsMap[keys[index]] = index }
        }
        focusItem(itemToFocus)
    }

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
@Composable
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
        itemContent: @Composable SelectableLazyItemScope.(index: Int) -> Unit
    )

    fun stickyHeader(
        key: Any,
        contentType: Any? = null,
        focusable: Boolean = false,
        selectable: Boolean = false,
        content: @Composable SelectableLazyItemScope.() -> Unit
    )
}

internal fun FocusableLazyListScope.SelectableLazyListScope(state: SelectableLazyListState) =
    SelectableLazyListScopeDelegate(state)

sealed interface SelectableKey {

    val key: Any?

    class Selectable(override val key: Any?) : SelectableKey
    class NotSelectable(override val key: Any?) : SelectableKey
}

internal class SelectableLazyListScopeDelegate(private val state: SelectableLazyListState) : SelectableLazyListScope {

    private val _keys = mutableListOf<SelectableKey>()

    private val delegate = FocusableLazyListScopeContainer()

    val keys: List<SelectableKey>
        get() = _keys

    val entries: List<FocusableLazyListScopeContainer.Entry>
        get() = delegate.entries

    override fun item(
        key: Any,
        contentType: Any?,
        focusable: Boolean,
        selectable: Boolean,
        content: @Composable SelectableLazyItemScope.() -> Unit
    ) {
        _keys.add(if (selectable) SelectableKey.Selectable(key) else SelectableKey.NotSelectable(key))
        delegate.item(key, contentType, focusable) {
            val scope = rememberCoroutineScope()
            if (selectable) {
                BoxWithConstraints(
                    modifier = Modifier.onPointerEvent(PointerEventType.Press) {
                        with(state.keybindings) {
                            when {
                                it.keyboardModifiers.isKeyboardMultiSelectionKeyPressed && it.keyboardModifiers.isCtrlPressed -> {
                                    println("ctrl and shift pressed on click")
                                    //do nothing
                                }

                                it.keyboardModifiers.isKeyboardMultiSelectionKeyPressed -> {
                                    println("shift pressed on click")
                                    scope.launch {
                                        state.onExtendSelectionToKey(SelectableKey.Selectable(key))
                                    }
                                }

                                it.keyboardModifiers.isCtrlPressed -> {
                                    println("ctrl pressed on click")
                                    state.lastKeyEventUsedMouse = false
                                    scope.launch {
                                        state.toggleSelectionKey(SelectableKey.Selectable(key))
                                    }
                                }

                                else -> {
                                    println("single click")
                                    scope.launch {
                                        state.selectSingleKey(SelectableKey.Selectable(key))
                                    }
//                                    onElementClick(element) TODO
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
        itemContent: @Composable SelectableLazyItemScope.(index: Int) -> Unit
    ) {
        repeat(count) {
            val selectable = selectable(it)
            _keys.add(if (selectable) SelectableKey.Selectable(key) else SelectableKey.NotSelectable(key))
            item(key(it), contentType(it), focusable(it), selectable) {
                itemContent(it)
            }
        }
    }

    override fun stickyHeader(
        key: Any,
        contentType: Any?,
        focusable: Boolean,
        selectable: Boolean,
        content: @Composable SelectableLazyItemScope.() -> Unit
    ) {
        _keys.add(if (selectable) SelectableKey.Selectable(key) else SelectableKey.NotSelectable(key))
        delegate.item(key, contentType, focusable) {
            if (selectable) {
                val scope = rememberCoroutineScope()
                BoxWithConstraints(
                    modifier = Modifier.onPointerEvent(PointerEventType.Press) {
                        with(state.keybindings) {
                            when {
                                it.keyboardModifiers.isKeyboardMultiSelectionKeyPressed && it.keyboardModifiers.isCtrlPressed -> {
                                    println("ctrl and shift pressed on click")
                                    //do nothing
                                }

                                it.keyboardModifiers.isKeyboardMultiSelectionKeyPressed -> {
                                    println("shift pressed on click")
                                    scope.launch {
                                        state.onExtendSelectionToKey(SelectableKey.Selectable(key))
                                    }
                                }

                                it.keyboardModifiers.isCtrlPressed -> {
                                    println("ctrl pressed on click")
                                    state.lastKeyEventUsedMouse = false
                                    scope.launch {
                                        state.toggleSelectionKey(SelectableKey.Selectable(key))
                                    }
                                }

                                else -> {
                                    println("single click")
                                    scope.launch {
                                        state.selectSingleKey(SelectableKey.Selectable(key))
                                    }
//                                    onElementClick(element) TODO
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
