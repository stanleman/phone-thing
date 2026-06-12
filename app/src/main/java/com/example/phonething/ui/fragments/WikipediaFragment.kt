package com.example.phonething.ui.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.phonething.databinding.FragmentWikipediaBinding
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WikipediaFragment : Fragment() {

    private var _binding: FragmentWikipediaBinding? = null
    private val binding get() = _binding!!

    private val mainHandler = Handler(Looper.getMainLooper())
    private var fetchThread: Thread? = null

    private val apiDateFormatter = SimpleDateFormat("yyyy/MM/dd", Locale.US)
    private val tfaDateFormatter = SimpleDateFormat("MMMM d, yyyy", Locale.US)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWikipediaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.wikiError.setOnClickListener { fetchArticle() }
        fetchArticle()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fetchThread?.interrupt()
        fetchThread = null
        _binding = null
    }

    private fun fetchArticle() {
        binding.wikiLoading.visibility = View.VISIBLE
        binding.wikiTitle.text = ""
        binding.wikiExtract.text = ""
        binding.wikiError.visibility = View.GONE

        fetchThread = Thread {
            try {
                // Step 1: Get today's featured article title from the feed API
                val dateStr = apiDateFormatter.format(Date())
                val feedUrl = URL("https://en.wikipedia.org/api/rest_v1/feed/featured/$dateStr")
                val feedConn = feedUrl.openConnection() as HttpURLConnection
                feedConn.connectTimeout = 10_000
                feedConn.readTimeout = 10_000
                feedConn.requestMethod = "GET"
                feedConn.setRequestProperty("User-Agent", "PhoneThing/1.0 (Android)")

                val feedReader = BufferedReader(InputStreamReader(feedConn.inputStream, "utf-8"))
                val feedResponse = feedReader.readText()
                feedReader.close()

                val feedJson = JSONObject(feedResponse)
                val tfa = feedJson.getJSONObject("tfa")
                val title = tfa.optString("normalizedtitle", tfa.getString("title"))

                // Step 2: Get the actual TFA blurb from the main page transclusion
                val tfaDate = tfaDateFormatter.format(Date())
                val tfaPage = java.net.URLEncoder.encode("Wikipedia:Today's featured article/$tfaDate", "utf-8")
                val parseUrl = URL("https://en.wikipedia.org/w/api.php?action=parse&page=$tfaPage&prop=text&format=json")
                val parseConn = parseUrl.openConnection() as HttpURLConnection
                parseConn.connectTimeout = 10_000
                parseConn.readTimeout = 10_000
                parseConn.requestMethod = "GET"
                parseConn.setRequestProperty("User-Agent", "PhoneThing/1.0 (Android)")

                val parseReader = BufferedReader(InputStreamReader(parseConn.inputStream, "utf-8"))
                val parseResponse = parseReader.readText()
                parseReader.close()

                val parseJson = JSONObject(parseResponse)
                var html = parseJson.getJSONObject("parse").getJSONObject("text").getString("*")

                // Strip everything after "Recently featured"
                val recentIdx = html.indexOf("Recently featured")
                if (recentIdx > 0) html = html.substring(0, recentIdx)

                // Remove the image caption (<div class="thumbcaption">...</div>)
                val captionStart = html.indexOf("<div class=\"thumbcaption\"")
                if (captionStart >= 0) {
                    val captionEnd = html.indexOf("</div>", captionStart)
                    if (captionEnd >= 0) {
                        html = html.substring(0, captionStart) + html.substring(captionEnd + 6)
                    }
                }

                // Clean HTML to plain text
                html = html.replace("<br>", "\n")
                    .replace("<br/>", "\n")
                    .replace("<br />", "\n")
                    .replace("</p>", "\n\n")
                    .replace("</div>", "\n")
                    .replace(Regex("<[^>]+>"), "")
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&#39;", "'")
                    .replace("&nbsp;", " ")
                    .replace(Regex("\\s*\\(Full\\s*article\\.\\.\\.\\)\\s*"), "")
                    .replace(Regex("\\n{3,}"), "\n\n")
                    .trim()

                mainHandler.post {
                    if (_binding == null) return@post
                    binding.wikiTitle.text = title
                    binding.wikiExtract.text = html
                    binding.wikiLoading.visibility = View.GONE
                    // Fit text size after layout is complete
                    binding.wikiExtract.post { fitTextSize(binding.wikiExtract) }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    if (_binding == null) return@post
                    binding.wikiLoading.visibility = View.GONE
                    binding.wikiError.text = "Tap to retry — ${e.localizedMessage ?: "Could not load"}"
                    binding.wikiError.visibility = View.VISIBLE
                }
            }
        }
        fetchThread?.start()
    }

    private fun fitTextSize(textView: TextView, minSp: Float = 8f, maxSp: Float = 40f) {
        val availableWidth = textView.width - textView.paddingLeft - textView.paddingRight
        val availableHeight = textView.height - textView.paddingTop - textView.paddingBottom

        if (availableWidth <= 0 || availableHeight <= 0) return
        if (textView.text.isNullOrEmpty()) return

        var low = minSp
        var high = maxSp
        var bestSize = minSp

        while (high - low > 0.25f) {
            val mid = (low + high) / 2f
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mid)

            textView.measure(
                View.MeasureSpec.makeMeasureSpec(availableWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )

            if (textView.measuredHeight <= availableHeight) {
                bestSize = mid
                low = mid
            } else {
                high = mid
            }
        }

        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, bestSize)
        textView.requestLayout()
    }
}
