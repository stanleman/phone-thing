package com.example.phonething.ui

/**
 * Which pages exist in the carousel.
 * Index must match the ALL_FACTORIES order in MainPagerAdapter.
 */
enum class PageType(val index: Int, val label: String) {
    CLOCK(0, "Clock"),
    CALENDAR(1, "Calendar"),
    TODO(2, "To-Do List"),
    WIKIPEDIA(3, "Wikipedia");

    companion object {
        fun fromIndex(idx: Int): PageType = entries.first { it.index == idx }
    }
}

/**
 * Singleton that tracks which pages are enabled/visible.
 * Notifies [Listener]s on every change so the adapter can rebuild.
 */
object PageSettings {

    private val enabled = mutableMapOf<PageType, Boolean>()

    init {
        PageType.entries.forEach { enabled[it] = true }
    }

    fun isEnabled(page: PageType): Boolean = enabled[page] ?: true

    /** Returns enabled pages in their natural order. */
    fun activePages(): List<PageType> = PageType.entries.filter { isEnabled(it) }

    /** Number of enabled pages (floor to 1 to avoid empty adapters). */
    val activeCount: Int get() = maxOf(1, PageType.entries.count { isEnabled(it) })

    fun setEnabled(page: PageType, on: Boolean) {
        if ((enabled[page] ?: true) == on) return
        enabled[page] = on
        listeners.toList().forEach { it.onPageSettingsChanged() }
    }

    fun toggle(page: PageType) {
        setEnabled(page, !isEnabled(page))
    }

    // ── Listener mechanism ──────────────────────────────────────

    private val listeners = mutableListOf<Listener>()

    fun addListener(l: Listener) { listeners.add(l) }
    fun removeListener(l: Listener) { listeners.remove(l) }

    interface Listener {
        fun onPageSettingsChanged()
    }
}
