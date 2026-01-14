package com.five9th.motionlogger.presentation.adapters

import androidx.recyclerview.widget.DiffUtil
import com.five9th.motionlogger.domain.entities.SessionInfo

class SessionInfoDiffCallback : DiffUtil.ItemCallback<SessionInfo>() {
    override fun areItemsTheSame(oldItem: SessionInfo, newItem: SessionInfo): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SessionInfo, newItem: SessionInfo): Boolean {
        return oldItem == newItem
    }
}