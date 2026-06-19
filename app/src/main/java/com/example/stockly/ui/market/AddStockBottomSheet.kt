package com.example.stockly.ui.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.stockly.R
import com.example.stockly.databinding.DialogAddStockBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddStockBottomSheet(
    private val onAddStock: (symbol: String, name: String, logoUrl: String) -> Unit
) : BottomSheetDialogFragment() {

    override fun getTheme() = R.style.AppBottomSheetDialogTheme

    private var _binding: DialogAddStockBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddStockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnAdd.setOnClickListener {
            val symbol = binding.editTextSymbol.text?.toString()?.trim() ?: ""
            val name = binding.editTextName.text?.toString()?.trim() ?: ""
            val logoUrl = binding.editTextLogoUrl.text?.toString()?.trim() ?: ""
            if (symbol.isNotBlank()) {
                binding.inputLayoutSymbol.error = null
                onAddStock(symbol, name, logoUrl)
                dismiss()
            } else {
                binding.inputLayoutSymbol.error = getString(R.string.error_symbol_required)
            }
        }
        binding.btnCancel.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddStockBottomSheet"
    }
}
