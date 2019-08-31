package com.oceanhero.search.di

import com.oceanhero.search.adblock.allowlist.AllowListModel
import com.oceanhero.search.adblock.allowlist.SessionAllowListModel
import com.oceanhero.search.adblock.source.AssetsHostsDataSource
import com.oceanhero.search.adblock.source.HostsDataSource
import com.oceanhero.search.adblock.source.HostsDataSourceProvider
import com.oceanhero.search.adblock.source.PreferencesHostsDataSourceProvider
import com.oceanhero.search.database.adblock.HostsDatabase
import com.oceanhero.search.database.adblock.HostsRepository
import com.oceanhero.search.database.allowlist.AdBlockAllowListDatabase
import com.oceanhero.search.database.allowlist.AdBlockAllowListRepository
import com.oceanhero.search.database.bookmark.BookmarkDatabase
import com.oceanhero.search.database.bookmark.BookmarkRepository
import com.oceanhero.search.database.downloads.DownloadsDatabase
import com.oceanhero.search.database.downloads.DownloadsRepository
import com.oceanhero.search.database.history.HistoryDatabase
import com.oceanhero.search.database.history.HistoryRepository
import com.oceanhero.search.ssl.SessionSslWarningPreferences
import com.oceanhero.search.ssl.SslWarningPreferences
import dagger.Binds
import dagger.Module

/**
 * Dependency injection module used to bind implementations to interfaces.
 */
@Module
abstract class AppBindsModule {

    @Binds
    abstract fun provideBookmarkModel(bookmarkDatabase: BookmarkDatabase): BookmarkRepository

    @Binds
    abstract fun provideDownloadsModel(downloadsDatabase: DownloadsDatabase): DownloadsRepository

    @Binds
    abstract fun providesHistoryModel(historyDatabase: HistoryDatabase): HistoryRepository

    @Binds
    abstract fun providesAdBlockAllowListModel(adBlockAllowListDatabase: AdBlockAllowListDatabase): AdBlockAllowListRepository

    @Binds
    abstract fun providesAllowListModel(sessionAllowListModel: SessionAllowListModel): AllowListModel

    @Binds
    abstract fun providesSslWarningPreferences(sessionSslWarningPreferences: SessionSslWarningPreferences): SslWarningPreferences

    @Binds
    abstract fun providesHostsDataSource(assetsHostsDataSource: AssetsHostsDataSource): HostsDataSource

    @Binds
    abstract fun providesHostsRepository(hostsDatabase: HostsDatabase): HostsRepository

    @Binds
    abstract fun providesHostsDataSourceProvider(preferencesHostsDataSourceProvider: PreferencesHostsDataSourceProvider): HostsDataSourceProvider
}
