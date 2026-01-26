package com.example.frontend.di

import com.example.frontend.core.network.TokenProvider
import com.example.frontend.data.datastore.TokenDataStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreModule {

    @Binds
    @Singleton
    abstract fun bindTokenProvider(
        tokenDataStore: TokenDataStore
    ): TokenProvider
}
