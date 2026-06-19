package com.example.stockly.ui.market

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.stockly.R
import com.example.stockly.data.remote.model.Stock
import com.example.stockly.databinding.FragmentMarketBinding
import com.example.stockly.util.Resource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MarketFragment : Fragment() {

    private var _binding: FragmentMarketBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MarketViewModel by viewModels()
    private lateinit var adapter: StockAdapter

    private var fullList: List<Stock> = emptyList()
    private var currentQuery = ""
    private var currentFeaturedStock: Stock? = null
    private var pinnedFeaturedStock: Stock? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        observeViewModel()
        binding.fabAddStock.setOnClickListener { showAddStockBottomSheet() }
        binding.cardFeatured.setOnClickListener {
            currentFeaturedStock?.let { navigateToDetail(it) }
        }
        binding.btnFeaturedFavorite.setOnClickListener {
            currentFeaturedStock?.let { viewModel.toggleWatchlist(it) }
        }
        viewModel.isFeaturedInWatchlist.observe(viewLifecycleOwner) { isWatched ->
            binding.btnFeaturedFavorite.setImageResource(
                if (isWatched) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            )
        }
        viewModel.addStockState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Error -> Snackbar.make(
                    binding.root,
                    getString(R.string.error_loading_data),
                    Snackbar.LENGTH_SHORT
                ).setAnchorView(requireActivity().findViewById(R.id.bottom_nav)).show()
                else -> {}
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = StockAdapter(
            onStockClicked = { stock -> navigateToDetail(stock) },
            onStockLongClicked = { stock -> showDeleteStockDialog(stock) }
        )
        binding.recyclerViewStocks.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewStocks.adapter = adapter
    }

    private fun setupSearch() {
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentQuery = s?.toString()?.trim() ?: ""
                applyFilters()
            }
        })
    }

    private fun applyFilters() {
        val filtered = fullList.filter { matchesQuery(it, currentQuery) }
        adapter.submitList(filtered)
        if (filtered.isNotEmpty()) {
            val pinnedStillPresent = pinnedFeaturedStock != null &&
                filtered.any { it.symbol == pinnedFeaturedStock!!.symbol }
            if (!pinnedStillPresent) {
                pinnedFeaturedStock = filtered.random()
                updateFeaturedCard(pinnedFeaturedStock!!)
            }
        }
    }

    private fun matchesQuery(stock: Stock, query: String): Boolean {
        if (query.isEmpty()) return true
        return stock.symbol.contains(query, ignoreCase = true) ||
            stock.name.contains(query, ignoreCase = true)
    }

    private fun observeViewModel() {
        viewModel.combinedList.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.recyclerViewStocks.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.recyclerViewStocks.visibility = View.VISIBLE
                    fullList = resource.data ?: emptyList()
                    applyFilters()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.recyclerViewStocks.visibility = View.VISIBLE
                    Snackbar.make(binding.root, getString(R.string.error_loading_data), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.retry)) { viewModel.loadStocks() }
                        .setAnchorView(requireActivity().findViewById(R.id.bottom_nav))
                        .show()
                }
            }
        }
    }

    private fun updateFeaturedCard(stock: Stock) {
        currentFeaturedStock = stock
        viewModel.setFeaturedStock(stock.symbol)
        binding.layoutFeaturedHeader.visibility = View.VISIBLE
        binding.cardFeatured.visibility = View.VISIBLE
        binding.textFeaturedName.text = stock.name.ifBlank { stock.symbol }
        binding.textFeaturedMeta.text = if (stock.industry.isNotBlank()) {
            stock.industry
        } else {
            stock.symbol
        }
        Glide.with(this)
            .load(stock.logoUrl.ifBlank { null })
            .placeholder(R.drawable.ic_placeholder_stock)
            .error(R.drawable.ic_placeholder_stock)
            .centerInside()
            .into(binding.imageFeatured)
    }

    private fun showAddStockBottomSheet() {
        AddStockBottomSheet { symbol, name, logoUrl ->
            viewModel.addUserStock(symbol, name, logoUrl)
        }.show(childFragmentManager, AddStockBottomSheet.TAG)
    }

    private fun showDeleteStockDialog(stock: Stock) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_stock_title))
            .setMessage(getString(R.string.delete_stock_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteUserStock(stock)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun navigateToDetail(stock: Stock) {
        val bundle = Bundle().apply {
            putString("symbol", stock.symbol)
            putString("logoUrl", stock.logoUrl)
            putString("name", stock.name)
        }
        findNavController().navigate(R.id.action_homeFragment_to_stockDetailFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
