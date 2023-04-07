package org.jetbrains.jewel.themes.expui.standalone.control.lazy

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.themes.expui.standalone.control.lazy.FocusableLazyListScopeContainer.Entry
import org.jetbrains.jewel.themes.expui.standalone.control.lazy.FocusableLazyListState.LastFocusedKeyContainer

@Composable
fun FocusableLazyColumn(
    modifier: Modifier = Modifier,
    verticalScroll: Boolean = false,
    state: FocusableLazyListState = rememberFocusableLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onKeyPressed: KeyEvent.(Int) -> Boolean = { _ -> false },
    content: FocusableLazyListScope.() -> Unit
) {
    FocusableLazyColumn(
        modifier,
        verticalScroll,
        state,
        contentPadding,
        reverseLayout,
        verticalArrangement,
        horizontalAlignment,
        flingBehavior,
        interactionSource,
        onKeyPressed,
        FocusableLazyListScopeContainer().apply(content).entries
    )
}

@Composable
internal fun FocusableLazyColumn(
    modifier: Modifier = Modifier,
    verticalScroll: Boolean = false,
    state: FocusableLazyListState = rememberFocusableLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onKeyPressed: KeyEvent.(Int) -> Boolean = { _ -> false },
    content: List<Entry>
) {
    VerticalScroller(
        listState = state.delegate,
        topContentPadding = 0.dp,
        thumbAllowed = { verticalScroll },
        endContentPadding = contentPadding.calculateEndPadding(LayoutDirection.Ltr),
    ) {
        BaseFocusableLazyColumn(
            modifier,
            state,
            contentPadding,
            reverseLayout,
            verticalArrangement,
            horizontalAlignment,
            flingBehavior,
            interactionSource,
            onKeyPressed,
            content
        )
    }
}

@Composable
internal fun BaseFocusableLazyColumn(
    modifier: Modifier = Modifier,
    state: FocusableLazyListState = rememberFocusableLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onKeyPressed: KeyEvent.(Int) -> Boolean = { _ -> false },
    content: List<Entry>
) {
    Box(
        modifier
            .onPreviewKeyEvent { event -> state.lastFocusedIndex?.let { onKeyPressed(event, it) } ?: false }
            .focusable(interactionSource = interactionSource)
    ) {
        LazyColumn(
            state = state.delegate,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            flingBehavior = flingBehavior
        ) {
            content.forEach { entry ->
                when (entry) {
                    is Entry.Items -> items(entry, state)
                    is Entry.Single -> singleElement(entry, state)
                }
            }
        }
    }
}

fun <T> FocusableLazyListScope.items(
    items: List<T>,
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    focusable: (item: T) -> Boolean = { true },
    itemContent: @Composable FocusableLazyListScope.(item: T) -> Unit
) = repeat(items.size) { index ->
    item(
        key = key?.invoke(items[index]),
        contentType = contentType(items[index]),
        focusable = focusable(items[index]),
    ) {
        itemContent(items[index])
    }
}

fun <T> FocusableLazyListScope.itemsIndexed(
    items: List<T>,
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    focusable: (item: T) -> Boolean = { true },
    itemContent: @Composable FocusableLazyItemScope.(item: T, index: Int) -> Unit
) = repeat(items.size) { index ->
    item(
        key = key?.invoke(items[index]),
        contentType = contentType(items[index]),
        focusable = focusable(items[index]),
    ) {
        itemContent(items.getOrNull(index) ?: return@item, index)
    }
}

interface FocusableLazyListScope {

    fun item(
        key: Any? = null,
        contentType: Any? = null,
        focusable: Boolean = true,
        content: @Composable FocusableLazyItemScope.() -> Unit
    )

    fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        contentType: (index: Int) -> Any? = { null },
        focusable: (index: Int) -> Boolean = { true },
        itemContent: @Composable FocusableLazyItemScope.(index: Int) -> Unit
    )

    fun stickyHeader(
        key: Any? = null,
        contentType: Any? = null,
        focusable: Boolean = false,
        content: @Composable FocusableLazyItemScope.() -> Unit
    )
}

private fun LazyListScope.items(
    entry: Entry.Items,
    state: FocusableLazyListState,
) {
    val lastKey = state.lastFocusedKeyState.value
    val requesters = Array(entry.count) { if (entry.focusable(it)) FocusRequester() else null }
    if (lastKey is LastFocusedKeyContainer.Set) repeat(entry.count) {
        if (entry.key?.invoke(it) == lastKey) {
            state.lastFocusedIndexState.value = entry.innerIndex + it
        }
    }
    items(
        count = entry.count,
        key = {
            if (entry.focusable(it)) {
                FocusableKey.Focusable(requesters[it]!!, entry.key?.invoke(it))
            } else {
                FocusableKey.NotFocusable(entry.key?.invoke(it))
            }
        },
        contentType = entry.contentType
    ) { itemIndex ->
        val itemFinalIndex = entry.innerIndex + itemIndex
        if (entry.focusable(itemIndex)) {
            BoxWithConstraints(
                Modifier
                    .focusRequester(requesters[itemIndex]!!)
                    .onFocusChanged {
                        if (it.hasFocus) {
                            state.lastFocusedKeyState.value = LastFocusedKeyContainer.Set(entry.key?.invoke(itemIndex))
                            state.lastFocusedIndexState.value = itemFinalIndex
                        }
                    }
                    .focusable()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitFirstDown(false)
                                requesters[itemIndex]!!.requestFocus()
                                println("focus requested item: $itemIndex")
                            }
                        }
                    }
            ) {
                entry.itemContent(FocusableLazyItemScope(), itemIndex)
            }
        } else {
            entry.itemContent(FocusableLazyItemScope(), itemIndex)
        }
        if (state.lastFocusedIndex == itemFinalIndex) {
            SideEffect { requesters[itemIndex]!!.requestFocus() }
        }
    }
}

private fun LazyListScope.singleElement(
    entry: Entry.Single,
    state: FocusableLazyListState
) {
    if (entry.focusable) {
        val lastKey = state.lastFocusedKeyState.value
        if (lastKey is LastFocusedKeyContainer.Set && lastKey.key == entry.key) {
            state.lastFocusedIndexState.value = entry.innerIndex
        }
        val fr = FocusRequester()
        val newContent: @Composable LazyItemScope.() -> Unit = {
            BoxWithConstraints(
                Modifier
                    .focusRequester(fr)
                    .onFocusChanged {
                        if (it.hasFocus) {
                            state.lastFocusedKeyState.value = LastFocusedKeyContainer.Set(entry.key)
                            state.lastFocusedIndexState.value = entry.innerIndex
                        }
                    }
                    .focusable()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitFirstDown(false)
                                fr.requestFocus()
                                println("focus requested on single item")
                            }
                        }
                    }
            ) {
                entry.content(FocusableLazyItemScope(derivedStateOf { state.lastFocusedIndex == entry.innerIndex }))
            }
            if (state.lastFocusedIndex == entry.innerIndex) SideEffect { fr.requestFocus() }
        }
        if (entry is Entry.Single.Item)
            item(FocusableKey.Focusable(fr, entry.key), entry.contentType, newContent)
        else
            stickyHeader(FocusableKey.Focusable(fr, entry.key), entry.contentType, newContent)
    } else {
        if (entry is Entry.Single.Item) item(FocusableKey.NotFocusable(entry.key), entry.contentType) {
            entry.content(FocusableLazyItemScope())
        }
        else stickyHeader(FocusableKey.NotFocusable(entry.key), entry.contentType) {
            entry.content(FocusableLazyItemScope())
        }
    }
}

