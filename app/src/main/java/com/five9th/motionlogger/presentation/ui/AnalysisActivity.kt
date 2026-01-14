package com.five9th.motionlogger.presentation.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.five9th.motionlogger.R
import com.five9th.motionlogger.databinding.ActivityAnalysisBinding
import com.five9th.motionlogger.presentation.uimodel.SessionItem
import com.five9th.motionlogger.presentation.vm.AnalysisViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnalysisActivity : AppCompatActivity() {

    private val tag = "AnalysisActivity"

    private val viewModel: AnalysisViewModel by viewModels()

    private lateinit var binding: ActivityAnalysisBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        collectFlows()
    }

    private fun collectFlows() {
        lifecycleScope.launch {
            viewModel.sessionInfoSF.collect(::onSessionInfoChanged)
        }

        lifecycleScope.launch {
            viewModel.sampleCountSF.collect(::onSampleCountChanged)
        }

        lifecycleScope.launch {
            viewModel.messageSF.collect(::onMessageChanged)
        }

        lifecycleScope.launch {
            viewModel.analysisResultSF.collect(::onAnalysisResultChanged)
        }
    }

    private fun onSessionInfoChanged(item: SessionItem) {
        binding.tvSessionNumber.text = item.number
        binding.tvSessionKeyword.text = item.keyWord
        binding.tvStartTime.text = item.startTime
        binding.tvStopTime.text = item.stopTime
        binding.tvSessionDuration.text = item.duration
    }

    private fun onSampleCountChanged(count: String) {
        binding.tvSampleCount.text = count
    }

    private fun onMessageChanged(message: String) {
        binding.tvOutputMessage.text = message
    }

    private fun onAnalysisResultChanged(analysisStr: String) {
        if (analysisStr.isEmpty()) return

        binding.llAnalysis.visibility = View.VISIBLE
        binding.tvOutputAnalysis.text = analysisStr
    }


    companion object {

        fun newIntent(context: Context, sessionId: Int): Intent {
            return Intent(context, AnalysisActivity::class.java).also {
                it.putExtra(AnalysisViewModel.EXTRA_ID, sessionId)
            }
        }
    }
}