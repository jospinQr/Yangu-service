package com.megamind.yanguservice.di

import com.megamind.yanguservice.data.local.AndroidImageContentReader
import com.megamind.yanguservice.data.repository.SenderRepositoryImpl
import com.megamind.yanguservice.data.security.AndroidKeystoreTokenStore
import com.megamind.yanguservice.domain.AuthTokenStore
import com.megamind.yanguservice.domain.ImageContentReader
import com.megamind.yanguservice.domain.SenderRepository
import com.megamind.yanguservice.ui.screen.SenderViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {

    single<CoroutineDispatcher>(named("io")) { Dispatchers.IO }
    single<AuthTokenStore> { AndroidKeystoreTokenStore(get(), get(named("io"))) }
    single<SenderRepository> { SenderRepositoryImpl(get(), get(named("io"))) }
    single<ImageContentReader> { AndroidImageContentReader(get(), get(named("io"))) }
    viewModel { SenderViewModel(get(), get(), get()) }
}
