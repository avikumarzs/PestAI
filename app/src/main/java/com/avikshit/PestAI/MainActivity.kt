package com.avikshit.PestAI

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androi...........................................................................................................................................................................................................................................................,dx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.avikshit.PestAI.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.devicesFragment,
                R.id.historyFragment,
                R.id.remediesFragment,
                R.id.settingsFragment
            ),
            binding.drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
        // No title on toolbar – seamless organic look; hamburger only
        navController.addOnDestinationChangedListener { _, _, _ ->
            binding.toolbar.title = ""
        }
        binding.toolbar.title = ""

        setupBottomNav(navController)
    }

    private fun setupBottomNav(navController: androidx.navigation.NavController) {
        val root = binding.root.findViewById<View>(R.id.bottomNav)
        val navHome = root.findViewById<LinearLayout>(R.id.navHome)
        val navHistory = root.findViewById<LinearLayout>(R.id.navHistory)
        val navRemedies = root.findViewById<LinearLayout>(R.id.navRemedies)
        val navDevices = root.findViewById<LinearLayout>(R.id.navDevices)
        val fabScan = root.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabScan)

        val primaryColor = ContextCompat.getColor(this, R.color.forest_green)
        val secondaryColor = ContextCompat.getColor(this, R.color.text_secondary)

        fun setSelected(item: LinearLayout?, selected: Boolean) {
            item ?: return
            val icon = item.getChildAt(0) as? ImageView
            val label = item.getChildAt(1) as? TextView
            val color = if (selected) primaryColor else secondaryColor
            icon?.setColorFilter(color)
            label?.setTextColor(color)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            setSelected(navHome, destination.id == R.id.homeFragment)
            setSelected(navHistory, destination.id == R.id.historyFragment)
            setSelected(navRemedies, destination.id == R.id.remediesFragment || destination.id == R.id.remedyDetailFragment)
            setSelected(navDevices, destination.id == R.id.devicesFragment)
        }
        setSelected(navHome, true)

        navHome?.setOnClickListener { if (navController.currentDestination?.id != R.id.homeFragment) navController.navigate(R.id.homeFragment) }
        navHistory?.setOnClickListener { if (navController.currentDestination?.id != R.id.historyFragment) navController.navigate(R.id.historyFragment) }
        navRemedies?.setOnClickListener { if (navController.currentDestination?.id != R.id.remediesFragment) navController.navigate(R.id.remediesFragment) }
        navDevices?.setOnClickListener { if (navController.currentDestination?.id != R.id.devicesFragment) navController.navigate(R.id.devicesFragment) }
        fabScan?.setOnClickListener { navController.navigate(R.id.scanFragment) }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private val navController by lazy {
        (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
    }
}
