package com.dev_vlad.fyredapp.ui.contacts

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dev_vlad.fyredapp.R
import com.dev_vlad.fyredapp.databinding.FragmentContactsBinding
import com.dev_vlad.fyredapp.room.entities.MyContacts
import com.dev_vlad.fyredapp.ui.adapters.MyContactsAdapter
import com.dev_vlad.fyredapp.utils.*

class ContactsFragment : Fragment(), MyContactsAdapter.ContactsClickedListener {

    private lateinit var contactsViewModel: ContactsViewModel
    private lateinit var binding: FragmentContactsBinding
    private val contactsPermission = Manifest.permission.READ_CONTACTS
    private var contactsPermissionsJustDenied = false
    private lateinit var adapter: MyContactsAdapter

    companion object {
        private val LOG_TAG = ContactsFragment::class.java.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_contacts, container, false)
        contactsViewModel = ViewModelProvider(this).get(
            ContactsViewModel::class.java
        )
        adapter = MyContactsAdapter(this)
        binding.myContactsRv.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL, false
        )
        binding.myContactsRv.adapter = adapter

        binding.emptyContactListTv.setOnClickListener {
            onRefreshMyContactsListClicked()
        }

        restoreState(savedInstanceState)

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        contactsViewModel.observerContactListStatus().observe(
            viewLifecycleOwner, Observer { fyredAppContactsStatus ->
                when (fyredAppContactsStatus) {
                    ContactsViewModel.ContactsListStatus.LOADED_NOT_SYNCED_WITH_PHONE_BOOK -> {
                        val fyredAppContacts = contactsViewModel.getContacts().toMutableList()
                        if (::adapter.isInitialized) adapter.submitList(fyredAppContacts)

                        if (fyredAppContacts.isEmpty()) {
                            binding.emptyContactListTv.setText(R.string.refresh_contact_list_msg)
                            binding.emptyContactListTv.isEnabled = true
                            binding.emptyContactListTv.visibility = View.VISIBLE
                        } else {
                            if (hasAppPermission(contactsPermission)) {
                                contactsViewModel.scanPhoneContacts()
                            }
                        }

                    }

                    ContactsViewModel.ContactsListStatus.SYNCED_WITH_PHONE_BOOK -> {

                        binding.progressBar.visibility = View.GONE

                        val fyredAppContacts = contactsViewModel.getContacts().toMutableList()
                        if (::adapter.isInitialized) adapter.submitList(fyredAppContacts)

                        if (fyredAppContacts.isEmpty()) {
                            binding.emptyContactListTv.setText(R.string.empty_contact_list_msg)
                            binding.emptyContactListTv.isEnabled = false
                            binding.emptyContactListTv.visibility = View.VISIBLE
                        } else
                            binding.emptyContactListTv.visibility = View.GONE

                    }

                    ContactsViewModel.ContactsListStatus.SYNCING_WITH_PHONE_BOOK -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }

                    else -> {
                    } //do nothing
                }


            })

    }

    private fun onRefreshMyContactsListClicked() {
        when {
            hasAppPermission(contactsPermission) -> {
                binding.emptyContactListTv.visibility = View.GONE
                binding.progressBar.visibility = View.VISIBLE
                contactsViewModel.scanPhoneContacts()
            }
            shouldShowRationaleForAppPermission(contactsPermission) -> {
                binding.contactsFragmentLayout.showSnackBarToUser(
                    msgResId = R.string.contacts_access_rationale,
                    isErrorMsg = false,
                    actionMessage = R.string.ok,
                    actionToTake = {
                        requestAppPermissions(
                            arrayOf(contactsPermission),
                            AppConstants.PERMISSION_REQUEST_READ_CONTACTS
                        )
                    }
                )
            }
            else -> {
                requestAppPermissions(
                    arrayOf(contactsPermission),
                    AppConstants.PERMISSION_REQUEST_READ_CONTACTS
                )
            }
        }

    }

    override fun onToggleContactBlockStatus(shouldBlockContact: Boolean, contact: MyContacts) {
        contactsViewModel.toggleContactsBlockStatus(shouldBlockContact, contact)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == AppConstants.PERMISSION_REQUEST_READ_CONTACTS) {
            // Request for reading contacts permissions.
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                binding.emptyContactListTv.visibility = View.GONE
                binding.progressBar.visibility = View.VISIBLE
                contactsViewModel.scanPhoneContacts()
            } else {
                // Permission request was denied
                contactsPermissionsJustDenied = true
            }
        }
    }


    override fun onResume() {
        super.onResume()
        if (contactsPermissionsJustDenied) {
            contactsPermissionsJustDenied = false
            binding.contactsFragmentLayout.showSnackBarToUser(
                msgResId = R.string.contacts_access_denied,
                isErrorMsg = true
            )
        }
    }


    private fun restoreState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            binding.emptyContactListTv.isEnabled = it.getBoolean("emptyContactListTvIsEnabled")
            binding.emptyContactListTv.visibility = it.getInt("emptyContactListTvVisibility")
            binding.progressBar.visibility = it.getInt("progressBarVisibility")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("emptyContactListTvIsEnabled", binding.emptyContactListTv.isEnabled)
        outState.putInt("emptyContactListTvVisibility", binding.emptyContactListTv.visibility)
        outState.putInt("progressBarVisibility", binding.progressBar.visibility)
    }


}