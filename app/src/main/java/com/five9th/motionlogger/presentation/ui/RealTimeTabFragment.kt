package com.five9th.motionlogger.presentation.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.five9th.motionlogger.R
import com.five9th.motionlogger.databinding.FragmentRealTimeTabBinding
import com.five9th.motionlogger.presentation.vm.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RealTimeTabFragment : Fragment() {

    private var _binding: FragmentRealTimeTabBinding? = null
    private val binding
        get() = _binding!!

    private val mainViewModel: MainViewModel by lazy {  // same VM as in activity
        ViewModelProvider(requireActivity())[MainViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRealTimeTabBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        collectFlows()
    }

    private fun collectFlows() {
        lifecycleScope.launch {
            mainViewModel.currentActivityScoresSF.collect{
                try {
                    bindScores(it)
                }
                catch (e: RuntimeException) {
                    e.printStackTrace()
                }
            }
        }
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
        // ["dws", "ups", "wlk", "jog", "std", "sit"]
        for (i in scores.indices) {
            setPercent(tvList[i], scores[i])
        }

        // make the greatest value green
        (scores.indices.maxByOrNull { scores[it] })?.let {
            tvList[it].setTextColor(resources.getColor(R.color.green))
        }
    }

    private fun setPercent(tv: TextView, value: Float) {
        tv.text = getString(R.string.percent_placeholder, value * 100)
        tv.setTextColor(resources.getColor(R.color.black))
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}