package com.five9th.motionlogger.app.di

import com.five9th.motionlogger.data.FilesRepoImpl
import com.five9th.motionlogger.data.SensorsRepoImpl
import com.five9th.motionlogger.data.SessionsRepoImpl
import com.five9th.motionlogger.data.ml.TFLiteModelInference
import com.five9th.motionlogger.domain.repos.FilesRepo
import com.five9th.motionlogger.domain.repos.ModelInference
import com.five9th.motionlogger.domain.repos.SensorsRepo
import com.five9th.motionlogger.domain.repos.SessionsRepo
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

    @Binds
    @Singleton
    abstract fun bindSessionsRepo(impl: SessionsRepoImpl): SessionsRepo

    @Binds
    @Singleton
    abstract fun bindModelInference(impl: TFLiteModelInference): ModelInference
}