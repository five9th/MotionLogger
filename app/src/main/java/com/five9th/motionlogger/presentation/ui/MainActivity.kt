package com.five9th.motionlogger.presentation.ui

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.five9th.motionlogger.R
import com.five9th.motionlogger.databinding.ActivityMainBinding
import com.five9th.motionlogger.domain.entities.SessionInfo
import com.five9th.motionlogger.domain.utils.TimeFormatHelper
import com.five9th.motionlogger.presentation.adapters.SessionInfoAdapter
import com.five9th.motionlogger.presentation.uimodel.CollectionStats
import com.five9th.motionlogger.presentation.uimodel.UiMapper
import com.five9th.motionlogger.presentation.vm.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val tag = "MainActivity"

    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding

    private lateinit var adapter: SessionInfoAdapter

    private lateinit var keywordValidator: KeywordValidator

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

        keywordValidator = KeywordValidator(binding.tilKeyword)

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
        mainViewModel.reloadSavedSessions()
    }

    private fun setListeners() {
        binding.btnStart.setOnClickListener {
            onStartClick()
        }

        binding.btnStop.setOnClickListener {
            onStopClick()
        }

        binding.tvSensorInfo.setOnClickListener {
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
        val sessionKeyWord = keywordValidator.getCurrentWordOrNull() ?: "" // if input is invalid use empty string instead
        mainViewModel.stopCollectAndSave(sessionKeyWord)
    }

    private fun onInfoClick() {
        val helper = SensorsInfoDialogHelper(
            this,
            layoutInflater,
            lifecycleScope
        )

        helper.showSensorsInfo(
            infoFlow = mainViewModel.sensorsInfoSF,
            requestInfoCallback = mainViewModel::getSensorsInfo
        )
    }


    // ====== clear EditText's focus ======

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val focusedView = currentFocus
            if (focusedView is EditText) {
                if (!isClickInsideView(ev, focusedView)) {
                    clearFocusAndHideKeyboard(focusedView)
                }
            }
        }

        return super.dispatchTouchEvent(ev)
    }

    private fun isClickInsideView(event: MotionEvent, view: View): Boolean {
        val outRect = Rect()
        view.getGlobalVisibleRect(outRect)

        return outRect.contains(event.rawX.toInt(), event.rawY.toInt())
    }

    private fun clearFocusAndHideKeyboard(view: View) {
        view.clearFocus()
        hideKeyboard()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
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
}