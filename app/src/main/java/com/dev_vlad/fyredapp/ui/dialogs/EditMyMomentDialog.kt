package com.dev_vlad.fyredapp.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.dev_vlad.fyredapp.R
import com.dev_vlad.fyredapp.media.CustomVideoPlayer
import com.dev_vlad.fyredapp.models.RecordedMoment
import com.google.android.exoplayer2.ui.PlayerView


class EditMyMomentDialog(private val recordedMoment: RecordedMoment) : DialogFragment() {

    //video player related
    private lateinit var momentVideoVV: PlayerView
    private var videoPlayer: CustomVideoPlayer? = null


    // Use this instance of the interface to deliver action events
    lateinit var listener: MyMomentDialogListener

    interface MyMomentDialogListener {
        fun onDeleteMomentClicked(removedMoment: RecordedMoment)
        fun onEditMoment(
            oldMoment: RecordedMoment,
            newMoment: RecordedMoment
        )
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val view: View = inflater.inflate(R.layout.dialog_edit_my_moment, null)
            builder.setView(view)

            val momentPhotoIv = view.findViewById<ImageView>(R.id.moment_photo_iv)
            momentVideoVV = view.findViewById(R.id.moment_vid_pv)
            if (recordedMoment.isImage) {
                //show photo
                momentVideoVV.visibility = View.GONE
                momentPhotoIv.visibility = View.VISIBLE
                Glide.with(view.context)
                    .load(recordedMoment.mediaUriString)
                    .into(momentPhotoIv)
            } else {
                momentPhotoIv.visibility = View.GONE
                //play video
                momentVideoVV.visibility = View.VISIBLE
                videoPlayer = CustomVideoPlayer(momentVideoVV, recordedMoment.mediaUriString)
            }

            val captionTv = view.findViewById<TextView>(R.id.caption_tv)
            captionTv.text = recordedMoment.caption

            view.findViewById<TextView>(R.id.delete_tv).setOnClickListener {
                listener.onDeleteMomentClicked(recordedMoment)
                dismiss()
            }

            //set listeners
            view.findViewById<TextView>(R.id.cancel_tv).setOnClickListener {
                dismiss()
            }

            //set caption
            view.findViewById<TextView>(R.id.done_tv).setOnClickListener {

                //set the new caption
                val newMoment = RecordedMoment(
                    mediaUriString = recordedMoment.mediaUriString,
                    isImage = recordedMoment.isImage
                )
                newMoment.caption = captionTv.text.toString()
                listener.onEditMoment(oldMoment = recordedMoment, newMoment = newMoment)

                //dismiss
                dismiss()
            }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }


    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        videoPlayer?.releasePlayer()
    }


}