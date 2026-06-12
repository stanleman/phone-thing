package com.example.phonething.ui.fragments

import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.example.phonething.R
import com.example.phonething.databinding.FragmentCalendarBinding
import java.util.Calendar
import java.util.Locale

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private var displayYear = 0
    private var displayMonth = 0
    private var currentDialogDay: Int? = null
    private var currentDialog: AlertDialog? = null

    private val events = mutableListOf(
        CalendarEvent(1, "Meeting with Investors", 3),
        CalendarEvent(2, "Design Review", 5),
        CalendarEvent(3, "Catchup with Dev Team", 8),
        CalendarEvent(4, "Coffee with Sarah", 12),
        CalendarEvent(5, "Gym Session", 15),
        CalendarEvent(6, "Doctor Appointment", 18),
        CalendarEvent(7, "Book Club Meeting", 22),
        CalendarEvent(8, "Team Standup", 24),
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val cal = Calendar.getInstance()
        displayYear = cal.get(Calendar.YEAR)
        displayMonth = cal.get(Calendar.MONTH)
        buildCalendar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun buildCalendar() {
        buildWeekdayHeaders()
        buildDateGrid()
    }

    private fun buildWeekdayHeaders() {
        val grid = binding.weekdayGrid
        grid.removeAllViews()
        val density = resources.displayMetrics.density
        dayLabels.forEach { day ->
            val tv = TextView(requireContext()).apply {
                text = day
                textSize = 12f
                setTextColor(ResourcesCompat.getColor(resources, R.color.text_secondary, null))
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    bottomMargin = (4 * density).toInt()
                }
            }
            grid.addView(tv)
        }
    }

    private fun buildDateGrid() {
        val container = binding.calendarGrid
        container.removeAllViews()
        val density = resources.displayMetrics.density

        val tempCal = Calendar.getInstance()
        tempCal.set(displayYear, displayMonth, 1)
        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
        val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val startCol = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

        val dayEvents = mutableMapOf<Int, List<CalendarEvent>>()
        events.forEach { e ->
            dayEvents[e.startDay] = (dayEvents[e.startDay] ?: emptyList()) + e
        }

        // Calculate the actual number of weeks this month occupies
        val totalCells = startCol + daysInMonth
        val weeksNeeded = (totalCells + 6) / 7

        var cellIdx = 0

        for (row in 0 until weeksNeeded) {
            val rowLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                isBaselineAligned = false
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            }

            for (col in 0 until 7) {
                val dayNum = getDayNumber(row, col, startCol, daysInMonth)
                val evForDay = if (dayNum > 0) dayEvents[dayNum] ?: emptyList() else emptyList()

                val cell = buildDayCell(dayNum, density, evForDay)
                cell.tag = cellIdx
                cell.layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1f
                )
                rowLayout.addView(cell)
                cellIdx++
            }
            container.addView(rowLayout)
        }

        // Rows fill the container via weighted layout (0dp height + weight=1f each)
    }

    private fun getDayNumber(row: Int, col: Int, startCol: Int, daysInMonth: Int): Int {
        val day = row * 7 + col - startCol + 1
        return if (day in 1..daysInMonth) day else -1
    }

    private fun buildDayCell(dayNum: Int, density: Float, evForDay: List<CalendarEvent>): View {
        val cell = FrameLayout(requireContext())
        cell.setPadding((6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt())
        if (dayNum == -1) return cell

        cell.setBackgroundResource(R.drawable.calendar_cell_bg)

        val dayText = TextView(requireContext()).apply {
            text = dayNum.toString()
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.TOP or Gravity.END }
            setTextColor(ResourcesCompat.getColor(resources, R.color.text_primary, null))
        }
        cell.addView(dayText)

        if (evForDay.isNotEmpty()) {
            val eventStack = LinearLayout(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = (14 * density).toInt() }
                orientation = LinearLayout.VERTICAL
            }
            val shown = evForDay.take(2)
            val remaining = evForDay.size - shown.size
            shown.forEach { event ->
                eventStack.addView(TextView(requireContext()).apply {
                    text = "• " + event.title
                    textSize = 9f
                    setSingleLine(true)
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setTextColor(ResourcesCompat.getColor(resources, R.color.text_secondary, null))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                })
            }
            if (remaining > 0) {
                eventStack.addView(TextView(requireContext()).apply {
                    text = "+ $remaining more"
                    textSize = 8f
                    setTextColor(ResourcesCompat.getColor(resources, R.color.accent, null))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                })
            }
            cell.addView(eventStack)
        }

        cell.setOnClickListener {
            showDayEventsDialog(dayNum)
        }
        return cell
    }

    private fun showDayEventsDialog(dayNum: Int) {
        currentDialogDay = dayNum
        currentDialog?.dismiss()

        val cal = Calendar.getInstance()
        cal.set(displayYear, displayMonth, dayNum)
        val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US) ?: ""
        val dateTitle = "$monthName $dayNum, $displayYear"

        val dayEvents = events.filter { it.startDay == dayNum }
        val density = resources.displayMetrics.density

        val contentView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (16 * density).toInt(), (24 * density).toInt(), (8 * density).toInt())
        }

        if (dayEvents.isEmpty()) {
            contentView.addView(TextView(requireContext()).apply {
                text = "No events for this day"
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(ResourcesCompat.getColor(resources, R.color.text_secondary, null))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { bottomMargin = (16 * density).toInt() }
            })
        } else {
            dayEvents.forEach { event ->
                val eventRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply { bottomMargin = (4 * density).toInt() }
                    setPadding((8 * density).toInt(), (6 * density).toInt(), (8 * density).toInt(), (6 * density).toInt())
                    setBackgroundResource(R.drawable.settings_tile_bg)
                }

                val indicator = TextView(requireContext()).apply {
                    text = "•"
                    textSize = 14f
                    setTextColor(ResourcesCompat.getColor(resources, R.color.accent, null))
                    layoutParams = LinearLayout.LayoutParams(
                        (24 * density).toInt(),
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    gravity = Gravity.CENTER
                }

                val titleText = TextView(requireContext()).apply {
                    text = event.title
                    textSize = 14f
                    setTextColor(ResourcesCompat.getColor(resources, R.color.text_primary, null))
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply { leftMargin = (8 * density).toInt() }
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }

                val editBtn = TextView(requireContext()).apply {
                    text = "Edit"
                    textSize = 12f
                    setTextColor(ResourcesCompat.getColor(resources, R.color.accent, null))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply { leftMargin = (8 * density).toInt() }
                    gravity = Gravity.CENTER
                    setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { showEditEventDialog(event) }
                }

                val deleteBtn = TextView(requireContext()).apply {
                    text = "Del"
                    textSize = 12f
                    setTextColor(ResourcesCompat.getColor(resources, R.color.text_secondary, null))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply { leftMargin = (4 * density).toInt() }
                    gravity = Gravity.CENTER
                    setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        events.remove(event)
                        rebuildCalendar()
                        showDayEventsDialog(dayNum)
                    }
                }

                eventRow.addView(indicator)
                eventRow.addView(titleText)
                eventRow.addView(editBtn)
                eventRow.addView(deleteBtn)
                contentView.addView(eventRow)
            }
        }

        val addBtn = TextView(requireContext()).apply {
            text = "+ Add Event"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(ResourcesCompat.getColor(resources, R.color.accent, null))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = (12 * density).toInt() }
            setPadding(0, (8 * density).toInt(), 0, (8 * density).toInt())
            isClickable = true
            isFocusable = true
            setOnClickListener { showNewEventDialog(dayNum) }
        }
        contentView.addView(addBtn)

        currentDialog = AlertDialog.Builder(requireContext())
            .setTitle(dateTitle)
            .setView(contentView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showEditEventDialog(event: CalendarEvent) {
        val density = resources.displayMetrics.density
        val input = EditText(requireContext()).apply {
            setText(event.title)
            inputType = InputType.TYPE_CLASS_TEXT
            setSelection(event.title.length)
            setPadding((12 * density).toInt(), (8 * density).toInt(), (12 * density).toInt(), (8 * density).toInt())
            setTextColor(ResourcesCompat.getColor(resources, R.color.text_primary, null))
            setHintTextColor(ResourcesCompat.getColor(resources, R.color.text_secondary, null))
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Event")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = input.text?.toString()?.trim() ?: ""
                if (newTitle.isNotEmpty()) {
                    val updatedEvent = event.copy(title = newTitle)
                    val idx = events.indexOf(event)
                    if (idx >= 0) {
                        events[idx] = updatedEvent
                    }
                    rebuildCalendar()
                    currentDialogDay?.let { showDayEventsDialog(it) }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNewEventDialog(dayNum: Int) {
        val density = resources.displayMetrics.density
        val input = EditText(requireContext()).apply {
            hint = "Event title"
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding((12 * density).toInt(), (8 * density).toInt(), (12 * density).toInt(), (8 * density).toInt())
            setTextColor(ResourcesCompat.getColor(resources, R.color.text_primary, null))
            setHintTextColor(ResourcesCompat.getColor(resources, R.color.text_secondary, null))
        }

        AlertDialog.Builder(requireContext())
            .setTitle("New Event")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val title = input.text?.toString()?.trim() ?: ""
                if (title.isNotEmpty()) {
                    val maxId = (events.maxOfOrNull { it.id } ?: 0) + 1
                    events.add(CalendarEvent(maxId, title, dayNum))
                    rebuildCalendar()
                    showDayEventsDialog(dayNum)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rebuildCalendar() {
        buildCalendar()
    }
}

