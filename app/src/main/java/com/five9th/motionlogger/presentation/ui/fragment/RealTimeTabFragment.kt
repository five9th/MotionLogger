package com.five9th.motionlogger.presentation.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.five9th.motionlogger.databinding.ClassesPercentageBinding
import com.five9th.motionlogger.presentation.vm.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RealTimeTabFragment : Fragment() {

    private val mainViewModel: MainViewModel by lazy {  // same VM as in activity
        ViewModelProvider(requireActivity())[MainViewModel::class.java]
    }

    private var _binding: ClassesPercentageBinding? = null
    private val binding
        get() = _binding!!

    private var _helper: ActivityPercentageDisplayer? = null
    private val helper: ActivityPercentageDisplayer
        get() = _helper
            ?: ActivityPercentageDisplayer(
                binding, mainViewModel.currentActivityScoresSF, lifecycleScope
            ).also {
                _helper = it
            }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ClassesPercentageBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        helper.start()
    }

    override fun onDestroyView() {
        helper.cancel()
        _helper = null
        _binding = null

        super.onDestroyView()
    }
}