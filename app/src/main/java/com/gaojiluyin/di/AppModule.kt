package com.gaojiluyin.di

import com.gaojiluyin.data.remote.claude.ClaudeApi
import com.gaojiluyin.data.remote.claude.ClaudeApiImpl
import com.gaojiluyin.data.remote.openai.OpenAIApi
import com.gaojiluyin.data.remote.openai.OpenAIApiImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindClaudeApi(impl: ClaudeApiImpl): ClaudeApi

    @Binds
    @Singleton
    abstract fun bindOpenAIApi(impl: OpenAIApiImpl): OpenAIApi
}
