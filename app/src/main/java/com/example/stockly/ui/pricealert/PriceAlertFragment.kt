package com.example.stockly.ui.pricealert

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stockly.R
import com.example.stockly.data.local.entity.PriceAlertEntity
import com.example.stockly.databinding.FragmentPriceAlertBinding
import com.example.stockly.util.Resource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PriceAlertFragment : Fragment() {

    private var _binding: FragmentPriceAlertBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PriceAlertViewModel by viewModels()
    private lateinit var adapter: PriceAlertAdapter
    private var editingAlert: PriceAlertEntity? = null

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            showListPanel()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPriceAlertBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)
        setupRecyclerView()
        setupDropdown()
        setupThresholdChips()
        setupSubmitButtonState()
        observeViewModel()

        binding.fabAddAlert.setOnClickListener {
            editingAlert = null
            resetForm()
            binding.textAlertFormTitle.text = getString(R.string.label_new_alert)
            showFormPanel()
        }

        binding.buttonBack.setOnClickListener { showListPanel() }

        binding.buttonSubmitAlert.setOnClickListener {
            val symbol = binding.editTextSymbol.text?.toString()?.trim() ?: ""
            val condition = binding.autoCompleteCondition.text?.toString()?.trim() ?: ""
            val price = binding.editTextQuantity.text?.toString()?.trim() ?: ""
            val note = binding.editTextNote.text?.toString()?.trim() ?: ""
            clearErrors()
            viewModel.submitAlert(editingAlert?.id, symbol, condition, price, note)
        }
    }

    private fun setupRecyclerView() {
        adapter = PriceAlertAdapter()
        binding.recyclerViewAlerts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewAlerts.adapter = adapter
        attachSwipeHelper()
    }

    private fun attachSwipeHelper() {
        val deleteColor = ContextCompat.getColor(requireContext(), R.color.urgency_red)
        val editColor = ContextCompat.getColor(requireContext(), R.color.text_muted)
        val trashIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_trash)
        val editIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_edit)

        val callback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            private val bgPaint = Paint().apply { isAntiAlias = true }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.layoutPosition
                if (position == RecyclerView.NO_POSITION) return
                val entity = adapter.currentList[position]
                if (direction == ItemTouchHelper.LEFT) {
                    showDeleteDialog(entity)
                } else {
                    adapter.notifyItemChanged(position)
                    editingAlert = entity
                    preFillForm(entity)
                    binding.textAlertFormTitle.text = getString(R.string.label_edit_alert)
                    showFormPanel()
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val cornerRadius = 18f * resources.displayMetrics.density

                if (dX < 0) {
                    bgPaint.color = deleteColor
                    val bg = RectF(
                        itemView.right + dX, itemView.top.toFloat(),
                        itemView.right.toFloat(), itemView.bottom.toFloat()
                    )
                    c.drawRoundRect(bg, cornerRadius, cornerRadius, bgPaint)
                    trashIcon?.let { icon ->
                        icon.colorFilter = PorterDuffColorFilter(0xFFFFFFFF.toInt(), PorterDuff.Mode.SRC_IN)
                        val iconSize = 24 * resources.displayMetrics.density.toInt()
                        val iconMargin = 24 * resources.displayMetrics.density.toInt()
                        val iconTop = itemView.top + (itemView.height - iconSize) / 2
                        icon.setBounds(
                            itemView.right - iconMargin - iconSize, iconTop,
                            itemView.right - iconMargin, iconTop + iconSize
                        )
                        icon.draw(c)
                    }
                } else if (dX > 0) {
                    bgPaint.color = editColor
                    val bg = RectF(
                        itemView.left.toFloat(), itemView.top.toFloat(),
                        itemView.left + dX, itemView.bottom.toFloat()
                    )
                    c.drawRoundRect(bg, cornerRadius, cornerRadius, bgPaint)
                    editIcon?.let { icon ->
                        icon.colorFilter = PorterDuffColorFilter(0xFFFFFFFF.toInt(), PorterDuff.Mode.SRC_IN)
                        val iconSize = 24 * resources.displayMetrics.density.toInt()
                        val iconMargin = 24 * resources.displayMetrics.density.toInt()
                        val iconTop = itemView.top + (itemView.height - iconSize) / 2
                        icon.setBounds(
                            itemView.left + iconMargin, iconTop,
                            itemView.left + iconMargin + iconSize, iconTop + iconSize
                        )
                        icon.draw(c)
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(callback).attachToRecyclerView(binding.recyclerViewAlerts)
    }

    private fun observeViewModel() {
        viewModel.alerts.observe(viewLifecycleOwner) { alerts ->
            adapter.submitList(alerts)
            val isEmpty = alerts.isEmpty()
            binding.layoutEmptyAlerts.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.textAlertsCount.text = if (isEmpty) "" else getString(R.string.label_alerts_count, alerts.size)
        }

        viewModel.submissionState.observe(viewLifecycleOwner) { resource ->
            resource ?: return@observe
            when (resource) {
                is Resource.Loading -> {
                    binding.buttonSubmitAlert.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.INVISIBLE
                    val message = if (editingAlert != null) {
                        getString(R.string.alert_updated)
                    } else {
                        getString(R.string.alert_success)
                    }
                    showListPanel()
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.change_positive))
                        .setAnchorView(requireActivity().findViewById(R.id.bottom_nav))
                        .show()
                    viewModel.resetState()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.INVISIBLE
                    updateSubmitButton()
                    when (resource.message) {
                        "symbol" -> binding.inputLayoutSymbol.error = getString(R.string.error_symbol_required)
                        "invalid_symbol" -> binding.inputLayoutSymbol.error = getString(R.string.error_symbol_not_in_app)
                        "condition" -> binding.inputLayoutCondition.error = getString(R.string.error_condition_required)
                        "price" -> binding.inputLayoutQuantity.error = getString(R.string.error_invalid_price)
                        else -> Snackbar.make(binding.root, getString(R.string.error_loading_data), Snackbar.LENGTH_LONG)
                            .setAnchorView(requireActivity().findViewById(R.id.bottom_nav))
                            .show()
                    }
                    viewModel.resetState()
                }
            }
        }
    }

    private fun setupSubmitButtonState() {
        binding.buttonSubmitAlert.isEnabled = false
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { updateSubmitButton() }
        }
        binding.editTextSymbol.addTextChangedListener(watcher)
        binding.autoCompleteCondition.addTextChangedListener(watcher)
        binding.editTextQuantity.addTextChangedListener(watcher)
    }

    private fun updateSubmitButton() {
        val symbol = binding.editTextSymbol.text?.toString()?.trim() ?: ""
        val condition = binding.autoCompleteCondition.text?.toString()?.trim() ?: ""
        val price = binding.editTextQuantity.text?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
        binding.buttonSubmitAlert.isEnabled = symbol.isNotEmpty() && condition.isNotEmpty() && price > 0.0
    }

    private fun setupDropdown() {
        val alertConditions = resources.getStringArray(R.array.alert_conditions)
        val dropAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, alertConditions)
        binding.autoCompleteCondition.setAdapter(dropAdapter)
    }

    private fun setupThresholdChips() {
        selectChip(binding.chipAmount50)
        binding.editTextQuantity.setText(getString(R.string.amount_50_value))

        binding.chipAmount25.setOnClickListener {
            selectChip(binding.chipAmount25)
            binding.editTextQuantity.setText(getString(R.string.amount_25_value))
        }
        binding.chipAmount50.setOnClickListener {
            selectChip(binding.chipAmount50)
            binding.editTextQuantity.setText(getString(R.string.amount_50_value))
        }
        binding.chipAmount100.setOnClickListener {
            selectChip(binding.chipAmount100)
            binding.editTextQuantity.setText(getString(R.string.amount_100_value))
        }

        binding.editTextQuantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: ""
                val matchesChip = text == getString(R.string.amount_25_value) ||
                    text == getString(R.string.amount_50_value) ||
                    text == getString(R.string.amount_100_value)
                if (!matchesChip) clearChipSelection()
            }
        })
    }

    private fun selectChip(selected: android.widget.TextView) {
        val chips = listOf(binding.chipAmount25, binding.chipAmount50, binding.chipAmount100)
        chips.forEach { chip ->
            if (chip == selected) {
                chip.setBackgroundResource(R.drawable.bg_amount_chip_selected)
                chip.setTextColor(requireContext().getColor(R.color.primary))
            } else {
                chip.setBackgroundResource(R.drawable.bg_amount_chip_default)
                chip.setTextColor(requireContext().getColor(R.color.text_muted))
            }
        }
    }

    private fun clearChipSelection() {
        listOf(binding.chipAmount25, binding.chipAmount50, binding.chipAmount100).forEach { chip ->
            chip.setBackgroundResource(R.drawable.bg_amount_chip_default)
            chip.setTextColor(requireContext().getColor(R.color.text_muted))
        }
    }

    private fun clearErrors() {
        binding.inputLayoutSymbol.error = null
        binding.inputLayoutCondition.error = null
        binding.inputLayoutQuantity.error = null
    }

    private fun preFillForm(entity: PriceAlertEntity) {
        binding.editTextSymbol.setText(entity.symbol)
        binding.autoCompleteCondition.setText(entity.condition, false)
        binding.editTextQuantity.setText(entity.targetPrice.toInt().toString())
        binding.editTextNote.setText(entity.note)
        clearChipSelection()
        clearErrors()
    }

    private fun resetForm() {
        binding.editTextSymbol.text?.clear()
        binding.autoCompleteCondition.text?.clear()
        binding.editTextNote.text?.clear()
        selectChip(binding.chipAmount50)
        binding.editTextQuantity.setText(getString(R.string.amount_50_value))
        clearErrors()
    }

    private fun showListPanel() {
        binding.layoutList.visibility = View.VISIBLE
        binding.layoutAddForm.visibility = View.GONE
        binding.fabAddAlert.visibility = View.VISIBLE
        backCallback.isEnabled = false
        editingAlert = null
        adapter.notifyDataSetChanged()
    }

    private fun showFormPanel() {
        binding.layoutList.visibility = View.GONE
        binding.layoutAddForm.visibility = View.VISIBLE
        binding.fabAddAlert.visibility = View.GONE
        backCallback.isEnabled = true
    }

    private fun showDeleteDialog(entity: PriceAlertEntity) {
        MaterialAlertDialogBuilder(requireContext(), R.style.Stockly_AlertDialog_Destructive)
            .setTitle(getString(R.string.delete_alert_title))
            .setMessage(getString(R.string.delete_alert_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteAlert(entity)
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                adapter.notifyDataSetChanged()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
