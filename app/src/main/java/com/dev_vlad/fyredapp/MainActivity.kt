package com.dev_vlad.fyredapp

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import com.dev_vlad.fyredapp.databinding.ActivityMainBinding
import com.dev_vlad.fyredapp.repositories.HotSpotsRepo
import com.dev_vlad.fyredapp.repositories.UserRepo

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private val appBarConfiguration =
        AppBarConfiguration(setOf(R.id.homeFragment, R.id.welcomeFragment))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView
        DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        navController = findNavController(R.id.myNavHostFragment)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setupWithNavController(navController, appBarConfiguration)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {

                R.id.homeFragment -> {
                    toolbar.visibility = View.VISIBLE
                    toolbar.title = getString(R.string.app_name)

                }

                R.id.contactsFragment -> {
                    toolbar.visibility = View.VISIBLE
                    toolbar.title = getString(R.string.my_contacts_fragment_lbl)

                }

                R.id.recordMomentFragment -> {
                    toolbar.visibility = View.VISIBLE
                    toolbar.title = getString(R.string.capture_e_moment)
                }

                R.id.welcomeFragment,
                R.id.loginFragment,
                R.id.hotSpotFragment,
                R.id.userProfileFragment,
                R.id.submitFeedbackFragment,
                R.id.aboutAppFragment -> {
                    toolbar.visibility = View.GONE
                }
            }

        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navController = findNavController(R.id.myNavHostFragment)
        return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        if (UserRepo.userIsLoggedIn()) HotSpotsRepo.autoDeleteOldMoments()
    }

}