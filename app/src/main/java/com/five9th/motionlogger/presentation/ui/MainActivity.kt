package com.five9th.motionlogger.presentation.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.five9th.motionlogger.R
import com.five9th.motionlogger.databinding.ActivityMainBinding
import com.five9th.motionlogger.domain.entities.SensorsInfo
import com.five9th.motionlogger.domain.utils.TimeFormatHelper
import com.five9th.motionlogger.presentation.uimodel.CollectionStats
import com.five9th.motionlogger.presentation.vm.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val tag = "MainActivity"

    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding

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

        initViewModel()
        setListeners()
        collectFlows()
    }

    private fun initViewModel() {
        mainViewModel.sensorsInfoLD.observe(this, ::onSensorsInfoChanged)
        mainViewModel.getSensorsInfo()
    }

    private fun setListeners() { // TODO: check for notification permission if Android 13+
        binding.btnStart.setOnClickListener {
            mainViewModel.startCollect()
        }

        binding.btnStop.setOnClickListener {
            mainViewModel.stopCollect()
        }
    }

    private fun collectFlows() {
        lifecycleScope.launch {
            mainViewModel.isCollectingSF.collect(::onCollectingStateChanged)
        }

        lifecycleScope.launch {
            mainViewModel.collectionStatsSF.collect(::onCollectionStatsChanged)
        }
    }


    private fun onSensorsInfoChanged(sensorsInfo: SensorsInfo) {
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

    private fun onCollectingStateChanged(isCollecting: Boolean) {
        binding.tvStatus.text =
            if (isCollecting) getString(R.string.collecting)
            else getString(R.string.stopped)
    }

    private fun onCollectionStatsChanged(stats: CollectionStats) {
        binding.tvTimer.text = TimeFormatHelper.elapsedMillisToMmSs(stats.elapsedMillis)
        binding.tvSamplesCount.text = String.format(
            Locale.getDefault(), "%d", stats.samplesCount)
    }
}