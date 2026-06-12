package com.example.phonething.ui.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.phonething.databinding.FragmentClockBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ClockFragment : Fragment() {

    private var _binding: FragmentClockBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private val hoursFormatter = SimpleDateFormat("HH", Locale.US)
    private val minutesFormatter = SimpleDateFormat("mm", Locale.US)
    private val dayOfWeekFormatter = SimpleDateFormat("EEEE", Locale.US)
    private val dateFormatter = SimpleDateFormat("MMM — d", Locale.US)

    private var lastMinute = -1
    private var slotHeight = 0f
    private var isAnimating = false

    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            updateTime()
            handler.postDelayed(this, 1_000) // check every second for precise flip
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.minutesStack.post {
            measureSlotHeight()
            updateTime()
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateTimeRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateTimeRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateTimeRunnable)
        _binding = null
    }

    private fun measureSlotHeight() {
        // Distance between the visual centers of consecutive slots
        slotHeight = (binding.clockMinutes.y - binding.clockPrevMinute.y).toFloat()
    }

    private fun updateTime() {
        val now = Date()
        val cal = Calendar.getInstance()
        val minute = cal.get(Calendar.MINUTE)

        // Always update static elements (hours, date — never animate these)
        binding.clockHours.text = hoursFormatter.format(now)
        binding.clockDayOfWeek.text = dayOfWeekFormatter.format(now).uppercase()
        binding.clockDate.text = dateFormatter.format(now).uppercase()

        // First run — just set values, no animation
        if (lastMinute == -1) {
            lastMinute = minute
            val minuteStr = minutesFormatter.format(now)
            val prevMinute = if (minute == 0) 59 else minute - 1
            val nextMinute = if (minute == 59) 0 else minute + 1
            binding.clockMinutes.text = minuteStr
            binding.clockPrevMinute.text = String.format("%02d", prevMinute)
            binding.clockNextMinute.text = String.format("%02d", nextMinute)
            return
        }

        // No minute change — nothing to animate
        if (minute == lastMinute || slotHeight <= 0f || isAnimating) return

        isAnimating = true
        val prevMinute = if (minute == 0) 59 else minute - 1
        val nextMinute = if (minute == 59) 0 else minute + 1

        val oldCurrent = binding.clockMinutes.text.toString()
        val oldNext = binding.clockNextMinute.text.toString()
        val newNextStr = String.format("%02d", nextMinute)

        // ── Pre-animation setup ──────────────────────────────────

        // The new bottom element starts invisible, shifted down by slotHeight.
        // Its textSize is already 48sp (preview size), so scale stays 1.0.
        binding.clockNewMinute.apply {
            text = newNextStr
            translationY = slotHeight
            scaleX = 1f
            scaleY = 1f
            alpha = 0f
            visibility = View.VISIBLE
        }

        // The old-next (currently at bottom, textSize 48sp) becomes the new current.
        // It needs to visually grow to 120sp → scale from 1.0 to 2.5.
        // It already shows old_next text, no text change needed.

        // The old-current (currently at center, textSize 120sp) becomes the new prev.
        // It needs to visually shrink to 48sp → scale from 1.0 to 0.4.
        // No text change needed yet.

        // The old-prev (currently at top, textSize 48sp) slides out. No change needed.

        // ── Animate ──────────────────────────────────────────────

        val duration = 420L
        val interpolator = DecelerateInterpolator()

        binding.clockPrevMinute.animate()
            .translationYBy(-slotHeight)
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .start()

        binding.clockMinutes.animate()
            .translationYBy(-slotHeight)
            .alpha(0.35f)
            .scaleX(0.4f).scaleY(0.4f)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .start()

        binding.clockNextMinute.animate()
            .translationYBy(-slotHeight)
            .alpha(1.0f)
            .scaleX(2.5f).scaleY(2.5f)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .start()

        binding.clockNewMinute.animate()
            .translationY(0f)
            .alpha(0.35f)
            .scaleX(1f).scaleY(1f)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .withEndAction {
                // Reset all transforms and update texts to final state.
                // After reset, each view's native textSize (from XML) determines
                // the rendered size — texts simply move between slots.
                binding.clockPrevMinute.text = oldCurrent
                binding.clockMinutes.text = oldNext
                binding.clockNextMinute.text = newNextStr

                resetView(binding.clockPrevMinute, 0.35f)
                resetView(binding.clockMinutes, 1.0f)
                resetView(binding.clockNextMinute, 0.35f)

                binding.clockNewMinute.visibility = View.GONE
                binding.clockNewMinute.translationY = 0f

                lastMinute = minute
                isAnimating = false
            }
            .start()
    }

    private fun resetView(view: TextView, targetAlpha: Float) {
        view.translationY = 0f
        view.alpha = targetAlpha
        view.scaleX = 1f
        view.scaleY = 1f
    }
}
