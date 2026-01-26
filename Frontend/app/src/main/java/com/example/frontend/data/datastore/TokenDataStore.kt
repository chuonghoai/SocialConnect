package com.example.frontend.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.frontend.core.network.TokenProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

@Singleton
class TokenDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) : TokenProvider {

    private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")

    override suspend fun getAccessToken(): String? {
        return context.dataStore.data
            .map { it[KEY_ACCESS_TOKEN] }
            .first()
    }

    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = token
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.remove(KEY_ACCESS_TOKEN) }
    }

    suspend fun clearAccessToken() {
        clear()
    }
}
