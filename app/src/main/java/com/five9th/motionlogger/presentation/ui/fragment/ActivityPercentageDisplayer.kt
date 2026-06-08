package com.five9th.motionlogger.presentation.ui.fragment

import android.content.Context
import android.widget.TextView
import com.five9th.motionlogger.R
import com.five9th.motionlogger.databinding.ClassesPercentageBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ActivityPercentageDisplayer(
    private val binding: ClassesPercentageBinding,
    private val windowIdx: Int = 0, // <-- make it flow
    private val percentFlow: Flow<List<Float>>? = null,
    private val messageFlow: Flow<String>? = null,
    private val scope: CoroutineScope? = null,
    private val context: Context = binding.root.context
) {

    fun start() {
        scope!!.launch {
            percentFlow!!.collect{
                bindScores(it)
            }
        }

        scope.launch {
            messageFlow!!.collect {
                binding.tvMessage.text = it
            }
        }
    }

    fun displayOnce(scores: List<Float>) {
        bindScores(scores)
    }

    private val tvList by lazy {
        listOf(
            binding.pctDws,
            binding.pctUps,
            binding.pctWlk,
            binding.pctJog,
            binding.pctStd,
            binding.pctSit
        )
    }

    private fun bindScores(scores: List<Float>) {
        // window index
        binding.tvWindowIdx.text = context.getString(R.string.window_idx, windowIdx)

        // ["dws", "ups", "wlk", "jog", "std", "sit"]
        for (i in scores.indices) {
            setPercent(tvList[i], scores[i])
        }

        // make the greatest value green
        (scores.indices.maxByOrNull { scores[it] })?.let {
            tvList[it].setTextColor(context.getColor(R.color.green))
        }
    }

    private fun setPercent(tv: TextView, value: Float) {
        tv.text = context.getString(R.string.percent_placeholder, value * 100)
        tv.setTextColor(context.getColor(R.color.black))
    }

    /** cancels the scope */
    fun cancel() {
        scope?.cancel()
    }
}