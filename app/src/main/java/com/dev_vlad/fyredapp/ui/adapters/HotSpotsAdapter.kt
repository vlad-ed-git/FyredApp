package com.dev_vlad.fyredapp.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.dev_vlad.fyredapp.R
import com.dev_vlad.fyredapp.databinding.ListItemHotspotBinding
import com.dev_vlad.fyredapp.models.UserMomentWrapper

class HotSpotsAdapter(private val clickListener: FriendsHotSpotClickListener) :
    ListAdapter<UserMomentWrapper, HotSpotsAdapter.ViewHolder>(HotSpotsDiffCallback()) {


    interface FriendsHotSpotClickListener {
        fun onFriendsHotSpotClicked(
            clickedMoment: UserMomentWrapper
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //use getItem from listAdapter
        holder.bind(getItem(position), clickListener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    //dataset difference calculator
    class HotSpotsDiffCallback :
        DiffUtil.ItemCallback<UserMomentWrapper>() {
        override fun areItemsTheSame(
            oldItem: UserMomentWrapper,
            newItem: UserMomentWrapper
        ): Boolean {
            return oldItem.isSameAs(newItem)
        }

        override fun areContentsTheSame(
            oldItem: UserMomentWrapper,
            newItem: UserMomentWrapper
        ): Boolean {
            return oldItem.hasSameContentsAs(newItem)
        }
    }

    class ViewHolder private constructor(val binding: ListItemHotspotBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            moment: UserMomentWrapper,
            clickListener: FriendsHotSpotClickListener
        ) {
            Glide.with(itemView.context)
                .load(moment.recordedBy.userPhotoUriStr)
                .placeholder(R.drawable.ic_profile_pic_pin_placeholder)
                .error(R.drawable.ic_profile_pic_pin_placeholder)
                .apply(RequestOptions.circleCropTransform())
                .into(binding.thumbnail)
            binding.userNameTv.text = moment.recordedBy.phoneBookName
            binding.userNameTv.isSelected = true

            itemView.setOnClickListener {
                clickListener.onFriendsHotSpotClicked(
                    clickedMoment = moment
                )
            }


        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemHotspotBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }
}