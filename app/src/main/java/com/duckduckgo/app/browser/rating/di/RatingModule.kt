/*
 * Copyright (c) 2019 DuckDuckGo
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

package com.duckduckgo.app.browser.rating.di

import android.content.Context
import androidx.lifecycle.LifecycleObserver
import com.duckduckgo.app.browser.rating.db.AppEnjoymentDao
import com.duckduckgo.app.browser.rating.db.AppEnjoymentDatabaseRepository
import com.duckduckgo.app.browser.rating.db.AppEnjoymentRepository
import com.duckduckgo.app.di.AppCoroutineScope
import com.duckduckgo.app.global.db.AppDatabase
import com.duckduckgo.app.global.rating.*
import com.duckduckgo.app.onboarding.store.OnboardingStore
import com.duckduckgo.app.playstore.PlayStoreAndroidUtils
import com.duckduckgo.app.playstore.PlayStoreUtils
import com.duckduckgo.app.usage.app.AppDaysUsedRepository
import com.duckduckgo.app.usage.search.SearchCountDao
import com.duckduckgo.di.scopes.AppObjectGraph
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CoroutineScope
import javax.inject.Named
import dagger.SingleIn

@Module
class RatingModule {

    @SingleIn(AppObjectGraph::class)
    @Provides
    @IntoSet
    fun appEnjoymentManagerObserver(
        appEnjoymentPromptEmitter: AppEnjoymentPromptEmitter,
        promptTypeDecider: PromptTypeDecider,
        @AppCoroutineScope appCoroutineScope: CoroutineScope
    ): LifecycleObserver {
        return AppEnjoymentAppCreationObserver(appEnjoymentPromptEmitter, promptTypeDecider, appCoroutineScope)
    }

    @Provides
    @SingleIn(AppObjectGraph::class)
    fun appEnjoymentPromptEmitter(): AppEnjoymentPromptEmitter {
        return AppEnjoymentLiveDataEmitter()
    }

    @SingleIn(AppObjectGraph::class)
    @Provides
    fun appEnjoymentUserEventRecorder(
        appEnjoymentRepository: AppEnjoymentRepository,
        appEnjoymentPromptEmitter: AppEnjoymentPromptEmitter
    ): AppEnjoymentUserEventRecorder {
        return AppEnjoymentUserEventDatabaseRecorder(appEnjoymentRepository, appEnjoymentPromptEmitter)
    }

    @Provides
    fun promptTypeDecider(
        playStoreUtils: PlayStoreUtils,
        searchCountDao: SearchCountDao,
        @Named(INITIAL_PROMPT_DECIDER_NAME) initialPromptDecider: ShowPromptDecider,
        @Named(SECONDARY_PROMPT_DECIDER_NAME) secondaryPromptDecider: ShowPromptDecider,
        context: Context,
        onboardingStore: OnboardingStore
    ): PromptTypeDecider {
        return InitialPromptTypeDecider(playStoreUtils, searchCountDao, initialPromptDecider, secondaryPromptDecider, context, onboardingStore)
    }

    @Provides
    fun playStoreUtils(context: Context): PlayStoreUtils {
        return PlayStoreAndroidUtils(context)
    }

    @SingleIn(AppObjectGraph::class)
    @Provides
    fun appEnjoymentDao(database: AppDatabase): AppEnjoymentDao {
        return database.appEnjoymentDao()
    }

    @SingleIn(AppObjectGraph::class)
    @Provides
    fun appEnjoymentRepository(appEnjoymentDao: AppEnjoymentDao): AppEnjoymentRepository {
        return AppEnjoymentDatabaseRepository(appEnjoymentDao)
    }

    @Named(INITIAL_PROMPT_DECIDER_NAME)
    @Provides
    fun initialPromptDecider(appDaysUsedRepository: AppDaysUsedRepository, appEnjoymentRepository: AppEnjoymentRepository): ShowPromptDecider {
        return InitialPromptDecider(appDaysUsedRepository, appEnjoymentRepository)
    }

    @Named(SECONDARY_PROMPT_DECIDER_NAME)
    @Provides
    fun secondaryPromptDecider(appDaysUsedRepository: AppDaysUsedRepository, appEnjoymentRepository: AppEnjoymentRepository): ShowPromptDecider {
        return SecondaryPromptDecider(appDaysUsedRepository, appEnjoymentRepository)
    }

    companion object {
        private const val INITIAL_PROMPT_DECIDER_NAME = "initial-prompt-decider"
        private const val SECONDARY_PROMPT_DECIDER_NAME = "secondary-prompt-decider"
    }

}
