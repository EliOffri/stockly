package com.example.stockly.ui.stockdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.stockly.R
import com.example.stockly.databinding.FragmentStockDetailBinding
import com.example.stockly.util.Resource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StockDetailFragment : Fragment() {

    private var _binding: FragmentStockDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StockDetailViewModel by viewModels()
    private lateinit var newsAdapter: NewsAdapter
    private var prevWatchlistState: Boolean? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStockDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val symbol = arguments?.getString("symbol") ?: ""
        val logoUrl = arguments?.getString("logoUrl") ?: ""
        val name = arguments?.getString("name") ?: ""

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, statusBarHeight, 0, 0)
            insets
        }

        setupRecyclerView()
        setupInitialInfo(symbol, logoUrl, name)
        observeViewModel()

        viewModel.loadStockDetails(symbol, logoUrl, name)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.fabFavorite.setOnClickListener {
            val isWatched = viewModel.isInWatchlist.value ?: false
            if (isWatched) {
                viewModel.removeFromWatchlist()
            } else {
                showAddToWatchlistDialog()
            }
        }
        binding.buttonTrade.setOnClickListener {
            val bundle = Bundle().apply { putString("symbol", symbol) }
            findNavController().navigate(R.id.action_stockDetailFragment_to_tradeFragment, bundle)
        }
    }

    private fun setupRecyclerView() {
        newsAdapter = NewsAdapter()
        binding.recyclerViewImages.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewImages.adapter = newsAdapter
    }

    private fun setupInitialInfo(symbol: String, logoUrl: String, name: String) {
        binding.textStockTitle.text = name.ifBlank { symbol }
        binding.textStockSymbol.text = symbol
        binding.textStatPriceValue.text = getString(R.string.label_stat_none)
        Glide.with(this)
            .load(logoUrl.ifBlank { null })
            .placeholder(R.drawable.ic_placeholder_stock)
            .error(R.drawable.ic_placeholder_stock)
            .centerInside()
            .into(binding.imageHero)
    }

    private fun observeViewModel() {
        viewModel.detailState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val (profile, quote) = resource.data!!
                    binding.textStockTitle.text = profile.name
                    binding.textStockSymbol.text = if (profile.exchange.isNotBlank()) {
                        "${arguments?.getString("symbol")} • ${profile.exchange}"
                    } else {
                        arguments?.getString("symbol") ?: ""
                    }
                    val priceText = if (quote.currentPrice > 0.0) {
                        getString(R.string.price_format, quote.currentPrice)
                    } else {
                        getString(R.string.label_stat_none)
                    }
                    binding.textStatPriceValue.text = priceText
                    if (quote.changePercent != 0.0) {
                        val changeText = if (quote.changePercent > 0.0) {
                            "+${getString(R.string.change_format, quote.changePercent)}"
                        } else {
                            getString(R.string.change_format, quote.changePercent)
                        }
                        val changeColor = if (quote.changePercent > 0.0) {
                            ContextCompat.getColor(requireContext(), R.color.change_positive)
                        } else {
                            ContextCompat.getColor(requireContext(), R.color.change_negative)
                        }
                        binding.textStatChangeValue.text = changeText
                        binding.textStatChangeValue.setTextColor(changeColor)
                    } else {
                        binding.textStatChangeValue.text = getString(R.string.label_stat_none)
                        binding.textStatChangeValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    }
                    binding.textAboutDescription.text = getString(
                        R.string.stock_detail_description,
                        profile.finnhubIndustry.ifBlank { getString(R.string.label_stat_none) },
                        if (profile.weburl.isNotBlank()) profile.weburl else getString(R.string.label_stat_none)
                    )
                    Glide.with(this)
                        .load(profile.logo.ifBlank { null })
                        .placeholder(R.drawable.ic_placeholder_stock)
                        .error(R.drawable.ic_placeholder_stock)
                        .centerInside()
                        .into(binding.imageHero)
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, getString(R.string.error_loading_data), Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.buttonTrade)
                        .show()
                }
            }
        }

        viewModel.newsState.observe(viewLifecycleOwner) { resource ->
            if (resource is Resource.Success) {
                newsAdapter.submitList(resource.data ?: emptyList())
            }
        }

        viewModel.isInWatchlist.observe(viewLifecycleOwner) { isWatched ->
            val icon = if (isWatched) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            binding.fabFavorite.setImageResource(icon)
            val desc = if (isWatched) {
                getString(R.string.remove_from_favorites)
            } else {
                getString(R.string.add_to_favorites)
            }
            binding.fabFavorite.contentDescription = desc
            if (prevWatchlistState == false && isWatched) {
                Snackbar.make(binding.root, getString(R.string.watchlist_added), Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.change_positive))
                    .setAnchorView(binding.buttonTrade)
                    .show()
            }
            prevWatchlistState = isWatched
        }
    }

    private fun showAddToWatchlistDialog() {
        val inputLayout = TextInputLayout(
            requireContext(), null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            hint = getString(R.string.notes_hint)
            setPadding(48, 8, 48, 0)
        }
        val editText = TextInputEditText(requireContext()).apply {
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        inputLayout.addView(editText)

        MaterialAlertDialogBuilder(requireContext(), R.style.Stockly_AlertDialog)
            .setTitle(getString(R.string.add_to_favorites))
            .setView(inputLayout)
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                val notes = editText.text?.toString()?.trim() ?: ""
                viewModel.addToWatchlist(notes)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
