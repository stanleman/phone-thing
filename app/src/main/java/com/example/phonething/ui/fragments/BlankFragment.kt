package com.example.phonething.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

/**
 * Rendered in place of a page that the user has disabled in Settings.
 */
class BlankFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return View(requireContext()).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
    }
}
