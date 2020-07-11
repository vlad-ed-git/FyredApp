package com.dev_vlad.fyredapp.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.dev_vlad.fyredapp.R
import com.dev_vlad.fyredapp.databinding.ListItemContactBinding
import com.dev_vlad.fyredapp.room.entities.MyContacts

class MyContactsAdapter(private val clickListener: ContactsClickedListener) :
    ListAdapter<MyContacts, MyContactsAdapter.ViewHolder>(MyContactsDiffCallback()) {

    interface ContactsClickedListener {
        fun onToggleContactBlockStatus(shouldBlockContact: Boolean, contact: MyContacts)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //use getItem from listAdapter
        holder.bind(getItem(position), clickListener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    //dataset difference calculator
    class MyContactsDiffCallback :
        DiffUtil.ItemCallback<MyContacts>() {
        override fun areItemsTheSame(
            oldItem: MyContacts,
            newItem: MyContacts
        ): Boolean {
            val areSameItems = oldItem.isSameAs(newItem)
            Log.d("MyContactsAdapter", "from fyredapp | areItemsSame? $areSameItems")
            return oldItem.phoneNumber == newItem.phoneNumber
        }

        override fun areContentsTheSame(
            oldItem: MyContacts,
            newItem: MyContacts
        ): Boolean {
            val haveSameContents = oldItem.hasSameContentsAs(newItem)
            Log.d(
                "MyContactsAdapter",
                "from fyredapp | ${oldItem.canViewMyMoments} , ${newItem.canViewMyMoments}"
            )
            Log.d("MyContactsAdapter", "from fyredapp |  areContentsSame? $haveSameContents")
            return haveSameContents
        }
    }

    class ViewHolder private constructor(val binding: ListItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: MyContacts, clickListener: ContactsClickedListener) {

            Log.d("MyContactsAdapter", "binding ${contact.profileUrl}")
            Glide.with(itemView.context)
                .load(contact.profileUrl)
                .placeholder(R.drawable.ic_empty_profile_pic)
                .error(R.drawable.ic_empty_profile_pic)
                .apply(RequestOptions.circleCropTransform())
                .into(binding.profilePic)

            binding.contactNameTv.text = contact.phoneBookSavedName
            if (contact.canViewMyMoments) {
                binding.canViewMySpotsTv.setText(R.string.block_txt)
                val unblockedColor = ContextCompat.getColor(itemView.context, R.color.colorWhitish)
                binding.contactNameTv.setTextColor(unblockedColor)
                binding.canViewMySpotsTv.setTextColor(unblockedColor)
                binding.canViewMySpotsTv.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    R.drawable.ic_block,
                    0,
                    0
                )
                binding.canViewMySpotsTv.setOnClickListener {
                    clickListener.onToggleContactBlockStatus(
                        shouldBlockContact = true,
                        contact = contact
                    )
                }
            } else {
                binding.canViewMySpotsTv.setText(R.string.un_block_txt)
                val blockedColor = ContextCompat.getColor(itemView.context, R.color.colorGrey)
                binding.contactNameTv.setTextColor(blockedColor)
                binding.canViewMySpotsTv.setTextColor(blockedColor)
                binding.canViewMySpotsTv.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    R.drawable.ic_unblock,
                    0,
                    0
                )
                binding.canViewMySpotsTv.setOnClickListener {
                    clickListener.onToggleContactBlockStatus(
                        shouldBlockContact = false,
                        contact = contact
                    )
                }
            }

        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemContactBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }
}