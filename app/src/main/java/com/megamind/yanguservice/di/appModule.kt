package com.megamind.yanguservice.di

import com.megamind.yanguservice.data.repository.ContactRepositoryImpl
import com.megamind.yanguservice.utlis.AndroidImageContentReader
import com.megamind.yanguservice.data.repository.SenderRepositoryImpl
import com.megamind.yanguservice.data.security.AndroidKeystoreTokenStore
import com.megamind.yanguservice.domain.repo.ContactRepository
import com.megamind.yanguservice.domain.utils.AuthTokenStore
import com.megamind.yanguservice.domain.utils.ImageContentReader
import com.megamind.yanguservice.domain.repo.SenderRepository
import com.megamind.yanguservice.ui.screen.SenderViewModel
import com.megamind.yanguservice.ui.screen.contacts.ContactsViewModel
import com.megamind.yanguservice.ui.screen.settings.SettingsViewModel
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
    single<ContactRepository> { ContactRepositoryImpl(get()) }


    viewModel { ContactsViewModel(get()) }
    viewModel { SenderViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get()) }
}
