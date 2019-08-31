package com.oceanhero.search.di

import com.oceanhero.search.BrowserApp
import com.oceanhero.search.adblock.BloomFilterAdBlocker
import com.oceanhero.search.adblock.NoOpAdBlocker
import com.oceanhero.search.browser.SearchBoxModel
import com.oceanhero.search.browser.activity.BrowserActivity
import com.oceanhero.search.browser.activity.ThemableBrowserActivity
import com.oceanhero.search.browser.fragment.BookmarksFragment
import com.oceanhero.search.browser.fragment.TabsFragment
import com.oceanhero.search.device.BuildInfo
import com.oceanhero.search.dialog.LightningDialogBuilder
import com.oceanhero.search.download.DownloadHandler
import com.oceanhero.search.download.LightningDownloadListener
import com.oceanhero.search.reading.activity.ReadingActivity
import com.oceanhero.search.search.SuggestionsAdapter
import com.oceanhero.search.settings.activity.SettingsActivity
import com.oceanhero.search.settings.activity.ThemableSettingsActivity
import com.oceanhero.search.settings.fragment.*
import com.oceanhero.search.utils.ProxyUtils
import com.oceanhero.search.view.LightningChromeClient
import com.oceanhero.search.view.LightningView
import com.oceanhero.search.view.LightningWebClient
import android.app.Application
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [(AppModule::class), (AppBindsModule::class)])
interface AppComponent {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun buildInfo(buildInfo: BuildInfo): Builder

        fun build(): AppComponent
    }

    fun inject(activity: BrowserActivity)

    fun inject(fragment: BookmarksFragment)

    fun inject(fragment: BookmarkSettingsFragment)

    fun inject(builder: LightningDialogBuilder)

    fun inject(fragment: TabsFragment)

    fun inject(lightningView: LightningView)

    fun inject(activity: ThemableBrowserActivity)

    fun inject(advancedSettingsFragment: AdvancedSettingsFragment)

    fun inject(app: BrowserApp)

    fun inject(proxyUtils: ProxyUtils)

    fun inject(activity: ReadingActivity)

    fun inject(webClient: LightningWebClient)

    fun inject(activity: SettingsActivity)

    fun inject(activity: ThemableSettingsActivity)

    fun inject(listener: LightningDownloadListener)

    fun inject(fragment: PrivacySettingsFragment)

    fun inject(fragment: DebugSettingsFragment)

    fun inject(suggestionsAdapter: SuggestionsAdapter)

    fun inject(chromeClient: LightningChromeClient)

    fun inject(downloadHandler: DownloadHandler)

    fun inject(searchBoxModel: SearchBoxModel)

    fun inject(generalSettingsFragment: GeneralSettingsFragment)

    fun inject(displaySettingsFragment: DisplaySettingsFragment)

    fun inject(adBlockSettingsFragment: AdBlockSettingsFragment)

    fun provideBloomFilterAdBlocker(): BloomFilterAdBlocker

    fun provideNoOpAdBlocker(): NoOpAdBlocker

}
