package com.dev_vlad.fyredapp.ui.dialogs

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.dev_vlad.fyredapp.R
import com.dev_vlad.fyredapp.media.CustomVideoPlayer
import com.dev_vlad.fyredapp.models.UserMomentWrapper
import com.google.android.exoplayer2.ui.PlayerView

class ViewHotSpotMomentsDialog(
    private val userMomentWrapper: UserMomentWrapper
) : DialogFragment() {


    // Use this instance of the interface to deliver action events
    lateinit var listener: ViewMomentsAtHotSpotDialogListener
    private var videoPlayer: CustomVideoPlayer? = null

    interface ViewMomentsAtHotSpotDialogListener {
        fun onSeeMoreClicked(userMomentWrapper: UserMomentWrapper)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return super.onCreateView(inflater, container, savedInstanceState)
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val view: View = inflater.inflate(R.layout.dialog_view_hotspot_moment, null)
            builder.setView(view)

            view.findViewById<Button>(R.id.cancel_btn).setOnClickListener {
                dismiss()
            }

            val momentVidVv = view.findViewById<PlayerView>(R.id.moment_vid_pv)
            val thumbnail = view.findViewById<ImageView>(R.id.thumbnail_iv)

            //is there is a photo or video thumbnail to display
            val firstMoment = userMomentWrapper.recordedMoments[0]
            if (firstMoment.isImage) {
                momentVidVv.visibility = View.GONE
                thumbnail.visibility = View.VISIBLE
                Glide.with(view.context)
                    .load(firstMoment.mediaUriString)
                    .placeholder(R.drawable.ic_img_loading_placeholder)
                    .into(thumbnail)
            } else {
                thumbnail.visibility = View.GONE
                momentVidVv.visibility = View.VISIBLE
                videoPlayer = CustomVideoPlayer(momentVidVv, firstMoment.mediaUriString)
            }

            /*TODO set caption
            if (moment.caption != null)
                binding.captionTv.text = moment.caption */

            /*TODO set
            if (totalMoments > 1){
                show badge or sthn
            }*/
            view.findViewById<Button>(R.id.view_moments_btn).setOnClickListener {
                listener.onSeeMoreClicked(userMomentWrapper)
                //dismiss
                dismiss()
            }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }


}