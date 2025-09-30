package com.bsikar.helix

import android.os.Bundle
import androidx.activity.viewModels
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.bsikar.helix.viewmodels.PlayerViewModel
import com.bsikar.helix.viewmodels.PlayerViewModel.PlaybackState
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNavView: BottomNavigationView = findViewById(R.id.bottom_nav_view)
        bottomNavView.setupWithNavController(navController)

        val playingMenuItem = bottomNavView.menu.add(
            Menu.NONE,
            R.id.nav_playing,
            1,
            getString(R.string.playing)
        )
        playingMenuItem.icon = AppCompatResources.getDrawable(this, R.drawable.ic_headphones)
        playingMenuItem.isVisible = false
        playingMenuItem.isCheckable = false

        configureBottomNav(bottomNavView, navController)
        observePlaybackState(bottomNavView)
    }

    private fun configureBottomNav(
        bottomNavView: BottomNavigationView,
        navController: NavController
    ) {
        bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_playing -> {
                    navController.navigate(R.id.audiobookPlayerFragment)
                    false
                }
                else -> {
                    NavigationUI.onNavDestinationSelected(item, navController)
                    true
                }
            }
        }

        bottomNavView.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.nav_playing) {
                navController.navigate(R.id.audiobookPlayerFragment)
            }
        }
    }

    private fun observePlaybackState(bottomNavView: BottomNavigationView) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                playerViewModel.playbackState.collect { state ->
                    val playingItem = bottomNavView.menu.findItem(R.id.nav_playing)
                    val iconDrawable = playingItem.icon ?: AppCompatResources.getDrawable(
                        this@MainActivity,
                        R.drawable.ic_headphones
                    )
                    val highlightColor = ContextCompat.getColor(this@MainActivity, R.color.teal_200)
                    val defaultColor = ContextCompat.getColor(this@MainActivity, R.color.white)

                    when (state) {
                        is PlaybackState.Playing, is PlaybackState.Paused -> {
                            playingItem.isVisible = true
                            val badge = bottomNavView.getOrCreateBadge(R.id.nav_playing)
                            badge.backgroundColor = highlightColor
                            badge.badgeTextColor = defaultColor
                            badge.isVisible = true
                            badge.clearNumber()
                            if (iconDrawable != null) {
                                val wrapped = DrawableCompat.wrap(iconDrawable.mutate())
                                DrawableCompat.setTint(wrapped, highlightColor)
                                playingItem.icon = wrapped
                            }
                        }
                        is PlaybackState.Stopped -> {
                            playingItem.isVisible = false
                            bottomNavView.removeBadge(R.id.nav_playing)
                            if (iconDrawable != null) {
                                val wrapped = DrawableCompat.wrap(iconDrawable.mutate())
                                DrawableCompat.setTint(wrapped, defaultColor)
                                playingItem.icon = wrapped
                            }
                        }
                    }
                }
            }
        }
    }

}
