package com.dev_vlad.fyredapp.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.dev_vlad.fyredapp.R

class UpdateProfilePhotoDialog : DialogFragment() {


    // Use this instance of the interface to deliver action events
    lateinit var listener: UpdateProfilePhotoDialogListener

    interface UpdateProfilePhotoDialogListener {
        fun onRemovePhotoClicked()
        fun onOpenGalleryClicked()
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val view: View = inflater.inflate(R.layout.dialog_update_profile_photo, null)
            builder.setView(view)

            view.findViewById<TextView>(R.id.cancel_tv).setOnClickListener {
                dismiss()
            }

            view.findViewById<TextView>(R.id.remove_photo_tv).setOnClickListener {
                listener.onRemovePhotoClicked()
            }

            view.findViewById<TextView>(R.id.open_gallery_tv).setOnClickListener {
                listener.onOpenGalleryClicked()
            }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}