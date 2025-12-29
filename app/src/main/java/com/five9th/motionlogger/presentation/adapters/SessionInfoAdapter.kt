package com.five9th.motionlogger.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.five9th.motionlogger.databinding.ItemSessionInfoBinding
import com.five9th.motionlogger.domain.entities.SessionInfo
import com.five9th.motionlogger.presentation.uimodel.SessionItem
import com.five9th.motionlogger.presentation.uimodel.UiMapper

class SessionInfoAdapter(
    private val mapper: UiMapper
) : ListAdapter<SessionInfo, SessionInfoAdapter.SessionViewHolder>(SessionInfoDiffCallback())
{
    class SessionViewHolder(
        private val binding: ItemSessionInfoBinding
    ) : RecyclerView.ViewHolder(binding.root)
    {
        fun bind(item: SessionItem) {
            //todo
            binding.tvSessionId.text = item.number
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSessionInfoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(
            mapper.mapDomainToUiModel(item)
        )
    }
}