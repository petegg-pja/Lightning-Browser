package com.oceanhero.search.browser.activity

import com.oceanhero.search.R
import com.oceanhero.search.di.injector
import com.oceanhero.search.preference.UserPreferences
import com.oceanhero.search.utils.ThemeUtils
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.iterator
import javax.inject.Inject

abstract class ThemableBrowserActivity : AppCompatActivity() {

    // TODO reduce protected visibility
    @Inject protected lateinit var userPreferences: UserPreferences

    private var themeId: Int = 0
    private var showTabsInDrawer: Boolean = false
    private var shouldRunOnResumeActions = false

    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject(this)
        themeId = userPreferences.useTheme
        showTabsInDrawer = userPreferences.showTabsInDrawer

        // set the theme
        if (themeId == 1) {
            setTheme(R.style.Theme_DarkTheme)
        } else if (themeId == 2) {
            setTheme(R.style.Theme_BlackTheme)
        }
        super.onCreate(savedInstanceState)

        resetPreferences()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        withStyledAttributes(attrs = intArrayOf(R.attr.iconColorState)) {
            val iconTintList = getColorStateList(0)
            menu.iterator().forEach { menuItem ->
                menuItem.icon?.let { DrawableCompat.setTintList(DrawableCompat.wrap(it), iconTintList) }
            }
        }

        return super.onCreateOptionsMenu(menu)
    }

    private fun resetPreferences() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (userPreferences.useBlackStatusBar) {
                window.statusBarColor = Color.BLACK
            } else {
                window.statusBarColor = ThemeUtils.getStatusBarColor(this)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && shouldRunOnResumeActions) {
            shouldRunOnResumeActions = false
            onWindowVisibleToUserAfterResume()
        }
    }

    /**
     * Called after the activity is resumed
     * and the UI becomes visible to the user.
     * Called by onWindowFocusChanged only if
     * onResume has been called.
     */
    protected open fun onWindowVisibleToUserAfterResume() = Unit

    override fun onResume() {
        super.onResume()
        resetPreferences()
        shouldRunOnResumeActions = true
        val themePreference = userPreferences.useTheme
        val drawerTabs = userPreferences.showTabsInDrawer
        if (themeId != themePreference || showTabsInDrawer != drawerTabs) {
            restart()
        }
    }

    protected fun restart() {
        finish()
        startActivity(Intent(this, javaClass))
    }
}
