package xyz.stignarnia.data_remote.di.module

import android.content.SharedPreferences
import xyz.stignarnia.common.security.SecretCipher
import xyz.stignarnia.data_remote.apikey.ApiKeyProvider
import xyz.stignarnia.data_remote.apikey.PreferencesApiKeyProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiKeyModule {

  @Provides
  @Singleton
  fun providesApiKeyProvider(
    @Named("apiKeyPreferences") sharedPreferences: SharedPreferences,
    secretCipher: SecretCipher,
  ): ApiKeyProvider =
    PreferencesApiKeyProvider(
      sharedPreferences = sharedPreferences,
      secretCipher = secretCipher,
    )
}
