package com.example.phonething

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.res.ResourcesCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.phonething.databinding.ActivityMainBinding
import com.example.phonething.ui.MainPagerAdapter
import com.example.phonething.ui.PageSettings
import com.example.phonething.ui.PageType

class MainActivity : AppCompatActivity(), PageSettings.Listener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var pagerAdapter: MainPagerAdapter
    private var gearVisible = false

    // Tap-detection state (dispatchTouchEvent doesn't get onClick from ViewPager)
    private var lastTapX = 0f
    private var lastTapY = 0f
    private var lastTapTime = 0L

    // Track which page type the user is currently viewing
    private var currentVisiblePageType: PageType = PageType.CLOCK

    companion object {
        private const val PREFS_NAME = "phone_thing_prefs"
        private const val KEY_DARK_MODE = "dark_mode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hideSystemBars()
        setupViewPager()
        setupGearAndSettings()
        PageSettings.addListener(this)
    }

    override fun onDestroy() {
        PageSettings.removeListener(this)
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    // ─── System bars ─────────────────────────────────────────────

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }

    // ─── ViewPager ───────────────────────────────────────────────

    private fun setupViewPager() {
        pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.apply {
            adapter = pagerAdapter
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            setCurrentItem(MainPagerAdapter.MIDDLE, false)
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    currentVisiblePageType = pagerAdapter.pageTypeAt(position)
                }
            })
        }
    }

    /** PageSettings.Listener — rebuild adapter when toggles change. */
    override fun onPageSettingsChanged() {
        // 1. Remember which page the user was viewing before the rebuild
        val wasOnPage = currentVisiblePageType
        val oldPos = binding.viewPager.currentItem

        // 2. Rebuild the adapter (activePages list changes)
        pagerAdapter.rebuild()

        // 3. Recenter so the user stays on the same logical page
        if (pagerAdapter.activePages.isNotEmpty()) {
            val targetIdx = pagerAdapter.activePages.indexOf(wasOnPage)

            val effectiveIdx = if (targetIdx >= 0) {
                targetIdx
            } else {
                // Page was turned off — snap to the nearest enabled page
                wasOnPage.let { removed ->
                    var found: PageType? = null
                    for (i in PageType.entries.indexOf(removed) downTo 0) {
                        if (PageSettings.isEnabled(PageType.entries[i])) {
                            found = PageType.entries[i]
                            break
                        }
                    }
                    if (found == null) {
                        for (i in PageType.entries.indexOf(removed) until PageType.entries.size) {
                            if (PageSettings.isEnabled(PageType.entries[i])) {
                                found = PageType.entries[i]
                                break
                            }
                        }
                    }
                    pagerAdapter.activePages.indexOf(found ?: pagerAdapter.activePages.first())
                }
            }

            // Calculate new absolute position close to oldPos that maps to effectiveIdx
            val newCount = pagerAdapter.activeCount
            val currentMod = ((oldPos % newCount) + newCount) % newCount
            val delta = ((effectiveIdx - currentMod) % newCount + newCount) % newCount
            binding.viewPager.setCurrentItem(oldPos + delta, false)
        }
    }

    // ─── Gear icon + Settings overlay ────────────────────────────

    private fun setupGearAndSettings() {
        buildTileGrid()

        binding.settingsGear.setOnClickListener { showSettingsOverlay() }
        binding.settingsClose.setOnClickListener { hideSettingsOverlay() }
    }

    private fun buildTileGrid() {
        val grid = binding.settingsTileGrid
        grid.removeAllViews()

        val density = resources.displayMetrics.density
        val pages = PageType.entries

        var i = 0
        while (i < pages.size) {
            val row = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.HORIZONTAL
            }
            val itemsInRow = minOf(2, pages.size - i)
            repeat(itemsInRow) { col ->
                val isLastInRow = (col == itemsInRow - 1)
                row.addView(buildSelectableItem(pages[i], density, isLastInRow))
                i++
            }
            grid.addView(row)
        }
    }

    private fun buildSelectableItem(page: PageType, density: Float, isLastInRow: Boolean): View {
        val isChecked = PageSettings.isEnabled(page)

        // Horizontal row: squircle + label
        val row = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                val rightMargin = if (isLastInRow) 0 else (12 * density).toInt()
                val bottomMargin = (12 * density).toInt()
                setMargins(0, 0, rightMargin, bottomMargin)
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(
                (12 * density).toInt(),
                (10 * density).toInt(),
                (12 * density).toInt(),
                (10 * density).toInt()
            )
            setBackgroundResource(com.example.phonething.R.drawable.settings_tile_bg)
            isClickable = true
            isFocusable = true
        }

        // Squircle checkbox indicator (24dp x 24dp)
        val squircle = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                (24 * density).toInt(),
                (24 * density).toInt()
            )
            background = if (isChecked)
                resources.getDrawable(com.example.phonething.R.drawable.ic_checkbox_checked, theme)
            else
                resources.getDrawable(com.example.phonething.R.drawable.ic_checkbox_unchecked, theme)
        }

        // Label with ellipsis overflow
        val label = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply { leftMargin = (12 * density).toInt() }
            text = page.label
            setTextColor(ResourcesCompat.getColor(resources, com.example.phonething.R.color.text_primary, null))
            textSize = 15f
            ellipsize = android.text.TextUtils.TruncateAt.END
            maxLines = 1
        }

        // Click row toggles state
        row.setOnClickListener {
            val newChecked = !PageSettings.isEnabled(page)
            PageSettings.setEnabled(page, newChecked)
            squircle.setBackgroundResource(
                if (newChecked) com.example.phonething.R.drawable.ic_checkbox_checked
                else com.example.phonething.R.drawable.ic_checkbox_unchecked
            )
        }

        row.addView(squircle)
        row.addView(label)
        return row
    }

    private fun showSettingsOverlay() {
        gearVisible = false
        binding.settingsGear.visibility = View.GONE

        // Refresh tile states
        refreshTileStates()

        // Refresh dark mode switch state
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean(KEY_DARK_MODE, true)

        binding.settingsDarkSwitch.apply {
            setOnCheckedChangeListener(null)
            isChecked = isDarkMode
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply()
                val mode = if (isChecked)
                    AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                AppCompatDelegate.setDefaultNightMode(mode)
                recreate()
            }
        }

        binding.settingsOverlay.visibility = View.VISIBLE
        binding.settingsOverlay.alpha = 0f
        binding.settingsOverlay.animate().alpha(1f).setDuration(200).start()
    }

    private fun refreshTileStates() {
        val grid = binding.settingsTileGrid
        var pageIdx = 0
        for (i in 0 until grid.childCount) {
            val row = grid.getChildAt(i) as? LinearLayout ?: continue
            for (j in 0 until row.childCount) {
                val itemRow = row.getChildAt(j) as? LinearLayout ?: continue
                if (itemRow.childCount < 2) continue
                if (pageIdx >= PageType.entries.size) break
                val page = PageType.entries[pageIdx]
                val squircle = itemRow.getChildAt(0)
                val isChecked = PageSettings.isEnabled(page)
                squircle.setBackgroundResource(
                    if (isChecked) com.example.phonething.R.drawable.ic_checkbox_checked
                    else com.example.phonething.R.drawable.ic_checkbox_unchecked
                )
                // Re-bind click to clear stale listeners
                itemRow.setOnClickListener {
                    val newChecked = !PageSettings.isEnabled(page)
                    PageSettings.setEnabled(page, newChecked)
                    squircle.setBackgroundResource(
                        if (newChecked) com.example.phonething.R.drawable.ic_checkbox_checked
                        else com.example.phonething.R.drawable.ic_checkbox_unchecked
                    )
                }
                pageIdx++
            }
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        if (binding.settingsOverlay.visibility != View.VISIBLE) {
            when (ev.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    lastTapX = ev.x
                    lastTapY = ev.y
                    lastTapTime = System.currentTimeMillis()
                }
                android.view.MotionEvent.ACTION_UP -> {
                    val dx = kotlin.math.abs(ev.x - lastTapX)
                    val dy = kotlin.math.abs(ev.y - lastTapY)
                    val slop = android.view.ViewConfiguration.get(this).scaledTouchSlop
                    val elapsed = System.currentTimeMillis() - lastTapTime
                    if (dx < slop && dy < slop && elapsed < android.view.ViewConfiguration.getLongPressTimeout()) {
                        if (!isTouchOnGear(ev.rawX, ev.rawY)) {
                            toggleGear()
                        }
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun isTouchOnGear(rawX: Float, rawY: Float): Boolean {
        if (!gearVisible) return false
        val loc = IntArray(2)
        binding.settingsGear.getLocationOnScreen(loc)
        val left = loc[0]
        val top = loc[1]
        val right = left + binding.settingsGear.width
        val bottom = top + binding.settingsGear.height
        return rawX.toInt() in left until right && rawY.toInt() in top until bottom
    }

    private fun toggleGear() {
        gearVisible = !gearVisible
        binding.settingsGear.visibility = if (gearVisible) View.VISIBLE else View.GONE
    }

    private fun hideSettingsOverlay() {
        binding.settingsOverlay.animate()
            .alpha(0f)
            .setDuration(150)
            .withEndAction {
                binding.settingsOverlay.visibility = View.GONE
            }
            .start()
    }
}
