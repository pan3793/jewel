package org.jetbrains.jewel.themes.expui.standalone.control.lazy

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.max
import kotlin.math.min

interface FocusableState : ScrollableState {

    suspend fun focusItem(itemIndex: Int, animateScroll: Boolean = false, scrollOffset: Int = 0)
}

val LazyListState.visibleItemsRange
    get() = firstVisibleItemIndex..firstVisibleItemIndex + layoutInfo.visibleItemsInfo.size

val FocusableLazyListState.visibleItemsRange
    get() = firstVisibleItemIndex..firstVisibleItemIndex + layoutInfo.visibleItemsInfo.size

class FocusableLazyListState(val delegate: LazyListState = LazyListState()) : FocusableState, ScrollableState by delegate {

    override suspend fun focusItem(itemIndex: Int, animateScroll: Boolean, scrollOffset: Int) {
        val visibleRange = visibleItemsRange.drop(2).dropLast(4)

        if (itemIndex !in visibleRange && visibleRange.isNotEmpty()) {
            when {
                itemIndex < visibleRange.first() -> delegate.scrollToItem(max(0, itemIndex - 2), animateScroll, scrollOffset)
                itemIndex > visibleRange.last() -> {
                    val indexOfFirstVisibleElement = itemIndex - visibleRange.size
                    delegate.scrollToItem(
                        min(delegate.layoutInfo.totalItemsCount - 1, indexOfFirstVisibleElement - 1),
                        animateScroll,
                        scrollOffset
                    )
                }
            }
        }

        focusVisibleItem(itemIndex)
    }

    private fun focusVisibleItem(itemIndex: Int) {
        layoutInfo.visibleItemsInfo
            .find { it.index == itemIndex }
            ?.focusableKey
            ?.let { it as? FocusableKey.Focusable }
            ?.focusRequester
            ?.requestFocus()
    }

    internal sealed interface LastFocusedKeyContainer {
        object NotSet : LastFocusedKeyContainer
        @JvmInline value class Set(val key: Any?) : LastFocusedKeyContainer
    }

    internal val lastFocusedKeyState: MutableState<LastFocusedKeyContainer> = mutableStateOf(LastFocusedKeyContainer.NotSet)
    internal val lastFocusedIndexState: MutableState<Int?> = mutableStateOf(null)

    val lastFocusedIndex
        get() = lastFocusedIndexState.value

    val layoutInfo: FocusableLazyListLayoutInfo
        get() = delegate.layoutInfo.asFocusable()

    /** The index of the first item that is visible */
    val firstVisibleItemIndex: Int
        get() = delegate.firstVisibleItemIndex

    /**
     * The scroll offset of the first visible item. Scrolling forward is
     * positive - i.e., the amount that the item is offset backwards
     */
    val firstVisibleItemScrollOffset: Int
        get() = delegate.firstVisibleItemScrollOffset

    /**
     * [InteractionSource] that will be used to dispatch drag events when
     * this list is being dragged. If you want to know whether the fling (or
     * animated scroll) is in progress, use [isScrollInProgress].
     */
    val interactionSource: InteractionSource
        get() = delegate.interactionSource
}

private fun LazyListLayoutInfo.asFocusable() = object : FocusableLazyListLayoutInfo {
    override val visibleItemsInfo: List<FocusableLazyListItemInfo>
        get() = this@asFocusable.visibleItemsInfo.asFocusable()
    override val viewportStartOffset: Int
        get() = this@asFocusable.viewportStartOffset
    override val viewportEndOffset: Int
        get() = this@asFocusable.viewportEndOffset
    override val totalItemsCount: Int
        get() = this@asFocusable.totalItemsCount
}

private fun List<LazyListItemInfo>.asFocusable(): List<FocusableLazyListItemInfo> = map {
    object : FocusableLazyListItemInfo {
        override val index: Int
            get() = it.index
        override val focusableKey: FocusableKey
            get() = it.key as FocusableKey
        override val offset: Int
            get() = it.offset
        override val size: Int
            get() = it.size
    }
}

/**
 * Contains useful information about the currently displayed layout state
 * of lazy lists like [LazyColumn] or [LazyRow]. For example you can get
 * the list of currently displayed item.
 *
 * Use [LazyListState.layoutInfo] to retrieve this
 */
interface FocusableLazyListLayoutInfo {

    /**
     * The list of [LazyListItemInfo] representing all the currently visible
     * items.
     */
    val visibleItemsInfo: List<FocusableLazyListItemInfo>

    /**
     * The start offset of the layout's viewport. You can think of it as a
     * minimum offset which would be visible. Usually it is 0, but it can be
     * negative if a content padding was applied as the content displayed in
     * the content padding area is still visible.
     *
     * You can use it to understand what items from [visibleItemsInfo] are
     * fully visible.
     */
    val viewportStartOffset: Int

    /**
     * The end offset of the layout's viewport. You can think of it as a
     * maximum offset which would be visible. Usually it is a size of the lazy
     * list container plus a content padding.
     *
     * You can use it to understand what items from [visibleItemsInfo] are
     * fully visible.
     */
    val viewportEndOffset: Int

    /** The total count of items passed to [LazyColumn] or [LazyRow]. */
    val totalItemsCount: Int
}

/**
 * Contains useful information about an individual item in lazy lists like
 * [LazyColumn] or [LazyRow].
 *
 * @see LazyListLayoutInfo
 */
interface FocusableLazyListItemInfo {

    /** The index of the item in the list. */
    val index: Int

    /** The key of the item which was passed to the item() or items() function. */
    val focusableKey: FocusableKey

    /**
     * The main axis offset of the item. It is relative to the start of the
     * lazy list container.
     */
    val offset: Int

    /**
     * The main axis size of the item. Note that if you emit multiple layouts
     * in the composable slot for the item then this size will be calculated as
     * the sum of their sizes.
     */
    val size: Int
}

private suspend fun LazyListState.scrollToItem(index: Int, animate: Boolean, scrollOffset: Int = 0) =
    if (animate) animateScrollToItem(index, scrollOffset) else scrollToItem(index, scrollOffset)

sealed interface FocusableKey {

    val key: Any?

    class Focusable(val focusRequester: FocusRequester, override val key: Any?) : FocusableKey
    class NotFocusable(override val key: Any?) : FocusableKey
}

interface FocusableLazyItemScope : LazyItemScope {

    val isFocused: Boolean
}

internal class FocusableLazyListScopeContainer : FocusableLazyListScope {

    private var lastIndex = 0

    internal interface Entry {

        sealed interface Single : Entry {

            val key: Any?
            val innerIndex: Int
            val contentType: Any?
            val focusable: Boolean
            val content: @Composable FocusableLazyItemScope.() -> Unit

            data class Item(
                override val key: Any?,
                override val innerIndex: Int,
                override val contentType: Any?,
                override val focusable: Boolean,
                override val content: @Composable FocusableLazyItemScope.() -> Unit
            ) : Single

            data class StickyHeader(
                override val key: Any?,
                override val innerIndex: Int,
                override val contentType: Any?,
                override val focusable: Boolean,
                override val content: @Composable FocusableLazyItemScope.() -> Unit
            ) : Single
        }

        data class Items(
            val count: Int,
            val key: ((index: Int) -> Any)?,
            val contentType: (index: Int) -> Any?,
            val innerIndex: Int,
            val focusable: (index: Int) -> Boolean,
            val itemContent: @Composable FocusableLazyItemScope.(index: Int) -> Unit
        ) : Entry
    }

    internal val entries = mutableListOf<Entry>()

    override fun item(
        key: Any?,
        contentType: Any?,
        focusable: Boolean,
        content: @Composable FocusableLazyItemScope.() -> Unit
    ) {
        entries.add(Entry.Single.Item(key, lastIndex++, contentType, focusable, content))
    }

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        contentType: (index: Int) -> Any?,
        focusable: (index: Int) -> Boolean,
        itemContent: @Composable FocusableLazyItemScope.(index: Int) -> Unit
    ) {
        entries.add(
            element = Entry.Items(
                count = count,
                key = key,
                contentType = contentType,
                innerIndex = lastIndex,
                focusable = focusable,
                itemContent = itemContent
            )
        )
        lastIndex += count
    }

    @ExperimentalFoundationApi
    override fun stickyHeader(key: Any?, contentType: Any?, focusable: Boolean, content: @Composable FocusableLazyItemScope.() -> Unit) {
        entries.add(Entry.Single.StickyHeader(key, lastIndex++, contentType, focusable, content))
    }
}

@Composable
fun rememberFocusableLazyListState(
    firstVisibleItemIndex: Int = 0,
    firstVisibleItemScrollOffset: Int = 0
) = remember { FocusableLazyListState(LazyListState(firstVisibleItemIndex, firstVisibleItemScrollOffset)) }

@Composable
fun BoxWithConstraintsScope.FocusableLazyItemScope(isFocused: State<Boolean>): FocusableLazyItemScope =
    FocusableLazyItemScopeImpl(LocalDensity.current, constraints, isFocused)

fun LazyItemScope.FocusableLazyItemScope(): FocusableLazyItemScope =
    FocusableLazyItemScopeDelegate(this)

internal class FocusableLazyItemScopeDelegate(
    private val delegate: LazyItemScope
) : FocusableLazyItemScope, LazyItemScope by delegate {

    override val isFocused = false
}

internal class FocusableLazyItemScopeImpl(
    val density: Density,
    val constraints: Constraints,
    val isFocusedState: State<Boolean>
) : FocusableLazyItemScope {

    override val isFocused
        get() = isFocusedState.value == true

    private val maxWidth: Dp = with(density) { constraints.maxWidth.toDp() }
    private val maxHeight: Dp = with(density) { constraints.maxHeight.toDp() }

    override fun Modifier.fillParentMaxSize(fraction: Float) = size(
        maxWidth * fraction,
        maxHeight * fraction
    )

    override fun Modifier.fillParentMaxWidth(fraction: Float) =
        width(maxWidth * fraction)

    override fun Modifier.fillParentMaxHeight(fraction: Float) =
        height(maxHeight * fraction)

    @ExperimentalFoundationApi
    override fun Modifier.animateItemPlacement(animationSpec: FiniteAnimationSpec<IntOffset>) =
        this.then(AnimateItemPlacementModifier(animationSpec, debugInspectorInfo {
            name = "animateItemPlacement"
            value = animationSpec
        }))
}

private class AnimateItemPlacementModifier(
    val animationSpec: FiniteAnimationSpec<IntOffset>,
    inspectorInfo: InspectorInfo.() -> Unit,
) : ParentDataModifier, InspectorValueInfo(inspectorInfo) {

    override fun Density.modifyParentData(parentData: Any?): Any = animationSpec

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnimateItemPlacementModifier) return false
        return animationSpec != other.animationSpec
    }

    override fun hashCode(): Int {
        return animationSpec.hashCode()
    }
}

