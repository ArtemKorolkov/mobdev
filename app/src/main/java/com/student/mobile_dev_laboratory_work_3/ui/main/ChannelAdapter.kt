package com.student.mobile_dev_laboratory_work_3.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.student.mobile_dev_laboratory_work_3.databinding.ItemChannelBinding

/** Адаптер списка каналов; выделяет выбранный канал в ландшафте */
class ChannelAdapter(
    private var selectedChannel: String?,
    private val onChannelClick: (String) -> Unit,
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    private var channels: List<String> = emptyList()

    fun submitList(list: List<String>, selected: String?) {
        channels = list
        selectedChannel = selected
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = ItemChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(channels[position], channels[position] == selectedChannel)
    }

    override fun getItemCount(): Int = channels.size

    inner class ChannelViewHolder(
        private val binding: ItemChannelBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(channel: String, isSelected: Boolean) {
            binding.textChannelName.text = channel
            binding.root.isSelected = isSelected
            binding.root.setOnClickListener { onChannelClick(channel) }
        }
    }
}
