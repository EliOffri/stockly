package com.example.stockly.ui.snapshot

import android.Manifest
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stockly.R
import com.example.stockly.data.local.entity.SnapshotEntity
import com.example.stockly.data.local.entity.WatchlistEntity
import com.example.stockly.databinding.FragmentMarketSnapshotBinding
import com.example.stockly.util.PermissionUtils
import com.example.stockly.util.Resource
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class MarketSnapshotFragment : Fragment() {

    private var _binding: FragmentMarketSnapshotBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MarketSnapshotViewModel by viewModels()
    private lateinit var adapter: SnapshotAdapter
    private var pendingPhotoUri: Uri? = null

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            showListPanel()
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else showPermissionDeniedSnackbar(getString(R.string.camera_permission_denied))
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fetchLocation()
        else showPermissionDeniedSnackbar(getString(R.string.location_permission_denied))
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) pendingPhotoUri?.let { uri -> viewModel.setPhotoUri(uri) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarketSnapshotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)
        setupRecyclerView()
        observeViewModel()
        setupSubmitButtonState()

        binding.fabAddSnapshot.setOnClickListener {
            viewModel.clearFormState()
            showFormPanel()
        }

        binding.buttonBack.setOnClickListener { showListPanel() }
        binding.buttonTakePhoto.setOnClickListener { onTakePhotoClicked() }
        binding.buttonGetLocation.setOnClickListener { onGetLocationClicked() }
        binding.buttonLocationSet.setOnClickListener { onGetLocationClicked() }
        binding.buttonSubmitReport.setOnClickListener { viewModel.submitSnapshot() }
    }

    private fun setupRecyclerView() {
        adapter = SnapshotAdapter { entity ->
            SnapshotDetailBottomSheet.newInstance(
                entity.photoUri, entity.description,
                entity.latitude, entity.longitude, entity.createdAt
            ).show(childFragmentManager, SnapshotDetailBottomSheet.TAG)
        }
        binding.recyclerViewSnapshots.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewSnapshots.adapter = adapter
        attachSwipeHelper()
    }

    private fun attachSwipeHelper() {
        val deleteColor = ContextCompat.getColor(requireContext(), R.color.urgency_red)
        val trashIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_trash)

        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private val bgPaint = android.graphics.Paint().apply { isAntiAlias = true }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.layoutPosition
                if (position == RecyclerView.NO_POSITION) return
                showDeleteDialog(adapter.currentList[position])
            }

            override fun onChildDraw(
                c: android.graphics.Canvas,
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
                    val bg = android.graphics.RectF(
                        itemView.right + dX, itemView.top.toFloat(),
                        itemView.right.toFloat(), itemView.bottom.toFloat()
                    )
                    c.drawRoundRect(bg, cornerRadius, cornerRadius, bgPaint)
                    trashIcon?.let { icon ->
                        icon.colorFilter = android.graphics.PorterDuffColorFilter(
                            0xFFFFFFFF.toInt(), android.graphics.PorterDuff.Mode.SRC_IN
                        )
                        val iconSize = 24 * resources.displayMetrics.density.toInt()
                        val iconMargin = 24 * resources.displayMetrics.density.toInt()
                        val iconTop = itemView.top + (itemView.height - iconSize) / 2
                        icon.setBounds(
                            itemView.right - iconMargin - iconSize, iconTop,
                            itemView.right - iconMargin, iconTop + iconSize
                        )
                        icon.draw(c)
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(callback).attachToRecyclerView(binding.recyclerViewSnapshots)
    }

    private fun setupSubmitButtonState() {
        binding.buttonSubmitReport.isEnabled = false
        viewModel.photoUri.observe(viewLifecycleOwner) { updateSubmitButton() }
        viewModel.location.observe(viewLifecycleOwner) { updateSubmitButton() }
    }

    private fun updateSubmitButton() {
        binding.buttonSubmitReport.isEnabled =
            viewModel.photoUri.value != null && viewModel.location.value != null
    }

    private fun observeViewModel() {
        viewModel.snapshots.observe(viewLifecycleOwner) { snapshots ->
            adapter.submitList(snapshots)
            val isEmpty = snapshots.isEmpty()
            binding.layoutEmptySnapshots.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.textSnapshotsCount.text = if (isEmpty) "" else getString(R.string.label_snapshots_count, snapshots.size)
        }

        viewModel.watchlistLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressWatchlistPrices.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.watchlistWithPrices.observe(viewLifecycleOwner) { items ->
            items ?: return@observe
            if (items.isEmpty()) {
                binding.layoutWatchlistStocks.visibility = View.GONE
                binding.textEmptyWatchlist.visibility = View.VISIBLE
            } else {
                binding.textEmptyWatchlist.visibility = View.GONE
                binding.layoutWatchlistStocks.visibility = View.VISIBLE
                renderWatchlistRows(items)
            }
        }

        viewModel.photoUri.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                binding.photoZonePlaceholder.visibility = View.INVISIBLE
                binding.imagePhotoPreview.visibility = View.VISIBLE
                Glide.with(this).load(uri).centerCrop().into(binding.imagePhotoPreview)
                setDotActive(binding.viewStatusDotPhoto)
                binding.textPhotoStatus.text = getString(R.string.photo_attached)
            } else {
                binding.photoZonePlaceholder.visibility = View.VISIBLE
                binding.imagePhotoPreview.visibility = View.INVISIBLE
                setDotInactive(binding.viewStatusDotPhoto)
                binding.textPhotoStatus.text = getString(R.string.photo_not_attached)
            }
        }

        viewModel.location.observe(viewLifecycleOwner) { location ->
            if (location != null) {
                binding.buttonLocationSet.setBackgroundResource(R.drawable.bg_location_button_active)
                setDotActive(binding.viewStatusDotLocation)
                binding.textLocationStatus.text = getString(R.string.location_format, location.first, location.second)
            } else {
                binding.buttonLocationSet.setBackgroundResource(R.drawable.bg_location_button)
                setDotInactive(binding.viewStatusDotLocation)
                binding.textLocationStatus.text = getString(R.string.location_not_attached)
            }
        }

        viewModel.submissionState.observe(viewLifecycleOwner) { resource ->
            resource ?: return@observe
            when (resource) {
                is Resource.Loading -> {
                    binding.buttonSubmitReport.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.INVISIBLE
                    showListPanel()
                    Snackbar.make(binding.root, getString(R.string.snapshot_success), Snackbar.LENGTH_SHORT)
                        .setAnchorView(requireActivity().findViewById(R.id.bottom_nav))
                        .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.change_positive))
                        .show()
                    viewModel.clearFormState()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.INVISIBLE
                    updateSubmitButton()
                    val message = when (resource.message) {
                        "photo" -> getString(R.string.error_photo_required)
                        "location" -> getString(R.string.error_location_required)
                        else -> getString(R.string.error_loading_data)
                    }
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                        .setAnchorView(requireActivity().findViewById(R.id.bottom_nav))
                        .show()
                    viewModel.resetSubmissionState()
                }
            }
        }
    }

    private fun renderWatchlistRows(items: List<Pair<WatchlistEntity, Double?>>) {
        binding.layoutWatchlistStocks.removeAllViews()
        val density = resources.displayMetrics.density
        items.forEachIndexed { index, (entity, price) ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (index > 0) topMargin = (8 * density).toInt()
                }
            }
            val symbolView = TextView(requireContext()).apply {
                text = entity.symbol
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val priceView = TextView(requireContext()).apply {
                val hasPrice = price != null && price > 0.0
                text = if (hasPrice) getString(R.string.price_format, price) else getString(R.string.label_stat_none)
                setTextColor(ContextCompat.getColor(requireContext(),
                    if (hasPrice) R.color.change_positive else R.color.text_muted))
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
            }
            row.addView(symbolView)
            row.addView(priceView)
            binding.layoutWatchlistStocks.addView(row)
        }
    }

    private fun setDotActive(dot: View) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ContextCompat.getColor(requireContext(), R.color.user_added_badge))
        }
        dot.background = drawable
    }

    private fun setDotInactive(dot: View) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ContextCompat.getColor(requireContext(), R.color.border))
        }
        dot.background = drawable
    }

    private fun onTakePhotoClicked() {
        when {
            PermissionUtils.hasCameraPermission(requireContext()) -> launchCamera()
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ->
                showRationaleDialog(getString(R.string.camera_rationale)) {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun onGetLocationClicked() {
        when {
            PermissionUtils.hasLocationPermission(requireContext()) -> fetchLocation()
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ->
                showRationaleDialog(getString(R.string.location_rationale)) {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            else -> locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun launchCamera() {
        val photoFile = createImageFile()
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        pendingPhotoUri = uri
        takePictureLauncher.launch(uri)
    }

    private fun fetchLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        lifecycleScope.launch {
            try {
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    viewModel.setLocation(location.latitude, location.longitude)
                } else {
                    Snackbar.make(binding.root, getString(R.string.location_not_attached), Snackbar.LENGTH_LONG)
                        .setAnchorView(requireActivity().findViewById(R.id.bottom_nav))
                        .show()
                }
            } catch (e: SecurityException) {
                showPermissionDeniedSnackbar(getString(R.string.location_permission_denied))
            }
        }
    }

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timestamp}_", ".jpg", storageDir)
    }

    private fun showRationaleDialog(message: String, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(message)
            .setPositiveButton(getString(R.string.confirm)) { _, _ -> onConfirm() }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showPermissionDeniedSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAnchorView(requireActivity().findViewById(R.id.bottom_nav))
            .setAction(getString(R.string.open_settings)) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }
                startActivity(intent)
            }
            .show()
    }

    private fun showDeleteDialog(entity: SnapshotEntity) {
        MaterialAlertDialogBuilder(requireContext(), R.style.Stockly_AlertDialog_Destructive)
            .setTitle(getString(R.string.delete_snapshot_title))
            .setMessage(getString(R.string.delete_snapshot_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteSnapshot(entity)
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                adapter.notifyDataSetChanged()
            }
            .show()
    }

    private fun showListPanel() {
        binding.layoutList.visibility = View.VISIBLE
        binding.layoutAddForm.visibility = View.GONE
        binding.fabAddSnapshot.visibility = View.VISIBLE
        backCallback.isEnabled = false
    }

    private fun showFormPanel() {
        binding.layoutList.visibility = View.GONE
        binding.layoutAddForm.visibility = View.VISIBLE
        binding.fabAddSnapshot.visibility = View.GONE
        backCallback.isEnabled = true
        viewModel.loadWatchlistPrices()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
