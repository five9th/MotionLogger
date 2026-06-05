package com.five9th.motionlogger.presentation.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.five9th.motionlogger.databinding.FragmentRealTimeTabBinding
import com.five9th.motionlogger.presentation.vm.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

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
        //todo: collect mainViewModel.currentActivityScoresSF
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}