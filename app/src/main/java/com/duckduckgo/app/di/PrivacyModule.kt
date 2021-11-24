/*
 * Copyright (c) 2018 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.app.di

import android.content.Context
import androidx.lifecycle.LifecycleObserver
import androidx.work.WorkManager
import com.duckduckgo.app.browser.WebDataManager
import com.duckduckgo.app.browser.cookies.ThirdPartyCookieManager
import com.duckduckgo.app.fire.*
import com.duckduckgo.app.fire.fireproofwebsite.data.FireproofWebsiteRepository
import com.duckduckgo.app.global.DispatcherProvider
import com.duckduckgo.app.global.file.FileDeleter
import com.duckduckgo.app.global.view.ClearDataAction
import com.duckduckgo.app.global.view.ClearPersonalDataAction
import com.duckduckgo.app.location.GeoLocationPermissions
import com.duckduckgo.app.location.GeoLocationPermissionsManager
import com.duckduckgo.app.location.data.LocationPermissionsRepository
import com.duckduckgo.app.privacy.model.PrivacyPractices
import com.duckduckgo.app.privacy.model.PrivacyPracticesImpl
import com.duckduckgo.app.privacy.store.TermsOfServiceStore
import com.duckduckgo.app.settings.db.SettingsDataStore
import com.duckduckgo.app.tabs.model.TabRepository
import com.duckduckgo.app.trackerdetection.EntityLookup
import com.duckduckgo.app.trackerdetection.TdsEntityLookup
import com.duckduckgo.app.trackerdetection.db.TdsDomainEntityDao
import com.duckduckgo.app.trackerdetection.db.TdsEntityDao
import com.duckduckgo.di.scopes.AppObjectGraph
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dagger.SingleIn

@Module
class PrivacyModule {

    @Provides
    @SingleIn(AppObjectGraph::class)
    fun privacyPractices(termsOfServiceStore: TermsOfServiceStore, entityLookup: EntityLookup): PrivacyPractices =
        PrivacyPracticesImpl(termsOfServiceStore, entityLookup)

    @Provides
    @SingleIn(AppObjectGraph::class)
    fun entityLookup(entityDao: TdsEntityDao, domainEntityDao: TdsDomainEntityDao): EntityLookup =
        TdsEntityLookup(entityDao, domainEntityDao)

    @Provides
    fun clearDataAction(
        context: Context,
        dataManager: WebDataManager,
        clearingStore: UnsentForgetAllPixelStore,
        tabRepository: TabRepository,
        settingsDataStore: SettingsDataStore,
        cookieManager: DuckDuckGoCookieManager,
        appCacheClearer: AppCacheClearer,
        geoLocationPermissions: GeoLocationPermissions,
        thirdPartyCookieManager: ThirdPartyCookieManager
    ): ClearDataAction {
        return ClearPersonalDataAction(
            context,
            dataManager,
            clearingStore,
            tabRepository,
            settingsDataStore,
            cookieManager,
            appCacheClearer,
            geoLocationPermissions,
            thirdPartyCookieManager
        )
    }

    @Provides
    fun backgroundTimeKeeper(): BackgroundTimeKeeper {
        return DataClearerTimeKeeper()
    }

    @Provides
    @SingleIn(AppObjectGraph::class)
    fun automaticDataClearer(
        workManager: WorkManager,
        settingsDataStore: SettingsDataStore,
        clearDataAction: ClearDataAction,
        dataClearerTimeKeeper: BackgroundTimeKeeper,
        dataClearerForegroundAppRestartPixel: DataClearerForegroundAppRestartPixel
    ): DataClearer {
        return AutomaticDataClearer(workManager, settingsDataStore, clearDataAction, dataClearerTimeKeeper, dataClearerForegroundAppRestartPixel)
    }

    @Provides
    @SingleIn(AppObjectGraph::class)
    @IntoSet
    fun dataClearerLifecycleObserver(dataClearer: DataClearer): LifecycleObserver = dataClearer

    @Provides
    @SingleIn(AppObjectGraph::class)
    @IntoSet
    fun dataClearerForegroundAppRestartPixelObserver(
        dataClearerForegroundAppRestartPixel: DataClearerForegroundAppRestartPixel
    ): LifecycleObserver = dataClearerForegroundAppRestartPixel

    @Provides
    @SingleIn(AppObjectGraph::class)
    fun appCacheCleaner(context: Context, fileDeleter: FileDeleter): AppCacheClearer {
        return AndroidAppCacheClearer(context, fileDeleter)
    }

    @Provides
    @SingleIn(AppObjectGraph::class)
    fun geoLocationPermissions(
        context: Context,
        locationPermissionsRepository: LocationPermissionsRepository,
        fireproofWebsiteRepository: FireproofWebsiteRepository,
        dispatcherProvider: DispatcherProvider
    ): GeoLocationPermissions {
        return GeoLocationPermissionsManager(context, locationPermissionsRepository, fireproofWebsiteRepository, dispatcherProvider)
    }
}
