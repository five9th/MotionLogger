package com.five9th.motionlogger.presentation.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.five9th.motionlogger.databinding.FragmentSessionListTabBinding
import com.five9th.motionlogger.domain.entities.SessionInfo
import com.five9th.motionlogger.presentation.adapters.SessionInfoAdapter
import com.five9th.motionlogger.presentation.uimodel.UiMapper
import com.five9th.motionlogger.presentation.vm.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SessionListTabFragment : Fragment() {

    private var _binding: FragmentSessionListTabBinding? = null
    private val binding
        get() = _binding!!

    private val mainViewModel: MainViewModel by lazy {  // same VM as in activity
        ViewModelProvider(requireActivity())[MainViewModel::class.java]
    }

    private lateinit var adapter: SessionInfoAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionListTabBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        initAdapter()
        collectFlows()
    }

    private fun initAdapter() {
        adapter = SessionInfoAdapter(UiMapper(requireContext()))
        adapter.onClickListener = ::onItemClick

        binding.rvSessionList.adapter = adapter
    }

    private fun collectFlows() {
        lifecycleScope.launch {
            mainViewModel.sessionListSF.collect(::onSessionListChanged)
        }
    }

    private fun onSessionListChanged(list: List<SessionInfo>) {
        adapter.submitList(list)
    }

    private fun onItemClick(item: SessionInfo) {
        val intent = AnalysisActivity.newIntent(requireContext(), item.id)
        startActivity(intent)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}