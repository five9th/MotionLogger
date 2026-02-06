package com.five9th.motionlogger.presentation.ui

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.five9th.motionlogger.R
import com.five9th.motionlogger.databinding.ActivityMainBinding
import com.five9th.motionlogger.databinding.DialogSensorInfoBinding
import com.five9th.motionlogger.domain.entities.SensorsInfo
import com.five9th.motionlogger.domain.entities.SessionInfo
import com.five9th.motionlogger.domain.utils.TimeFormatHelper
import com.five9th.motionlogger.presentation.adapters.SessionInfoAdapter
import com.five9th.motionlogger.presentation.uimodel.CollectionStats
import com.five9th.motionlogger.presentation.uimodel.UiMapper
import com.five9th.motionlogger.presentation.vm.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val tag = "MainActivity"

    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding

    private lateinit var adapter: SessionInfoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initAdapter()
        initViewModel()
        setListeners()
        collectFlows()
    }

    private fun initAdapter() {
        adapter = SessionInfoAdapter(UiMapper(this))
        adapter.onClickListener = ::onItemClick

        binding.rvSessionList.adapter = adapter
    }

    private fun initViewModel() {
        mainViewModel.getSensorsInfo()
        mainViewModel.reloadSavedSessions()
    }

    private fun setListeners() {
        binding.btnStart.setOnClickListener {
            onStartClick()
        }

        binding.btnStop.setOnClickListener {
            onStopClick()
        }

        binding.btnSensorInfo.setOnClickListener {
            onInfoClick()
        }
    }

    private fun collectFlows() {
        lifecycleScope.launch {
            mainViewModel.isCollectingSF.collect(::onCollectingStateChanged)
        }

        lifecycleScope.launch {
            mainViewModel.collectionStatsSF.collect(::onCollectionStatsChanged)
        }

        lifecycleScope.launch {
            mainViewModel.sessionListSF.collect(::onSessionListChanged)
        }
    }


    // ====== Notification stuff ======

    private val notificationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ::onNotificationPermissionResult
    )

    private val helper = NotificationHelper(this, notificationPermissionRequest)

    private fun checkPermissionsAndStartCollect() {
        helper.ensurePermission(
            onGranted = { onNotificationPermissionResult(true) },
            onDenied = { onNotificationPermissionResult(false) }
        )
    }

    private fun onNotificationPermissionResult(granted: Boolean) {
        if (granted) {
            // start collection
            mainViewModel.startCollect()
        }
        else {
            // show explanation or disable feature
            showExplanation()
        }
    }

    private fun showExplanation() {
        helper.showNotificationSettingsDialog()
    }


    // ====== Listeners ======

    // ---- UI State ----
    private fun onCollectingStateChanged(isCollecting: Boolean) {
        binding.tvStatus.text =
            if (isCollecting) getString(R.string.collecting)
            else getString(R.string.stopped)
    }

    private fun onCollectionStatsChanged(stats: CollectionStats) {
        binding.tvTimer.text = TimeFormatHelper.elapsedMillisToMmSs(stats.elapsedMillis)
        binding.tvSampleCount.text = String.format(
            Locale.getDefault(), "%d", stats.samplesCount)
    }

    private fun onSessionListChanged(list: List<SessionInfo>) {
        adapter.submitList(list)
    }

    // ---- Click ----
    private fun onItemClick(item: SessionInfo) {
        Log.d(tag, "Item click: $item")

        val intent = AnalysisActivity.newIntent(this, item.id)
        startActivity(intent)
    }

    private fun onStartClick() {
        checkPermissionsAndStartCollect()
    }

    private fun onStopClick() {
        // TODO: get key word
        mainViewModel.stopCollectAndSave("")
    }

    private fun onInfoClick() {
        val info = mainViewModel.sensorsInfoSF.value

        if (info != null) {
            showSensorsInfoDialog(info)
        }
        else {
            requestAndShowSensorsInfo()
        }
    }

    // ====== Sensors Info Dialog ======

    private fun showSensorsInfoDialog(info: SensorsInfo) {
        val binding = DialogSensorInfoBinding.inflate(layoutInflater)

        bindInfo(binding, info)

        AlertDialog.Builder(this)
            .setView(binding.root)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun bindInfo(binding: DialogSensorInfoBinding, sensorsInfo: SensorsInfo) {
        val strYes = getString(R.string.yes)
        val strNo = getString(R.string.no)

        binding.tvHasAccelerometer.text = if (sensorsInfo.hasAccelerometer) strYes else strNo
        binding.tvHasGyroscope.text = if (sensorsInfo.hasGyroscope) strYes else strNo
        binding.tvHasMagnetometer.text = if (sensorsInfo.hasMagnetometer) strYes else strNo
        binding.tvHasGameRotationVector.text = if (sensorsInfo.hasGameRotationVector) strYes else strNo
        binding.tvHasRotationVector.text = if (sensorsInfo.hasRotationVector) strYes else strNo
        binding.tvHasLinearAcceleration.text = if (sensorsInfo.hasLinearAcceleration) strYes else strNo
        binding.tvHasGravity.text = if (sensorsInfo.hasGravity) strYes else strNo
    }

    private fun requestAndShowSensorsInfo() {
        mainViewModel.getSensorsInfo()
        lifecycleScope.launch {
            val info = withTimeoutOrNull(2_000) {
                mainViewModel.sensorsInfoSF.first { it != null }
            }

            if (info != null) {
                showSensorsInfoDialog(info)
            }
            else {
                showError()
            }
        }
    }

    private fun showError() {
        Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show()
    }
}