package com.five9th.motionlogger.presentation.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.five9th.motionlogger.presentation.ui.RealTimeTabFragment
import com.five9th.motionlogger.presentation.ui.SessionListTabFragment

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SessionListTabFragment()
            1 -> RealTimeTabFragment()
            else -> throw IllegalArgumentException()
        }
    }
}