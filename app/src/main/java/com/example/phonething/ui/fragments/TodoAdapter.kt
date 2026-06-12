package com.example.phonething.ui.fragments

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.phonething.R

data class TaskItem(
    val id: Long,
    val text: String,
    val isDone: Boolean,
    val dueDate: String? = null
)

class TodoAdapter(
    items: MutableList<TaskItem>,
    private val onToggle: (TaskItem) -> Unit,
    private val onEdit: (TaskItem) -> Unit,
    private val onDelete: (TaskItem) -> Unit,
    private val onStartDrag: ((RecyclerView.ViewHolder) -> Unit)? = null
) : RecyclerView.Adapter<TodoAdapter.ViewHolder>() {

    var items: MutableList<TaskItem> = items
        private set

    /** Tracks which position has an open action row; -1 = none */
    private var openActionPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, position + 1, position == openActionPosition, onToggle, onEdit, onDelete, onStartDrag) {
            val pos = holder.getAdapterPosition()
            if (pos == RecyclerView.NO_POSITION) return@bind
            if (openActionPosition == pos) {
                openActionPosition = -1
                notifyItemChanged(pos)
            } else {
                val oldPos = if (openActionPosition != -1) openActionPosition else -1
                openActionPosition = pos
                if (oldPos != -1) notifyItemChanged(oldPos)
                notifyItemChanged(pos)
            }
        }
    }

    override fun getItemCount() = items.size

    fun replaceAll(newItems: List<TaskItem>) {
        items.clear()
        items.addAll(newItems)
        openActionPosition = -1
        notifyDataSetChanged()
    }

    fun hideActionRow() {
        if (openActionPosition != -1) {
            val pos = openActionPosition
            openActionPosition = -1
            notifyItemChanged(pos)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val mainRow: LinearLayout = view.findViewById(R.id.mainRow)
        private val numberView: TextView = view.findViewById(R.id.taskNumber)
        private val textView: TextView = view.findViewById(R.id.taskText)
        private val dueDateView: TextView = view.findViewById(R.id.taskDueDate)
        private val actionRow: LinearLayout = view.findViewById(R.id.actionRow)
        private val editBtn: TextView = view.findViewById(R.id.editBtn)
        private val deleteBtn: TextView = view.findViewById(R.id.deleteBtn)

        private var lastClickTime = 0L
        private val doubleTapThreshold = 300L
        private val handler = Handler(Looper.getMainLooper())
        private var pendingToggle: Runnable? = null

        fun bind(
            item: TaskItem,
            number: Int,
            isActionOpen: Boolean,
            onToggle: (TaskItem) -> Unit,
            onEdit: (TaskItem) -> Unit,
            onDelete: (TaskItem) -> Unit,
            onStartDrag: ((RecyclerView.ViewHolder) -> Unit)?,
            onActionToggled: () -> Unit
        ) {
            val res = itemView.resources
            textView.text = item.text
            numberView.text = number.toString()

            // Due date
            if (item.dueDate != null) {
                dueDateView.text = "due ${item.dueDate}"
                dueDateView.visibility = View.VISIBLE
            } else {
                dueDateView.visibility = View.GONE
            }

            val dimAlpha = if (item.isDone) 0.45f else 1.0f
            textView.alpha = dimAlpha
            numberView.alpha = dimAlpha
            dueDateView.alpha = dimAlpha
            numberView.setTextColor(
                ResourcesCompat.getColor(res, R.color.text_secondary, null)
            )

            actionRow.visibility = if (isActionOpen) View.VISIBLE else View.GONE

            // ── Long-press on drag handle or row to reorder ─────
            mainRow.setOnLongClickListener {
                onStartDrag?.invoke(this@ViewHolder)
                true
            }

            // ── Double-tap detection ────────────────────────────
            mainRow.setOnClickListener {
                val now = System.currentTimeMillis()
                if (now - lastClickTime < doubleTapThreshold) {
                    // Double-tap → cancel toggle, show actions
                    pendingToggle?.let { handler.removeCallbacks(it) }
                    pendingToggle = null
                    onActionToggled()
                } else {
                    pendingToggle?.let { handler.removeCallbacks(it) }
                    pendingToggle = Runnable {
                        onToggle(item)
                    }
                    handler.postDelayed(pendingToggle!!, doubleTapThreshold)
                }
                lastClickTime = now
            }

            // ── Action buttons ──────────────────────────────────
            editBtn.setOnClickListener {
                onActionToggled()
                onEdit(item)
            }
            deleteBtn.setOnClickListener {
                onActionToggled()
                onDelete(item)
            }
        }
    }

    // Called by ItemTouchHelper during drag — live reorder with notifyItemMoved only.
    // notifyItemMoved triggers smooth visual shifts without rebinding views,
    // which would break the active drag session.
    fun onItemMove(from: Int, to: Int): Boolean {
        if (from == to) return false
        openActionPosition = -1
        val item = items.removeAt(from)
        items.add(to, item)
        notifyItemMoved(from, to)
        return true
    }

    /** Refresh numbering after drag completes. Call from ItemTouchHelper.clearView. */
    fun refreshNumbers() {
        notifyDataSetChanged()
    }
}

