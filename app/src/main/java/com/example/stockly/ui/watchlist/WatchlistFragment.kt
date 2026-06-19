package com.example.stockly.ui.watchlist

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stockly.R
import com.example.stockly.data.local.entity.WatchlistEntity
import com.example.stockly.databinding.FragmentWatchlistBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WatchlistFragment : Fragment() {

    private var _binding: FragmentWatchlistBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WatchlistViewModel by viewModels()
    private lateinit var adapter: WatchlistAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWatchlistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        binding.buttonBrowseStocks.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }
    }

    private fun setupRecyclerView() {
        adapter = WatchlistAdapter { entity ->
            val bundle = Bundle().apply {
                putString("symbol", entity.symbol)
                putString("logoUrl", entity.logoUrl)
                putString("name", entity.name)
            }
            findNavController().navigate(R.id.action_watchlistFragment_to_stockDetailFragment, bundle)
        }
        binding.recyclerViewFavorites.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewFavorites.adapter = adapter
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
                    showEditNotesDialog(entity)
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
                            itemView.right - iconMargin - iconSize,
                            iconTop,
                            itemView.right - iconMargin,
                            iconTop + iconSize
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
                            itemView.left + iconMargin,
                            iconTop,
                            itemView.left + iconMargin + iconSize,
                            iconTop + iconSize
                        )
                        icon.draw(c)
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(callback).attachToRecyclerView(binding.recyclerViewFavorites)
    }

    private fun observeViewModel() {
        viewModel.watchlist.observe(viewLifecycleOwner) { watchlist ->
            adapter.submitList(watchlist)
            val isEmpty = watchlist.isEmpty()
            binding.textEmptyFavorites.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.nestedScrollFavorites.visibility = if (isEmpty) View.GONE else View.VISIBLE
            if (!isEmpty) {
                binding.textSavedCount.text = getString(R.string.watchlist_count, watchlist.size)
            }
        }
    }

    private fun showEditNotesDialog(entity: WatchlistEntity) {
        val inputLayout = TextInputLayout(
            requireContext(), null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            hint = getString(R.string.notes_hint)
            setPadding(48, 8, 48, 0)
        }
        val editText = TextInputEditText(requireContext()).apply {
            setText(entity.notes)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        inputLayout.addView(editText)

        MaterialAlertDialogBuilder(requireContext(), R.style.Stockly_AlertDialog)
            .setTitle(getString(R.string.edit_notes_title))
            .setView(inputLayout)
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                val newNotes = editText.text?.toString()?.trim() ?: ""
                viewModel.updateNotes(entity, newNotes)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteDialog(entity: WatchlistEntity) {
        MaterialAlertDialogBuilder(requireContext(), R.style.Stockly_AlertDialog_Destructive)
            .setTitle(getString(R.string.delete_favorite_title))
            .setMessage(getString(R.string.delete_favorite_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.removeFromWatchlist(entity)
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
