package com.student.mobile_dev_laboratory_work_3.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.student.mobile_dev_laboratory_work_3.data.model.MessageDto
import com.student.mobile_dev_laboratory_work_3.databinding.ItemMessageImageBinding
import com.student.mobile_dev_laboratory_work_3.databinding.ItemMessageTextBinding
import com.student.mobile_dev_laboratory_work_3.util.ImageUrlBuilder

/** Адаптер сообщений: текст и превью картинок */
class MessageAdapter(
    private val currentUser: String,
    private val onImageClick: (String) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var messages: List<MessageDto> = emptyList()

    fun submitList(list: List<MessageDto>) {
        messages = list
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (messages[position].data.image != null) VIEW_TYPE_IMAGE else VIEW_TYPE_TEXT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_IMAGE -> {
                val binding = ItemMessageImageBinding.inflate(inflater, parent, false)
                ImageViewHolder(binding)
            }
            else -> {
                val binding = ItemMessageTextBinding.inflate(inflater, parent, false)
                TextViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is TextViewHolder -> holder.bind(message)
            is ImageViewHolder -> holder.bind(message, onImageClick)
        }
    }

    override fun getItemCount(): Int = messages.size

    inner class TextViewHolder(
        private val binding: ItemMessageTextBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: MessageDto) {
            val isOwn = message.from == currentUser
            binding.textAuthor.text = message.from
            binding.textBody.text = message.data.text?.text.orEmpty()
            binding.layoutBubble.setBackgroundResource(
                if (isOwn) com.student.mobile_dev_laboratory_work_3.R.drawable.bubble_own
                else com.student.mobile_dev_laboratory_work_3.R.drawable.bubble_other,
            )
        }
    }

    inner class ImageViewHolder(
        private val binding: ItemMessageImageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: MessageDto, onClick: (String) -> Unit) {
            val link = message.data.image?.link.orEmpty()
            binding.textAuthor.text = message.from
            binding.imagePreview.load(ImageUrlBuilder.thumbUrl(link)) {
                crossfade(true)
            }
            binding.imagePreview.setOnClickListener { onClick(link) }
            binding.textImageHint.visibility = View.VISIBLE
        }
    }

    companion object {
        private const val VIEW_TYPE_TEXT = 0
        private const val VIEW_TYPE_IMAGE = 1
    }
}
