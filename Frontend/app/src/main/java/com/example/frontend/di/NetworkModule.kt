package com.example.frontend.di

import com.example.frontend.core.config.AppConfig
import com.example.frontend.core.network.JwtInterceptor
import com.example.frontend.core.network.TokenProvider
import com.example.frontend.data.datastore.TokenDataStore
import com.example.frontend.data.local.dao.PostDao
import com.example.frontend.data.local.dao.SearchHistoryDao
import com.example.frontend.data.local.dao.UserDao
import com.example.frontend.data.remote.api.AuthApi
import com.example.frontend.data.remote.api.PostApi
import com.example.frontend.data.remote.api.SearchApi
import com.example.frontend.data.repository.AuthRepositoryImpl
import com.example.frontend.data.repository.PostRepositoryImpl
import com.example.frontend.data.repository.SearchRepositoryImpl
import com.example.frontend.domain.repository.AuthRepository
import com.example.frontend.domain.repository.PostRepository
import com.example.frontend.domain.repository.SearchRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenProvider: TokenProvider,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(JwtInterceptor(tokenProvider))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AppConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        authApi: AuthApi,
        tokenDataStore: com.example.frontend.data.datastore.TokenDataStore
    ): AuthRepository {
        return AuthRepositoryImpl(authApi, tokenDataStore)
    }

    @Provides
    @Singleton
    fun providePostApi(retrofit: Retrofit): PostApi {
        return retrofit.create(PostApi::class.java)
    }

    @Provides
    @Singleton
    fun providePostRepository(
        postApi: PostApi
    ): PostRepository {
        return com.example.frontend.data.repository.PostRepositoryImpl(postApi)
    }

    @Provides
    @Singleton
    fun provideSearchApi(retrofit: Retrofit): SearchApi {
        return retrofit.create(SearchApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSearchRepository(
        searchApi: SearchApi,
        searchHistoryDao: SearchHistoryDao
    ): SearchRepository {
        return SearchRepositoryImpl(searchApi, searchHistoryDao)
    }
}
