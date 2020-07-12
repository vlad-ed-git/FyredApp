package com.dev_vlad.fyredapp.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.dev_vlad.fyredapp.R

class ShareMomentConfirmDialog : DialogFragment() {


    // Use this instance of the interface to deliver action events
    lateinit var listener: ShareMomentDialogListener

    interface ShareMomentDialogListener {
        fun onContinueSharingClicked()
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val view: View = inflater.inflate(R.layout.dialog_confirm_share_moment_action, null)
            builder.setView(view)

            view.findViewById<Button>(R.id.cancel_btn).setOnClickListener {
                dismiss()
            }

            view.findViewById<Button>(R.id.share_btn).setOnClickListener {
                listener.onContinueSharingClicked()
            }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }


}