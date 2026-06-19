package com.example.stockly.ui.snapshot

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stockly.R
import com.example.stockly.data.local.entity.SnapshotEntity
import com.example.stockly.databinding.ItemSnapshotBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SnapshotAdapter(
    private val onItemClick: (SnapshotEntity) -> Unit
) : ListAdapter<SnapshotEntity, SnapshotAdapter.SnapshotViewHolder>(SnapshotDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())

    inner class SnapshotViewHolder(
        private val binding: ItemSnapshotBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entity: SnapshotEntity) {
            binding.textSnapshotDescription.text = entity.description
            binding.textSnapshotLocation.text = binding.root.context.getString(
                R.string.location_format, entity.latitude, entity.longitude
            )
            binding.textSnapshotDate.text = dateFormat.format(Date(entity.createdAt))
            Glide.with(binding.root.context)
                .load(Uri.parse(entity.photoUri))
                .placeholder(R.drawable.ic_placeholder_stock)
                .error(R.drawable.ic_placeholder_stock)
                .centerCrop()
                .into(binding.imageSnapshotPhoto)
            binding.root.setOnClickListener { onItemClick(entity) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SnapshotViewHolder {
        val binding = ItemSnapshotBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SnapshotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SnapshotViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class SnapshotDiffCallback : DiffUtil.ItemCallback<SnapshotEntity>() {
        override fun areItemsTheSame(oldItem: SnapshotEntity, newItem: SnapshotEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SnapshotEntity, newItem: SnapshotEntity) =
            oldItem == newItem
    }
}
