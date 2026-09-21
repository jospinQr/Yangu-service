package com.megamind.yanguservice.di

import androidx.room.Room
import com.megamind.yanguservice.data.local.AppDataBase
import com.megamind.yanguservice.data.repository.ContactRepositoryImpl
import com.megamind.yanguservice.domain.repo.ContactRepository
import com.megamind.yanguservice.ui.screen.contacts.ContactsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val roomModule = module {
    single {
        Room.databaseBuilder(get(), AppDataBase::class.java, "yangu_service.db")
            .build()
    }
    single { get<AppDataBase>().contactDao() }

}
