package com.example.myemailapp.di

import com.example.myemailapp.data.repository.AuthRepository
import com.example.myemailapp.data.repository.ContactRepository
import com.example.myemailapp.data.repository.ContactRepositoryImpl
import com.example.myemailapp.data.repository.FoldersRepository
import com.example.myemailapp.data.repository.FoldersRepositoryImpl
import com.example.myemailapp.data.repository.EmailRepository
import com.example.myemailapp.data.repository.UserRepository
import com.example.myemailapp.data.repository.UserRepositoryImpl
import com.example.myemailapp.data.service.CreateFolderStatusService
import com.example.myemailapp.data.service.EmailStatusService
import com.example.myemailapp.data.repository.AuthRepositoryImpl
import com.example.myemailapp.data.repository.EmailRepositoryImpl
import com.example.myemailapp.data.service.EmailStatusServiceImpl
import com.example.myemailapp.data.util.TestDataSeeder
import com.example.myemailapp.presentation.ui.emails.AttachmentViewModel
import com.example.myemailapp.domain.service.CreateFolderStatusServiceImpl
import com.example.myemailapp.presentation.ui.emails.EmailsViewModel
import com.example.myemailapp.presentation.ui.emails.create.CreateEmailViewModel
import com.example.myemailapp.presentation.ui.emails.view.ViewEmailViewModel
import com.example.myemailapp.presentation.ui.folders.create_new.CreateFolderViewModel
import com.example.myemailapp.presentation.ui.folders.view_all.FoldersViewModel
import com.example.myemailapp.presentation.ui.login.LoginViewModel
import com.example.myemailapp.presentation.ui.main.MainViewModel
import com.example.myemailapp.presentation.ui.testdata.TestDataViewModel
import com.example.myemailapp.presentation.ui.profile.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Firebase instances
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }

    // Repositories
    single<AuthRepository> { AuthRepositoryImpl(auth = get()) }
    single<EmailRepository> { EmailRepositoryImpl(auth = get(), db = get()) }
    single<FoldersRepository> { FoldersRepositoryImpl(auth = get(), db = get()) }
    single<ContactRepository> { ContactRepositoryImpl(auth = get(), db = get()) }
    single<UserRepository> { UserRepositoryImpl(auth = get(), db = get()) }

    // Services
    single<EmailStatusService> { EmailStatusServiceImpl() }
    single<CreateFolderStatusService> { CreateFolderStatusServiceImpl() }

    // Utilities
    single { TestDataSeeder(firestore = get(), auth = get()) }

    // ViewModels
    viewModel { MainViewModel(get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { EmailsViewModel(get(), get()) }
    viewModel { ProfileViewModel(get()) }
    viewModel { CreateEmailViewModel(get(), get(), get()) }
    viewModel { ViewEmailViewModel(get(), get()) }
    viewModel { AttachmentViewModel() }
    viewModel { FoldersViewModel(get(), get()) }
    viewModel { CreateFolderViewModel(get(), get()) }
    viewModel { TestDataViewModel(get()) }
}