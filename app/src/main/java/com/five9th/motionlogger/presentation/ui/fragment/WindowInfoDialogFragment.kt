package com.five9th.motionlogger.presentation.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.five9th.motionlogger.databinding.ClassesPercentageBinding

class WindowInfoDialogFragment : DialogFragment()
{
    private var _binding: ClassesPercentageBinding? = null
    private val binding
        get() = _binding!!

    private var scores: FloatArray? = null
    private var windowIdx: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // retrieve Args
        arguments?.let {
            scores = it.getFloatArray(ARG_SCORES)
            windowIdx = it.getInt(ARG_WINDOW)
        }
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
        scores?.let {
            val helper = ActivityPercentageDisplayer(binding, windowIdx = windowIdx!!)
            helper.displayOnce(it.toList())
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_SCORES = "scores"
        private const val ARG_WINDOW = "window"

        fun newInstance(values: List<Float>, windowIdx: Int): WindowInfoDialogFragment {
            return WindowInfoDialogFragment().apply {
                arguments = Bundle().apply {
                    putFloatArray(ARG_SCORES, values.toFloatArray())
                    putInt(ARG_WINDOW, windowIdx)
                }
            }
        }
    }
}