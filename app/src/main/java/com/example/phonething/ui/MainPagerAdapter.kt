package com.example.phonething.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.phonething.ui.fragments.CalendarFragment
import com.example.phonething.ui.fragments.ClockFragment
import com.example.phonething.ui.fragments.TodoFragment
import com.example.phonething.ui.fragments.WikipediaFragment

/**
 * Infinite-loop adapter that only cycles through currently-enabled pages.
 * Disabled pages are completely removed from the carousel — you cannot
 * swipe to them until re-enabled in Settings.
 */
class MainPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    companion object {
        val ALL_FACTORIES = listOf(
            { ClockFragment() },
            { CalendarFragment() },
            { TodoFragment() },
            { WikipediaFragment() }
        )
        const val TOTAL_COUNT = Int.MAX_VALUE
        val MIDDLE get() = TOTAL_COUNT / 2
    }

    /** Current list of enabled pages in natural order — rebuilt on toggle. */
    var activePages: List<PageType> = PageType.entries.filter { PageSettings.isEnabled(it) }
        private set

    val activeCount: Int get() = maxOf(1, activePages.size)

    override fun getItemCount(): Int = TOTAL_COUNT

    override fun createFragment(position: Int): Fragment {
        val idx = position % activeCount
        val pageType = activePages[idx.coerceIn(activePages.indices)]
        return ALL_FACTORIES[pageType.index].invoke()
    }

    /** Get the PageType shown at a given absolute position. */
    fun pageTypeAt(position: Int): PageType {
        if (activePages.isEmpty()) return PageType.CLOCK
        return activePages[position % activePages.size]
    }

    override fun getItemId(position: Int): Long {
        // Encode position + activeCount so notifyDataSetChanged forces full rebuild
        return (position.toLong() shl 8) or activeCount.toLong()
    }

    override fun containsItem(itemId: Long): Boolean = true

    /** Rebuild the active-pages list after a settings toggle. */
    fun rebuild() {
        activePages = PageType.entries.filter { PageSettings.isEnabled(it) }
        notifyDataSetChanged()
    }

    /** Convert an infinite position to a logical 0-based page index. */
    fun logicalPosition(position: Int): Int = position % activeCount
}
