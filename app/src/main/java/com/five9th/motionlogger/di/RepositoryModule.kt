package com.five9th.motionlogger.di

import com.five9th.motionlogger.data.FilesRepoImpl
import com.five9th.motionlogger.data.SensorsRepoImpl
import com.five9th.motionlogger.domain.usecases.FilesRepo
import com.five9th.motionlogger.domain.usecases.SensorsRepo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSensorsRepo(impl: SensorsRepoImpl): SensorsRepo

    @Binds
    @Singleton
    abstract fun bindFilesRepo(impl: FilesRepoImpl): FilesRepo
}