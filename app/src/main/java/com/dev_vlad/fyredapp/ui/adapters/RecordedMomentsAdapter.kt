package com.dev_vlad.fyredapp.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dev_vlad.fyredapp.R
import com.dev_vlad.fyredapp.databinding.ListItemMomentBinding
import com.dev_vlad.fyredapp.models.RecordedMoment

class RecordedMomentsAdapter(private val clickListener: UserMomentClickListener) :
    ListAdapter<RecordedMoment, RecordedMomentsAdapter.ViewHolder>(UserMomentsDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //use getItem from listAdapter
        holder.bind(getItem(position), clickListener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }


    //dataset difference calculator
    class UserMomentsDiffCallback :
        DiffUtil.ItemCallback<RecordedMoment>() {
        override fun areItemsTheSame(
            oldItem: RecordedMoment,
            newItem: RecordedMoment
        ): Boolean {
            val areSame = oldItem.isSameAs(newItem)
            Log.d(
                "RecordedMomentsAdapter",
                "areItemsSame $areSame, captions ${oldItem.caption} && ${newItem.caption} "
            )
            return areSame

        }

        override fun areContentsTheSame(
            oldItem: RecordedMoment,
            newItem: RecordedMoment
        ): Boolean {
            val areContentsSame = oldItem.hasSameContentsAs(newItem)
            Log.d(
                "RecordedMomentsAdapter",
                "areContentsTheSame $areContentsSame, captions ${oldItem.caption} && ${newItem.caption}"
            )
            return areContentsSame
        }

    }

    class ViewHolder private constructor(val binding: ListItemMomentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            moment: RecordedMoment,
            clickListener: UserMomentClickListener
        ) {
            //is there is a photo or video thumbnail to display
            val momentThumbnail = if (moment.image) {
                binding.videoIndicatorIv.visibility = View.GONE
                moment.mediaUriString
            } else {
                binding.videoIndicatorIv.visibility = View.VISIBLE
                moment.mediaUriString
            }

            Glide.with(itemView.context)
                .load(momentThumbnail)
                .skipMemoryCache(true)
                .into(binding.thumbnail)

            if (moment.caption.trim().isNotEmpty())
                binding.captionTv.text = moment.caption
            else binding.captionTv.setText(R.string.add_caption_txt)

            itemView.setOnClickListener {
                clickListener.onClick(moment)
            }

        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemMomentBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }

    class UserMomentClickListener(val clickListener: (moment: RecordedMoment) -> Unit) {
        fun onClick(moment: RecordedMoment) = clickListener(moment)
    }

}