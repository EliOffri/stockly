package com.example.stockly.ui.pricealert

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stockly.R
import com.example.stockly.data.local.entity.PriceAlertEntity
import com.example.stockly.databinding.ItemPriceAlertBinding

class PriceAlertAdapter : ListAdapter<PriceAlertEntity, PriceAlertAdapter.AlertViewHolder>(AlertDiffCallback()) {

    inner class AlertViewHolder(
        private val binding: ItemPriceAlertBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entity: PriceAlertEntity) {
            binding.textAlertSymbol.text = entity.symbol
            binding.textAlertCondition.text = binding.root.context.getString(
                R.string.condition_format, entity.condition, entity.targetPrice
            )
            binding.textAlertNote.text = entity.note.ifBlank {
                binding.root.context.getString(R.string.no_notes)
            }
            Glide.with(binding.root.context)
                .load(entity.logoUrl.ifBlank { null })
                .placeholder(R.drawable.ic_placeholder_stock)
                .error(R.drawable.ic_placeholder_stock)
                .centerInside()
                .into(binding.imageAlertLogo)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val binding = ItemPriceAlertBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AlertViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class AlertDiffCallback : DiffUtil.ItemCallback<PriceAlertEntity>() {
        override fun areItemsTheSame(oldItem: PriceAlertEntity, newItem: PriceAlertEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: PriceAlertEntity, newItem: PriceAlertEntity) =
            oldItem == newItem
    }
}
