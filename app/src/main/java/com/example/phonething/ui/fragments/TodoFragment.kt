package com.example.phonething.ui.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import java.util.Calendar
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.phonething.R
import com.example.phonething.databinding.FragmentTodoBinding

class TodoFragment : Fragment() {

    private var _binding: FragmentTodoBinding? = null
    private val binding get() = _binding!!

    private val tasks = mutableListOf<TaskItem>()
    private var nextId = 1L

    private lateinit var doingAdapter: TodoAdapter
    private lateinit var doneAdapter: TodoAdapter
    private lateinit var doingTouchHelper: ItemTouchHelper
    private lateinit var doneTouchHelper: ItemTouchHelper

    // ─── "and X more tasks" indicator ────────────────────────────

    private fun updateMoreCount(recyclerView: RecyclerView, adapter: TodoAdapter, moreText: TextView) {
        val total = adapter.itemCount
        if (total <= 6) {
            moreText.visibility = View.GONE
            recyclerView.setPadding(
                recyclerView.paddingLeft,
                recyclerView.paddingTop,
                recyclerView.paddingRight,
                0
            )
            return
        }
        // Ensure bottom padding so items stop above the overlay line
        val overlayH = (resources.displayMetrics.density * 28).toInt()
        if (recyclerView.paddingBottom != overlayH) {
            recyclerView.setPadding(
                recyclerView.paddingLeft,
                recyclerView.paddingTop,
                recyclerView.paddingRight,
                overlayH
            )
        }
        // With clipToPadding + padding, standard visibility works correctly
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val lastVisible = layoutManager.findLastCompletelyVisibleItemPosition()
        if (lastVisible == RecyclerView.NO_POSITION) return
        val hidden = total - (lastVisible + 1)
        if (hidden > 0) {
            val word = if (hidden == 1) "task" else "tasks"
            moreText.text = "and $hidden more $word"
            moreText.visibility = View.VISIBLE
        } else {
            moreText.visibility = View.GONE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadTasks()
        setupLists()
        setupAddButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** Load tasks from persistent storage, seeding defaults on first launch. */
    private fun loadTasks() {
        val ctx = requireContext()
        tasks.clear()
        if (!StorageUtil.hasSeeded(ctx)) {
            val sample = listOf(
                TaskItem(1, "Paint the living room", false, "12 Feb"),
                TaskItem(2, "Fix the kitchen faucet", false, "15 Feb"),
                TaskItem(3, "Organize the garage", false, "20 Feb"),
                TaskItem(4, "Replace air filters", false, "10 Mar"),
                TaskItem(5, "Clean the gutters", false, null),
                TaskItem(6, "Mow the lawn", false, "5 Apr"),
                TaskItem(7, "Trim the hedges", false, null),
                TaskItem(8, "Wash the windows", false, null),
                TaskItem(9, "Download the app", true, null),
                TaskItem(10, "Install the stand", true, null),
                TaskItem(11, "Mount the phone", true, null),
            )
            tasks.addAll(sample)
            nextId = 12L
            StorageUtil.saveTasks(ctx, tasks)
            StorageUtil.markSeeded(ctx)
        } else {
            val loaded = StorageUtil.loadTasks(ctx)
            tasks.addAll(loaded)
            nextId = (loaded.maxOfOrNull { it.id } ?: 0) + 1
        }
    }

    private fun saveTasks() {
        StorageUtil.saveTasks(requireContext(), tasks)
    }

    private fun setupLists() {
        doingAdapter = TodoAdapter(
            getDoingItems(),
            onToggle = { task -> toggleTask(task) },
            onEdit = { task -> showEditTaskDialog(task) },
            onDelete = { task -> deleteTask(task) },
            onStartDrag = { holder -> doingTouchHelper.startDrag(holder) }
        )
        doneAdapter = TodoAdapter(
            getDoneItems(),
            onToggle = { task -> toggleTask(task) },
            onEdit = { task -> showEditTaskDialog(task) },
            onDelete = { task -> deleteTask(task) },
            onStartDrag = { holder -> doneTouchHelper.startDrag(holder) }
        )

        binding.doingList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = doingAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    updateMoreCount(recyclerView, doingAdapter, binding.doingMoreText)
                }
            })
        }
        binding.doneList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = doneAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    updateMoreCount(recyclerView, doneAdapter, binding.doneMoreText)
                }
            })
        }

        // Initial "and X more" state — deferred until after layout
        binding.doingList.post { updateMoreCount(binding.doingList, doingAdapter, binding.doingMoreText) }
        binding.doneList.post { updateMoreCount(binding.doneList, doneAdapter, binding.doneMoreText) }

        // Set up drag-and-drop via ItemTouchHelper
        // Live reorder with Trello-style visual feedback.
        doingTouchHelper = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
                override fun isLongPressDragEnabled(): Boolean = false // we use manual startDrag from adapter

                override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                    return doingAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition())
                }
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                        doingAdapter.hideActionRow()
                        viewHolder?.itemView?.let { v ->
                            v.alpha = 0.85f
                            v.elevation = v.context.resources.displayMetrics.density * 8
                        }
                    }
                }
                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    super.clearView(recyclerView, viewHolder)
                    viewHolder.itemView.let { v ->
                        v.alpha = 1.0f
                        v.elevation = 0f
                    }
                    doingAdapter.refreshNumbers()
                    recyclerView.post { updateMoreCount(recyclerView, doingAdapter, binding.doingMoreText) }
                }
                override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    if (isCurrentlyActive) {
                        viewHolder.itemView.elevation = viewHolder.itemView.context.resources.displayMetrics.density * 12
                    }
                }
            }
        )
        doingTouchHelper.attachToRecyclerView(binding.doingList)

        doneTouchHelper = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
                override fun isLongPressDragEnabled(): Boolean = false // we use manual startDrag from adapter

                override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                    return doneAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition())
                }
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                        doneAdapter.hideActionRow()
                        viewHolder?.itemView?.let { v ->
                            v.alpha = 0.85f
                            v.elevation = v.context.resources.displayMetrics.density * 8
                        }
                    }
                }
                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    super.clearView(recyclerView, viewHolder)
                    viewHolder.itemView.let { v ->
                        v.alpha = 1.0f
                        v.elevation = 0f
                    }
                    doneAdapter.refreshNumbers()
                    recyclerView.post { updateMoreCount(recyclerView, doneAdapter, binding.doneMoreText) }
                }
                override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    if (isCurrentlyActive) {
                        viewHolder.itemView.elevation = viewHolder.itemView.context.resources.displayMetrics.density * 12
                    }
                }
            }
        )
        doneTouchHelper.attachToRecyclerView(binding.doneList)
    }

    private fun getDoingItems() = tasks
        .filter { !it.isDone }
        .toMutableList()

    private fun getDoneItems() = tasks
        .filter { it.isDone }
        .toMutableList()

    private fun toggleTask(task: TaskItem) {
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index == -1) return
        val toggled = tasks[index].copy(isDone = !tasks[index].isDone)
        // Always move toggled task to the end so both directions go bottom
        tasks.removeAt(index)
        tasks.add(toggled)
        saveTasks()

        doingAdapter.replaceAll(getDoingItems())
        doneAdapter.replaceAll(getDoneItems())
        // Defer indicator update until after RecyclerView lays out
        binding.doingList.post { updateMoreCount(binding.doingList, doingAdapter, binding.doingMoreText) }
        binding.doneList.post { updateMoreCount(binding.doneList, doneAdapter, binding.doneMoreText) }
    }

    private fun setupAddButton() {
        binding.addButton.setOnClickListener { showAddTaskDialog() }
    }

    // ─── Edit / Delete ─────────────────────────────────────────────

    private fun showEditTaskDialog(task: TaskItem) {
        val input = buildEditText().apply { setText(task.text) }
        val dateInput = buildDueDateEditText().apply { setText(task.dueDate) }
        val container = buildDialogContainer(input, dateInput)

        AlertDialog.Builder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_Dialog)
            .setTitle("Edit Task")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) editTask(task, text, dateInput.text.toString().trim())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTask(task: TaskItem) {
        tasks.removeAll { it.id == task.id }
        saveTasks()
        doingAdapter.replaceAll(getDoingItems())
        doneAdapter.replaceAll(getDoneItems())
        binding.doingList.post { updateMoreCount(binding.doingList, doingAdapter, binding.doingMoreText) }
        binding.doneList.post { updateMoreCount(binding.doneList, doneAdapter, binding.doneMoreText) }
    }

    private fun editTask(task: TaskItem, newText: String, newDueDate: String) {
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index == -1) return
        val dueDate = if (newDueDate.isEmpty()) null else newDueDate
        tasks[index] = tasks[index].copy(text = newText, dueDate = dueDate)
        saveTasks()

        doingAdapter.replaceAll(getDoingItems())
        doneAdapter.replaceAll(getDoneItems())
        binding.doingList.post { updateMoreCount(binding.doingList, doingAdapter, binding.doingMoreText) }
        binding.doneList.post { updateMoreCount(binding.doneList, doneAdapter, binding.doneMoreText) }
    }

    // ─── Add-task dialog ──────────────────────────────────────────

    private fun showAddTaskDialog() {
        val input = buildEditText()
        val dateInput = buildDueDateEditText()
        val container = buildDialogContainer(input, dateInput)

        AlertDialog.Builder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_Dialog)
            .setTitle("New Task")
            .setView(container)
            .setPositiveButton("Add") { _, _ ->
                val text = input.text.toString().trim()
                val dueDate = dateInput.text.toString().trim()
                if (text.isNotEmpty()) addTask(text, dueDate)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addTask(text: String, dueDate: String) {
        val date = if (dueDate.isEmpty()) null else dueDate
        tasks.add(TaskItem(nextId++, text, false, date))
        saveTasks()
        doingAdapter.replaceAll(getDoingItems())
        binding.doingList.post { updateMoreCount(binding.doingList, doingAdapter, binding.doingMoreText) }
    }

    // ─── Reusable dialog widgets ──────────────────────────────────

    private fun buildEditText(): EditText {
        val density = resources.displayMetrics.density
        return EditText(requireContext()).apply {
            setHint("What needs to be done?")
            setHintTextColor(
                ResourcesCompat.getColor(resources, R.color.text_secondary, null)
            )
            setTextColor(
                ResourcesCompat.getColor(resources, R.color.text_primary, null)
            )
            setPadding(
                (12 * density).toInt(),
                (12 * density).toInt(),
                (12 * density).toInt(),
                (12 * density).toInt()
            )
            minLines = 3
            textSize = 16f
        }
    }

    private fun buildDueDateEditText(): EditText {
        val density = resources.displayMetrics.density
        val dateFormat = java.text.SimpleDateFormat("d MMM", java.util.Locale.US)
        return EditText(requireContext()).apply {
            setHint("Tap to pick due date")
            setHintTextColor(
                ResourcesCompat.getColor(resources, R.color.text_secondary, null)
            )
            setTextColor(
                ResourcesCompat.getColor(resources, R.color.text_primary, null)
            )
            setPadding(
                (12 * density).toInt(),
                (12 * density).toInt(),
                (12 * density).toInt(),
                (12 * density).toInt()
            )
            maxLines = 1
            textSize = 16f
            // Make it non-editable — selection comes from picker
            isFocusable = false
            isClickable = true
            isLongClickable = false
            setOnClickListener {
                val cal = Calendar.getInstance()
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        cal.set(Calendar.YEAR, year)
                        cal.set(Calendar.MONTH, month)
                        cal.set(Calendar.DAY_OF_MONTH, day)
                        setText(dateFormat.format(cal.time))
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        }
    }

    private fun buildDialogContainer(input: EditText, dateInput: EditText): android.widget.LinearLayout {
        val density = resources.displayMetrics.density
        val hm = (24 * density).toInt()
        val vm = (8 * density).toInt()

        return android.widget.LinearLayout(requireContext()).apply {
            layoutParams = android.view.ViewGroup.MarginLayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(hm, vm, hm, vm)
            addView(input)
            addView(dateInput)
        }
    }
}
