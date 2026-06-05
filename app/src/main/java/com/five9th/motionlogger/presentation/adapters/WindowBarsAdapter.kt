package com.five9th.motionlogger.presentation.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import com.five9th.motionlogger.R
import com.five9th.motionlogger.databinding.ItemWindowBarBinding
import com.five9th.motionlogger.domain.entities.ActivityClass
import com.five9th.motionlogger.domain.entities.WindowPrediction
import java.util.Locale

class WindowBarsAdapter(
    private val items: List<WindowPrediction>,
    private val onClickListener: ((WindowPrediction) -> Unit)? = null
) : RecyclerView.Adapter<WindowBarsAdapter.WindowBarViewHolder>() {

    class WindowBarViewHolder(
        private val binding: ItemWindowBarBinding
    ) : RecyclerView.ViewHolder(binding.root)
    {
        fun bind(item: WindowPrediction) {
            val color = mapActivityToColor(item.predictedClass, binding.rectView.context)

            binding.rectView.setBackgroundColor(color)
            binding.tvWindowIndex.text = String.format(
                Locale.getDefault(), "%d", item.windowIndex)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WindowBarViewHolder {
        val binding = ItemWindowBarBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return WindowBarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WindowBarViewHolder, position: Int) {
        val item = items[position]

        holder.bind(item)

        holder.itemView.setOnClickListener {
            onClickListener?.invoke(item)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }


    companion object {
        @ColorInt
        fun mapActivityToColor(activity: ActivityClass, context: Context): Int {
            return when (activity) {
                ActivityClass.DOWN_STAIRS -> context.getColor(R.color.dws)
                ActivityClass.UP_STAIRS -> context.getColor(R.color.ups)
                ActivityClass.WALKING -> context.getColor(R.color.wlk)
                ActivityClass.JOGGING -> context.getColor(R.color.jog)
                ActivityClass.STANDING -> context.getColor(R.color.std)
                ActivityClass.SITTING -> context.getColor(R.color.sit)
            }
        }
    }
}