package com.example.stockly.ui.trade

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.stockly.R
import com.example.stockly.databinding.FragmentTradeBinding
import com.example.stockly.util.Resource
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TradeFragment : Fragment() {

    private var _binding: FragmentTradeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TradeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTradeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, statusBarHeight, 0, 0)
            insets
        }

        val symbol = arguments?.getString("symbol") ?: ""
        binding.textSymbolName.text = symbol

        setupOrderTypeDropdown()
        observeViewModel(symbol)

        binding.buttonSubmit.setOnClickListener {
            val shares = binding.editTextName.text?.toString()?.trim() ?: ""
            val orderType = binding.editTextEmail.text?.toString()?.trim() ?: ""
            val limitPrice = binding.editTextPhone.text?.toString()?.trim() ?: ""
            clearErrors()
            viewModel.submitTrade(symbol, shares, orderType, limitPrice)
        }
    }

    private fun setupOrderTypeDropdown() {
        val orderTypes = resources.getStringArray(R.array.order_types)
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, orderTypes)
        binding.editTextEmail.setAdapter(adapter)
        binding.editTextEmail.setOnItemClickListener { _, _, _, _ ->
            val selected = binding.editTextEmail.text?.toString() ?: ""
            binding.inputLayoutPhone.visibility =
                if (selected == "Limit" || selected == "Stop") View.VISIBLE else View.GONE
        }
    }

    private fun clearErrors() {
        binding.inputLayoutName.error = null
        binding.inputLayoutEmail.error = null
        binding.inputLayoutPhone.error = null
    }

    private fun observeViewModel(symbol: String) {
        viewModel.submissionState.observe(viewLifecycleOwner) { resource ->
            resource ?: return@observe
            when (resource) {
                is Resource.Loading -> {
                    binding.buttonSubmit.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.buttonSubmit.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    viewModel.resetState()
                    Snackbar.make(binding.root, getString(R.string.trade_success), Snackbar.LENGTH_LONG)
                        .setBackgroundTint(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.change_positive))
                        .setAnchorView(binding.buttonSubmit)
                        .show()
                    findNavController().popBackStack()
                }
                is Resource.Error -> {
                    binding.buttonSubmit.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    when (resource.message) {
                        "shares" -> binding.inputLayoutName.error = getString(R.string.error_shares_required)
                        "orderType" -> binding.inputLayoutEmail.error = getString(R.string.error_order_type_required)
                        "price" -> binding.inputLayoutPhone.error = getString(R.string.error_invalid_price)
                        else -> Snackbar.make(binding.root, getString(R.string.error_loading_data), Snackbar.LENGTH_LONG)
                            .setAnchorView(binding.buttonSubmit)
                            .show()
                    }
                    viewModel.resetState()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
