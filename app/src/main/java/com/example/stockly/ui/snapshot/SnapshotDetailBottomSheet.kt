package com.example.stockly.ui.snapshot

import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.stockly.R
import com.example.stockly.databinding.BottomSheetSnapshotDetailBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SnapshotDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSnapshotDetailBinding? = null
    private val binding get() = _binding!!

    private val dateFormat = SimpleDateFormat("EEE, MMM dd yyyy · HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSnapshotDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        val photoUri = args.getString(ARG_PHOTO_URI) ?: ""
        val description = args.getString(ARG_DESCRIPTION) ?: ""
        val latitude = args.getDouble(ARG_LATITUDE)
        val longitude = args.getDouble(ARG_LONGITUDE)
        val createdAt = args.getLong(ARG_CREATED_AT)

        Glide.with(this)
            .load(Uri.parse(photoUri))
            .placeholder(R.drawable.ic_placeholder_stock)
            .error(R.drawable.ic_placeholder_stock)
            .centerCrop()
            .into(binding.imageDetailPhoto)

        binding.textDetailDate.text = dateFormat.format(Date(createdAt))
        binding.textDetailLocation.text = getString(R.string.location_format, latitude, longitude)

        val lines = description.split("\n").filter { it.isNotBlank() && it != "Market snapshot" }
        if (lines.isNotEmpty()) {
            binding.dividerStocks.visibility = View.VISIBLE
            binding.labelStocksSection.visibility = View.VISIBLE
            binding.layoutStocksCard.visibility = View.VISIBLE
            renderStockRows(lines)
        }
    }

    private fun renderStockRows(lines: List<String>) {
        val density = resources.displayMetrics.density
        lines.forEachIndexed { index, line ->
            val parts = line.split(": ", limit = 2)
            val symbol = parts.getOrElse(0) { line }
            val price = parts.getOrElse(1) { "" }

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (index > 0) topMargin = (10 * density).toInt()
                }
            }

            val symbolView = TextView(requireContext()).apply {
                text = symbol
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val hasPrice = price.startsWith("$")
            val priceView = TextView(requireContext()).apply {
                text = price.ifBlank { getString(R.string.label_stat_none) }
                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (hasPrice) R.color.change_positive else R.color.text_muted
                    )
                )
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
            }

            row.addView(symbolView)
            row.addView(priceView)
            binding.layoutStocksCard.addView(row)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SnapshotDetailBottomSheet"
        private const val ARG_PHOTO_URI = "photo_uri"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_LATITUDE = "latitude"
        private const val ARG_LONGITUDE = "longitude"
        private const val ARG_CREATED_AT = "created_at"

        fun newInstance(
            photoUri: String,
            description: String,
            latitude: Double,
            longitude: Double,
            createdAt: Long
        ): SnapshotDetailBottomSheet = SnapshotDetailBottomSheet().apply {
            arguments = Bundle().apply {
                putString(ARG_PHOTO_URI, photoUri)
                putString(ARG_DESCRIPTION, description)
                putDouble(ARG_LATITUDE, latitude)
                putDouble(ARG_LONGITUDE, longitude)
                putLong(ARG_CREATED_AT, createdAt)
            }
        }
    }
}
